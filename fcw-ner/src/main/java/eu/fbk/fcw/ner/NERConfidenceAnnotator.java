package eu.fbk.fcw.ner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.NERClassifierCombiner;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ie.regexp.NumberSequenceClassifier;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NormalizedNamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.DefaultPaths;
import edu.stanford.nlp.pipeline.SentenceAnnotator;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.stanford.nlp.util.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class NERConfidenceAnnotator extends SentenceAnnotator {

    private static final Logger LOGGER = Logger.getLogger(NERConfidenceAnnotator.class);

    private static final int DEFAULT_MAX_LABELINGS = 100;

    private static final double DEFAULT_MIN_SPAN_CONFIDENCE = 0.5;

    private static final boolean DEFAULT_NORMALIZE_PROBABILITIES = false;

    private static final Set<String> DEFAULT_PASS_DOWN_PROPERTIES = CollectionUtils.asSet(
            "encoding", "inputEncoding", "outputEncoding", "maxAdditionalKnownLCWords", "map",
            "ner4KBest.combinationMode");

    private final AbstractSequenceClassifier<CoreLabel> ner4KBest;
    private final NERClassifierCombiner nerStandard;

    private final long maxTime;

    private final int nThreads;

    private final int maxSentenceLength;

    private final int maxLabelings;

    private final double minSpanConfidence;

    private final boolean normalizeProbabilities;

    public NERConfidenceAnnotator(final String name, final Properties properties) {
        try {
//            change the model here?
            this.nerStandard = NERClassifierCombiner.createNERClassifierCombiner(name, properties);
            this.ner4KBest = CRFClassifier.<CoreLabel>getClassifier(DefaultPaths.DEFAULT_NER_CONLL_MODEL,
                    PropertiesUtils.extractSelectedProperties(properties,
                            DEFAULT_PASS_DOWN_PROPERTIES));
            this.maxTime = PropertiesUtils.getLong(properties, name + ".maxtime", -1);
            this.nThreads = PropertiesUtils.getInt(properties, name + ".nthreads",
                    PropertiesUtils.getInt(properties, ".nthreads", 1));
            this.maxSentenceLength = PropertiesUtils.getInt(properties, name + ".maxlength",
                    Integer.MAX_VALUE);
            this.maxLabelings = PropertiesUtils.getInt(properties, name + ".maxLabelings",
                    DEFAULT_MAX_LABELINGS);
            this.minSpanConfidence = PropertiesUtils.getDouble(properties,
                    name + ".minSpanConfidence", DEFAULT_MIN_SPAN_CONFIDENCE);
            this.normalizeProbabilities = PropertiesUtils.getBool(properties,
                    name + ".normalizeProbabilities", DEFAULT_NORMALIZE_PROBABILITIES);

        } catch (final ClassNotFoundException | IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    protected int nThreads() {
        return this.nThreads;
    }

    @Override
    protected long maxTime() {
        return this.maxTime;
    }

    @Override
    public void annotate(final Annotation annotation) {
        super.annotate(annotation);
        this.ner4KBest.finalizeClassification(annotation);
    }

    @Override
    public void doOneSentence(final Annotation annotation, final CoreMap sentence) {

        // Retrieve the tokens of the sentence
        final List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

        // If the sentence is too long, mark all the tokens with NER label O, probability 1.0
        if (this.maxSentenceLength > 0 && tokens.size() > this.maxSentenceLength) {
            doOneFailedSentence(annotation, sentence);
            return;
        }

        //for the best output;
        List<CoreLabel> output;
        try {
            //get the best annotation from stanford
            output = this.nerStandard.classifySentenceWithGlobalInformation(tokens, annotation, sentence);

            for (int i = 0, sz = tokens.size(); i < sz; ++i) {
                // add the named entity tag to each token
                String neTag = output.get(i).get(CoreAnnotations.NamedEntityTagAnnotation.class);
                String normNeTag = output.get(i).get(CoreAnnotations.NormalizedNamedEntityTagAnnotation.class);
                tokens.get(i).setNER(neTag);
                if (normNeTag != null)
                    tokens.get(i).set(CoreAnnotations.NormalizedNamedEntityTagAnnotation.class, normNeTag);
                NumberSequenceClassifier.transferAnnotations(output.get(i), tokens.get(i));
            }

            //retrieve the spanning produced by the standard version of NER CoreNLP
            final Map<Span, Counter<String>> entitiesStandard = Maps.newHashMap();
            extractEntitiesFromSingleLabelling(entitiesStandard, output, 1.0); //fake 1.0 prob

            //printing the spans
            if (LOGGER.isDebugEnabled()) {
//            if (true) {
                final StringBuilder builder = new StringBuilder();
                builder.append("Standard NER annotation for \"").append(annotation).append("\":");
                for (final Entry<Span, Counter<String>> entry : entitiesStandard.entrySet()) {
                    final Span span = entry.getKey();
                    final Counter<String> counter = entry.getValue();
                    builder.append("\n  ");
                    for (int i = span.begin; i < span.end; ++i) {
                        builder.append(tokens.get(i));
                    }
                    builder.append(" = ").append(counter);
                }
//                System.out.println(builder.toString());
                LOGGER.debug(builder.toString());
            }

            // Obtain top K labelings, each scored with a probability
            final Counter<List<CoreLabel>> labelings = this.ner4KBest.classifyKBest(tokens,
                    NamedEntityTagAnnotation.class, this.maxLabelings);

//              // sort the labellings: https://stackoverflow.com/a/42916649
//              List<List<CoreLabel>> sortedLabelings = Counters.toSortedList(labelings);


            // Map labelings to <span, scored tags> entities
            final Map<Span, Counter<String>> entities = extractEntities(labelings);

            // filter entities: keep only those whose span is among the ones of the Standard NER annotation
            final int numEntitiesBefore = entities.size();
            filterEntitiesWithStandardNER(entities, entitiesStandard, 0.0, this.normalizeProbabilities);


//            // OLD CODE: Resolve overlappings by dropping low-priority spans
//            final int numEntitiesBefore = entities.size();
//            filterEntities(entities, this.minSpanConfidence);

            // Annotate input tokens
            for (final Entry<Span, Counter<String>> entry : entities.entrySet()) {
                final Span span = entry.getKey();
                final Counter<String> counter = entry.getValue();
                final String tag = Counters.argmax(counter);
                for (int i = span.begin; i < span.end; ++i) {
                    final CoreLabel token = tokens.get(i);
                    token.set(ScoredNamedEntityTagsAnnotation.class, counter);
//                    // The next ones are already saved by the NER Standard
//                    token.set(NamedEntityTagAnnotation.class, tag);
//                    token.set(NormalizedNamedEntityTagAnnotation.class, tag);
                }
            }

            // Log outcome, if enabled
            if (LOGGER.isDebugEnabled()) {
//            if (true) {
                final StringBuilder builder = new StringBuilder();
                builder.append("NER annotation for \"").append(annotation).append("\":");
                builder.append("\n  ").append(labelings.size()).append("/")
                        .append(this.maxLabelings).append(" labelings, ")
                        .append(Counters.L1Norm(Counters.exp(labelings)))
                        .append(" confidence total; ");
                builder.append("\n  ").append(entities.size()).append("/")
                        .append(numEntitiesBefore)
                        .append(" non-overlapping entities with conf >= ")
                        .append(this.minSpanConfidence);
                for (final Entry<Span, Counter<String>> entry : entities.entrySet()) {
                    final Span span = entry.getKey();
                    final Counter<String> counter = entry.getValue();
                    builder.append("\n  ");
                    for (int i = span.begin; i < span.end; ++i) {
                        builder.append(tokens.get(i));
                    }
                    builder.append(" = ").append(counter);
                }
                LOGGER.debug(builder.toString());
//                System.out.println(builder.toString());
            }

        } catch (final RuntimeInterruptedException ex) {
            // If interrupted, mark all tokens with NER label O, probability 1.0
            doOneFailedSentence(annotation, sentence);
            return;
        }
    }

    @Override
    public void doOneFailedSentence(final Annotation annotation, final CoreMap sentence) {
        final List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
        for (final CoreLabel token : tokens) {
            final String tag = this.ner4KBest.backgroundSymbol(); // should be "O"
            final Counter<String> counter = new ClassicCounter<String>();
            counter.setCount(tag, 1.0);
            token.set(ScoredNamedEntityTagsAnnotation.class, counter);
            token.set(NamedEntityTagAnnotation.class, tag);
            token.set(NormalizedNamedEntityTagAnnotation.class, tag);
        }
    }

    /**
     * Returns a set of requirements for which tasks this annotator can
     * provide.  For example, the POS annotator will return "pos".
     */
    @Override
    public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
        return Collections.singleton(CoreAnnotations.NamedEntityTagAnnotation.class);
    }

    /**
     * Returns the set of tasks which this annotator requires in order
     * to perform.  For example, the POS annotator will return
     * "tokenize", "ssplit".
     */
    @Override
    public Set<Class<? extends CoreAnnotation>> requires() {
        return Collections.unmodifiableSet(new ArraySet<>(Arrays.asList(
                CoreAnnotations.TextAnnotation.class,
                CoreAnnotations.TokensAnnotation.class,
                CoreAnnotations.CharacterOffsetBeginAnnotation.class,
                CoreAnnotations.CharacterOffsetEndAnnotation.class,
                CoreAnnotations.IndexAnnotation.class,
                CoreAnnotations.ValueAnnotation.class,
                CoreAnnotations.SentencesAnnotation.class,
                CoreAnnotations.SentenceIndexAnnotation.class,
                CoreAnnotations.PartOfSpeechAnnotation.class,
                CoreAnnotations.LemmaAnnotation.class
        )));
    }

    public static class ScoredNamedEntityTagsAnnotation
            implements CoreAnnotation<Counter<String>> {

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public Class<Counter<String>> getType() {
            return (Class) Counter.class;
        }

    }


    private static Map<Span, Counter<String>> extractEntities(
            final Counter<List<CoreLabel>> labelingCounters) {

        final Map<Span, Counter<String>> entities = Maps.newHashMap();
        for (final List<CoreLabel> labeling : labelingCounters.keySet()) {
            final double expProb = labelingCounters.getCount(labeling);
            final double prob = Math.exp(expProb);
            extractEntitiesFromSingleLabelling(entities, labeling, prob);
        }
        return entities;
    }

    private static void extractEntitiesFromSingleLabelling(Map<Span, Counter<String>> entities, List<CoreLabel> labeling, double prob) {
        int index = 0;
        while (index < labeling.size()) {
            final int beginIndex = index++;
            final String tag = labeling.get(beginIndex).get(NamedEntityTagAnnotation.class);
            if (tag != null && !"O".equalsIgnoreCase(tag)) {
                while (index < labeling.size() && tag
                        .equals(labeling.get(index).get(NamedEntityTagAnnotation.class))) {
                    ++index;
                }
                final Span span = new Span(beginIndex, index);
                Counter<String> entityCounter = entities.get(span);
                if (entityCounter == null) {
                    entityCounter = new ClassicCounter<>();
                    entities.put(span, entityCounter);
                }
                entityCounter.incrementCount(tag, prob);
            }
        }
    }


    private static void filterEntitiesWithStandardNER(final Map<Span, Counter<String>> entities,
                                                      final Map<Span, Counter<String>> entitiesStandard,
                                                      final double minSpanConfidence, boolean normalize) {

        // Do nothing if there are no entities
        if (entities.isEmpty()) {
            return;
        }

        // Compute <span, span confidence> pairs, keeping onluy spans whose confidence is above the
        // threshold and are among the ones of the Standard NER;
        int maxIndex = 0;
//        final Counter<Span> spans = new ClassicCounter<>();
        for (final Entry<Span, Counter<String>> entry : ImmutableList
                .copyOf(entities.entrySet())) {
            final Span span = entry.getKey();
            final Counter<String> tags = entry.getValue();
            final double confidence = Counters.L1Norm(tags);
            if (!entitiesStandard.containsKey(span) || (confidence <= minSpanConfidence)) {
                entities.remove(span);
            } else if (normalize) {
                for (String tag : tags.keySet()
                        ) {
                    tags.setCount(tag, tags.getCount(tag) / confidence);
                }
                entities.put(span, tags);
            }
        }
    }


    private static void filterEntities(final Map<Span, Counter<String>> entities,
                                       final double minSpanConfidence) {

        // Do nothing if there are no entities
        if (entities.isEmpty()) {
            return;
        }

        // Compute <span, span confidence> pairs, dropping spans whose confidence is below the
        // threshold and identifying max token index;
        int maxIndex = 0;
        final Counter<Span> spans = new ClassicCounter<>();
        for (final Entry<Span, Counter<String>> entry : ImmutableList
                .copyOf(entities.entrySet())) {
            final Span span = entry.getKey();
            final Counter<String> tags = entry.getValue();
            final double confidence = Counters.L1Norm(tags);
            if (confidence >= minSpanConfidence) {
                spans.setCount(span, confidence);
                maxIndex = Math.max(maxIndex, span.end);
            } else {
                entities.remove(span);
            }
        }

        // Scan spans from the one with highest confidence, dropping it in case it overlaps with
        // tokens of previously scanned spans (this strategy resolves overlaps by always taking
        // the most probable span)
        final boolean[] taggedTokens = new boolean[maxIndex];
        outer:
        for (final Span span : Counters.toSortedList(spans)) {
            for (int index = span.begin; index < span.end; ++index) {
                if (taggedTokens[index]) {
                    entities.remove(span);
                    continue outer; // skip span
                }
            }
            for (int index = span.begin; index < span.end; ++index) {
                taggedTokens[index] = true;
            }
        }
    }

    private static final class Span {

        final short begin;

        final short end;

        Span(final int begin, final int end) {
            this.begin = (short) begin;
            this.end = (short) end;
        }

        @Override
        public boolean equals(final Object object) {
            if (object == this) {
                return true;
            }
            if (!(object instanceof Span)) {
                return false;
            }
            final Span other = (Span) object;
            return this.begin == other.begin && this.end == other.end;
        }

        @Override
        public int hashCode() {
            return this.begin << 16 | this.end;
        }

        @Override
        public String toString() {
            return this.begin + "," + this.end;
        }

    }

}
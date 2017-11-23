package eu.fbk.fcw.semafortranslate;

import com.google.common.collect.TreeMultimap;
import com.google.gson.Gson;
import edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.fcw.semafor.SemaforAnnotations;
import eu.fbk.utils.core.Network;
import eu.fbk.utils.core.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Created by alessio on 06/05/15.
 */

public class SemaforTranslateAnnotator implements Annotator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SemaforTranslateAnnotator.class);

    private static String DEFAULT_YANDEX_URL = "https://translate.yandex.net/api/v1.5/tr.json/translate";
    private static String DEFAULT_YANDEX_LANG = "it-en";

    private static String DEFAULT_DEEPL_SOURCE = "IT";
    private static String DEFAULT_DEEPL_TARGET = "EN";
    private static String DEFAULT_DEEPL_URL = "https://www.deepl.com/jsonrpc";

    private static String DEFAULT_TRANSLATION_ENGINE = "yandex";
    private SemaforTranslateModel model;

    private String yandexKey;
    private String yandexLang;
    private String yandexUrl;

    private String deeplUrl;

    private String engine;

    public SemaforTranslateAnnotator(String annotatorName, Properties props) {
        Properties annotatorProperties = PropertiesUtils.dotConvertedProperties(props, annotatorName);

        yandexKey = annotatorProperties.getProperty("yandex.key");
        yandexLang = annotatorProperties.getProperty("yandex.lang", DEFAULT_YANDEX_LANG);
        yandexUrl = annotatorProperties.getProperty("yandex.url", DEFAULT_YANDEX_URL);

        deeplUrl = annotatorProperties.getProperty("deepl.url", DEFAULT_DEEPL_URL);

        engine = annotatorProperties.getProperty("engine", DEFAULT_TRANSLATION_ENGINE);
        try {
            model = SemaforTranslateModel.getInstance(annotatorProperties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void annotate(Annotation annotation) {

        String text = annotation.get(CoreAnnotations.TextAnnotation.class);

        try {

            String stanfordText = null;
            String align = null;
            String response;

            switch (engine) {
                case "yandex":
                    Map<String, String> pars = new HashMap<>();
                    pars.put("key", yandexKey);
                    pars.put("lang", yandexLang);
                    pars.put("text", text);
                    pars.put("options", "4");
                    response = Network.postRequest(yandexUrl, pars);
                    YandexResponse yandexResponse = new Gson().fromJson(response, YandexResponse.class);
                    stanfordText = yandexResponse.text.get(0);
                    align = yandexResponse.align.get(0);
                    break;
                case "deepl":
                    DeeplRequest deeplRequest = new DeeplRequest();
                    deeplRequest.id = 1;
                    deeplRequest.jsonrc = "2.0";
                    deeplRequest.method = "LMT_handle_jobs";
                    deeplRequest.params = new DeeplRequest.Params();
                    deeplRequest.params.priority = 1;
                    deeplRequest.params.lang = new DeeplRequest.Lang();
                    deeplRequest.params.lang.user_preferred_langs = new ArrayList<>();
                    deeplRequest.params.lang.user_preferred_langs.add(DEFAULT_DEEPL_SOURCE);
                    deeplRequest.params.lang.user_preferred_langs.add(DEFAULT_DEEPL_TARGET);
                    deeplRequest.params.lang.source_lang_user_selected = DEFAULT_DEEPL_SOURCE;
                    deeplRequest.params.lang.target_lang = DEFAULT_DEEPL_TARGET;
                    deeplRequest.params.jobs = new ArrayList<>();
                    DeeplRequest.Job job = new DeeplRequest.Job();
                    job.kind = "default";
                    job.raw_en_sentence = text;
                    deeplRequest.params.jobs.add(job);
                    String json = new Gson().toJson(deeplRequest);
                    response = Network.postRequest(deeplUrl, json);
                    DeeplResponse deeplResponse = new Gson().fromJson(response, DeeplResponse.class);
                    stanfordText = deeplResponse.result.translations.get(0).beams.get(0).postprocessed_sentence;
//                    System.out.println(stanfordText);
//                    System.exit(1);
                    break;
            }

            if (stanfordText == null) {
                throw new Exception("Stanford text is null");
            }

            annotation.set(SemaforTranslateAnnotations.SemaforTranslateAnnotation.class, stanfordText);

            Annotation stanfordAnnotation = new Annotation(stanfordText);
            StanfordCoreNLP pipeline = new StanfordCoreNLP(model.getStanfordProperties());
            pipeline.annotate(stanfordAnnotation);

//            String out = JSONOutputter.jsonPrint(stanfordAnnotation);
//            System.out.println(out);

            TreeMultimap<Integer, Integer> finalAlignments = TreeMultimap.create();

            if (align != null) {
                String[] splits = align.split("[,;]");
                Map<Integer, Integer> accuracy = new HashMap<>();

                for (String split : splits) {
                    String[] pieces = split.split("-");
                    AlignmentSlot alignmentSlot1 = new AlignmentSlot(pieces[0], annotation);
                    AlignmentSlot alignmentSlot2 = new AlignmentSlot(pieces[1], stanfordAnnotation);

                    String output = prepareAligner(alignmentSlot1.getText(), alignmentSlot2.getText());
                    LOGGER.debug(output);
                    try {
                        String[] rawAlignments = model.getAlignerClient().runQuery(output).split("\\s+");
                        for (String rawAlignment : rawAlignments) {
                            String[] parts = rawAlignment.split("-");
                            int destTokenIndex = Integer.parseInt(parts[1]) + alignmentSlot2.getStartIndex();
                            int originTokenIndex = Integer.parseInt(parts[0]) + alignmentSlot1.getStartIndex();
                            Integer oldAccuracy = accuracy.getOrDefault(destTokenIndex, Integer.MAX_VALUE);
                            Integer newAccuracy = alignmentSlot2.getLength();
//                    System.out.println(String.format("%d %d - %d %d", oldAccuracy, newAccuracy, destTokenIndex, originTokenIndex));
                            if (oldAccuracy < newAccuracy) {
                                continue;
                            }
                            finalAlignments.put(destTokenIndex, originTokenIndex);
                            accuracy.put(destTokenIndex, newAccuracy);
                        }
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage());
                    }
                }
            } else {
                List<String> source = new ArrayList<>();
                for (CoreLabel token : annotation.get(CoreAnnotations.TokensAnnotation.class)) {
                    source.add(token.originalText().replaceAll("\\s+", "_"));
                }
                List<String> target = new ArrayList<>();
                for (CoreLabel token : stanfordAnnotation.get(CoreAnnotations.TokensAnnotation.class)) {
                    target.add(token.originalText().replaceAll("\\s+", "_"));
                }
                String output = prepareAligner(source, target);
                LOGGER.debug(output);
                try {
                    String[] rawAlignments = model.getAlignerClient().runQuery(output).split("\\s+");
                    for (String rawAlignment : rawAlignments) {
                        String[] parts = rawAlignment.split("-");
                        int destTokenIndex = Integer.parseInt(parts[1]);
                        int originTokenIndex = Integer.parseInt(parts[0]);
                        finalAlignments.put(destTokenIndex, originTokenIndex);
                    }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            }
//            for (Integer key : finalAlignments.keySet()) {
//                System.out.println(stanfordAnnotation.get(CoreAnnotations.TokensAnnotation.class).get(key));
//                for (Integer value : finalAlignments.get(key)) {
//                    System.out.println(annotation.get(CoreAnnotations.TokensAnnotation.class).get(value));
//                }
//                System.out.println();
//            }
//
//            System.out.println(finalAlignments);

            // Cleaning alignments
            Set<Integer> uniques = new HashSet<>();
            TreeMultimap<Integer, Integer> tmpAlignments = TreeMultimap.create();
            for (Integer key : finalAlignments.keySet()) {
                Set<Integer> values = finalAlignments.get(key);
                if (values.size() == 1) {
                    uniques.addAll(values);
                }
            }
            for (Integer key : finalAlignments.keySet()) {
                Set<Integer> values = new HashSet<>();
                values.addAll(finalAlignments.get(key));
                if (values.size() != 1) {
                    values.removeAll(uniques);
                }
                if (values.size() > 0) {
                    tmpAlignments.putAll(key, values);
                } else {
                    tmpAlignments.putAll(key, finalAlignments.get(key));
                }
            }

            finalAlignments = tmpAlignments;

            Map<String, String> stringAlignments = new LinkedHashMap<>();
            for (Integer key : finalAlignments.keySet()) {
                StringBuffer buffer = new StringBuffer();
                buffer.append(key);
                buffer.append("-");
                buffer.append(stanfordAnnotation.get(CoreAnnotations.TokensAnnotation.class).get(key).originalText().replaceAll("\\s+", ""));
                String startToken = buffer.toString();
                buffer = new StringBuffer();
                for (Integer value : finalAlignments.get(key)) {
                    String thisToken = annotation.get(CoreAnnotations.TokensAnnotation.class).get(value).originalText().replaceAll("\\s+", "");
                    buffer.append(thisToken).append(" ");
                }
                stringAlignments.put(startToken, buffer.toString().trim());
            }

            annotation.set(SemaforTranslateAnnotations.SemaforTranslateAlignmentsAnnotation.class, stringAlignments);

            // Sentences in original language
            Map<Integer, Integer> tokenToSentence = new HashMap<>();
            List<CoreLabel> get = annotation.get(CoreAnnotations.TokensAnnotation.class);
            for (int i = 0; i < get.size(); i++) {
                CoreLabel token = get.get(i);
                tokenToSentence.put(i, token.get(CoreAnnotations.SentenceIndexAnnotation.class));
            }

//            System.out.println(tokenToSentence);
//
//            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
//                System.out.println(sentence.get(CoreAnnotations.SentenceIndexAnnotation.class));
//                System.out.println(sentence.get(CoreAnnotations.TokenBeginAnnotation.class));
//                System.out.println(sentence.get(CoreAnnotations.TokenEndAnnotation.class));
//                System.out.println();
//            }

//            HashMultimap<Integer, SemaforParseResult.Frame> parseResultsMap = HashMultimap.create();
            Map<Integer, List<SemaforParseResult.Frame>> parseResults = new HashMap<>();

            for (CoreMap sentence : stanfordAnnotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                Integer offset = sentence.get(CoreAnnotations.TokenBeginAnnotation.class);
                SemaforParseResult result = sentence.get(SemaforAnnotations.SemaforAnnotation.class);
                if (result != null) {
                    for (SemaforParseResult.Frame frame : result.frames) {
                        Set<Integer> origTokens = new HashSet<>();
                        Set<Integer> origSentences = new HashSet<>();
                        for (SemaforParseResult.Frame.Span span : frame.target.spans) {
                            for (int i = span.start; i < span.end; i++) {
                                origTokens.addAll(finalAlignments.get(i + offset));
                            }
                        }
                        for (Integer origToken : origTokens) {
                            origSentences.add(tokenToSentence.get(origToken));
                        }

                        if (origSentences.size() != 1) {
                            LOGGER.warn("Sentence size is " + origSentences.size());
                            continue;
                        }
                        Integer sentenceIndex = origSentences.stream().findFirst().get();

                        if (origTokens.size() == 0) {
                            continue;
                        }

                        SemaforParseResult.Frame.NamedSpanSet targetNamedSpanSet = getSpanSet(origTokens, annotation, offset, frame.target.name);

                        List<SemaforParseResult.Frame.ScoredRoleAssignment> annotationSets = new ArrayList<>();
                        List<SemaforParseResult.Frame.NamedSpanSet> namedSpanSets = new ArrayList<>();

//                    SemaforParseResult newParseResult = new SemaforParseResult();
//                    SemaforParseResult.Frame newFrame = new SemaforParseResult.Frame();
//                    System.out.println(frame.target.name);
//                    System.out.println(origTokens);
//                    System.out.println(sentenceIndex);

                        // todo: now it considers only the first option
                        SemaforParseResult.Frame.ScoredRoleAssignment roleAssignment = frame.annotationSets.get(0);
                        for (SemaforParseResult.Frame.NamedSpanSet frameElement : roleAssignment.frameElements) {
                            Set<Integer> feTokens = new HashSet<>();
                            for (SemaforParseResult.Frame.Span span : frameElement.spans) {
                                for (int i = span.start; i < span.end; i++) {
                                    for (Integer key : finalAlignments.get(i + offset)) {
                                        Integer sent = tokenToSentence.get(key);
                                        if (sent != null && sent.equals(sentenceIndex)) {
                                            feTokens.add(key);
                                        }
                                    }
                                }
                            }

                            if (feTokens.size() > 0) {
                                SemaforParseResult.Frame.NamedSpanSet feNamedSpanSet = getSpanSet(feTokens, annotation, offset, frameElement.name);
                                namedSpanSets.add(feNamedSpanSet);
                            }

//                        System.out.println(frameElement.name);
//                        System.out.println(feTokens);
                        }

                        SemaforParseResult.Frame.ScoredRoleAssignment scoredRoleAssignment = new SemaforParseResult.Frame.ScoredRoleAssignment(roleAssignment.rank, roleAssignment.score, namedSpanSets);
                        annotationSets.add(scoredRoleAssignment);

                        SemaforParseResult.Frame newFrame = new SemaforParseResult.Frame(targetNamedSpanSet, annotationSets);
                        parseResults.putIfAbsent(sentenceIndex, new ArrayList<>());
                        parseResults.get(sentenceIndex).add(newFrame);
                    }
                }
            }

            List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
            for (int i = 0; i < sentences.size(); i++) {
                CoreMap sentence = sentences.get(i);
                List<String> tokens = new ArrayList<>();
                for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                    tokens.add(token.originalText());
                }

                SemaforParseResult semaforParseResult = new SemaforParseResult(parseResults.get(i), tokens);
                sentence.set(SemaforAnnotations.SemaforAnnotation.class, semaforParseResult);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String prepareAligner(List<String> text1, List<String> text2) {
        StringBuffer buffer = new StringBuffer();
        for (String word : text1) {
            buffer.append(word).append(" ");
        }
        buffer.append("_#_");
        for (String word : text2) {
            buffer.append(word).append(" ");
        }

        return buffer.toString().trim();
    }

    private SemaforParseResult.Frame.NamedSpanSet getSpanSet(Set<Integer> tokens, Annotation annotation, Integer offset, String name) {
        SemaforParseResult.Frame.Span targetSpan = getSpan(tokens, annotation, offset);
        List<SemaforParseResult.Frame.Span> spans = new ArrayList<>();
        spans.add(targetSpan);
        SemaforParseResult.Frame.NamedSpanSet namedSpanSet = new SemaforParseResult.Frame.NamedSpanSet(name, spans);
        return namedSpanSet;
    }

    private SemaforParseResult.Frame.Span getSpan(Set<Integer> tokens, Annotation annotation, Integer offset) {
        Integer firstToken = Collections.min(tokens);
        Integer lastToken = Collections.max(tokens);

        CoreLabel firstCoreLabel = annotation.get(CoreAnnotations.TokensAnnotation.class).get(firstToken);
        CoreLabel lastCoreLabel = annotation.get(CoreAnnotations.TokensAnnotation.class).get(lastToken);

        Integer start = firstCoreLabel.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
        Integer end = lastCoreLabel.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);

//        System.out.println(tokens);
//        System.out.println(offset);
//        System.out.println(start);
//        System.out.println(end);
//        System.out.println();

        String text = annotation.get(CoreAnnotations.TextAnnotation.class).substring(start, end);
        SemaforParseResult.Frame.Span span = new SemaforParseResult.Frame.Span(
                firstToken - offset,
                lastToken + 1 - offset,
                text
        );
        return span;
    }

    /**
     * Returns a set of requirements for which tasks this annotator can
     * provide.  For example, the POS annotator will return "pos".
     */
    @Override
    public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
        return Collections.singleton(SemaforAnnotations.SemaforAnnotation.class);
    }

    /**
     * Returns the set of tasks which this annotator requires in order
     * to perform.  For example, the POS annotator will return
     * "tokenize", "ssplit".
     */
    @Override
    public Set<Class<? extends CoreAnnotation>> requires() {
        return Collections.singleton(CoreAnnotations.SentencesAnnotation.class);
    }
}

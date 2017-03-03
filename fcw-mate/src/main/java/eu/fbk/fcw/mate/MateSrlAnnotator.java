package eu.fbk.fcw.mate;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.utils.core.PropertiesUtils;
import se.lth.cs.srl.SemanticRoleLabeler;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

/**
 * Created by alessio on 06/05/15.
 */

public class MateSrlAnnotator implements Annotator {

    private SemanticRoleLabeler labeler;
    private SemanticRoleLabeler labelerBe = null;
    int maxLen;

    public static final String MODEL_FOLDER = "models" + File.separator;
    public static final String MATE_MODEL = MODEL_FOLDER + "mate.model";
    public static final String MATE_MODEL_BE = MODEL_FOLDER + "mate_be.model";
    public static final int MAXLEN = 200;

    public MateSrlAnnotator(String annotatorName, Properties props) {

        String model = props.getProperty(annotatorName + ".model", MATE_MODEL);
        maxLen = PropertiesUtils.getInteger(props.getProperty(annotatorName + ".maxlen"), MAXLEN);
        String modelBe = props.getProperty(annotatorName + ".model_be", MATE_MODEL_BE);

        labeler = MateSrlModel.getInstance(new File(model)).getLabeler();

        if (modelBe != null) {
            labelerBe = MateSrlBeModel.getInstance(new File(modelBe)).getLabeler();
        }
    }

    public static Sentence createMateSentence(CoreMap stanfordSentence) {
        Sentence ret;

        java.util.List<CoreLabel> get = stanfordSentence.get(CoreAnnotations.TokensAnnotation.class);
        int size = get.size();

        String[] forms = new String[size + 1];
        String[] poss = new String[size + 1];
        String[] lemmas = new String[size + 1];
        String[] feats = new String[size + 1];
        String[] labels = new String[size];
        int[] parents = new int[size];

        forms[0] = "<root>";
        poss[0] = "<root>";
        lemmas[0] = "<root>";
        feats[0] = "<root>";

        for (int i = 0; i < get.size(); i++) {
            CoreLabel token = get.get(i);
            forms[i + 1] = token.get(CoreAnnotations.TextAnnotation.class);
            poss[i + 1] = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
            lemmas[i + 1] = token.get(CoreAnnotations.LemmaAnnotation.class);
            feats[i + 1] = null;

            labels[i] = token.get(CoreAnnotations.CoNLLDepTypeAnnotation.class);
            parents[i] = token.get(CoreAnnotations.CoNLLDepParentIndexAnnotation.class) + 1;
        }

        ret = new Sentence(forms, lemmas, poss, feats);

        ret.setHeadsAndDeprels(parents, labels);

        return ret;
    }

    @Override
    public void annotate(Annotation annotation) {
        if (annotation.containsKey(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreMap stanfordSentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {

                Sentence sentence;
                try {
                    sentence = createMateSentence(stanfordSentence);
                } catch (Exception e) {
                    // NullPointerException
                    continue;
                }

                labeler.parseSentence(sentence);

                for (Word word : sentence) {
                    int tokenID = word.getIdx() - 1;
                    if (tokenID < 0) {
                        continue;
                    }
                    try {
                        stanfordSentence.get(CoreAnnotations.TokensAnnotation.class).get(tokenID)
                                .set(MateAnnotations.MateTokenAnnotation.class, word);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

                for (Predicate predicate : sentence.getPredicates()) {
                    int tokenID = predicate.getIdx() - 1;
                    try {
                        stanfordSentence.get(CoreAnnotations.TokensAnnotation.class).get(tokenID)
                                .set(MateAnnotations.MateAnnotation.class, predicate);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (labelerBe != null) {
                    Sentence sentenceBe = createMateSentence(stanfordSentence);
                    labelerBe.parseSentence(sentenceBe);

                    for (Predicate predicate : sentenceBe.getPredicates()) {
                        int tokenID = predicate.getIdx() - 1;
                        String lemma = stanfordSentence.get(CoreAnnotations.TokensAnnotation.class).get(tokenID).get(
                                CoreAnnotations.LemmaAnnotation.class);
                        if (lemma.equals("be")) {
                            try {
                                stanfordSentence.get(CoreAnnotations.TokensAnnotation.class).get(tokenID)
                                        .set(MateAnnotations.MateAnnotation.class, predicate);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } else {
            throw new RuntimeException("unable to find words/tokens in: " + annotation);
        }

    }

    /**
     * Returns a set of requirements for which tasks this annotator can
     * provide.  For example, the POS annotator will return "pos".
     */
    @Override public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
        return Collections.unmodifiableSet(new ArraySet<>(Arrays.asList(
                MateAnnotations.MateAnnotation.class,
                MateAnnotations.MateTokenAnnotation.class
        )));
    }

    /**
     * Returns the set of tasks which this annotator requires in order
     * to perform.  For example, the POS annotator will return
     * "tokenize", "ssplit".
     */
    @Override public Set<Class<? extends CoreAnnotation>> requires() {
        return Collections.unmodifiableSet(new ArraySet<>(Arrays.asList(
                CoreAnnotations.CoNLLDepParentIndexAnnotation.class,
                CoreAnnotations.CoNLLDepTypeAnnotation.class
        )));
    }

//    @Override
//    public Set<Requirement> requirementsSatisfied() {
//        return Collections.singleton(PikesAnnotations.SRL_REQUIREMENT);
//    }
//
//    @Override
//    public Set<Requirement> requires() {
//        return Collections.singleton(CONLLPARSE_REQUIREMENT);
//    }
}

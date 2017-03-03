package eu.fbk.fcw.wnpos;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;

import java.util.*;

/**
 * Created by alessio on 06/05/15.
 */

public class SimplePosAnnotator implements Annotator {

    public SimplePosAnnotator(String annotatorName, Properties props) {
    }

    public static String getSimplePos(String pos) {
        String simplePos = POStagset.tagset.get(pos.toUpperCase());
        if (simplePos == null) {
            simplePos = "O";
        }
        return simplePos;
    }

    @Override
    public void annotate(Annotation annotation) {
        if (annotation.containsKey(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

                for (CoreLabel token : tokens) {
                    String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                    if (pos != null) {
                        token.set(WNPosAnnotations.WNPosAnnotation.class, getSimplePos(pos));
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
        return Collections.singleton(WNPosAnnotations.WNPosAnnotation.class);
    }

    /**
     * Returns the set of tasks which this annotator requires in order
     * to perform.  For example, the POS annotator will return
     * "tokenize", "ssplit".
     */
    @Override public Set<Class<? extends CoreAnnotation>> requires() {
        return Collections.unmodifiableSet(new ArraySet<>(Arrays.asList(
                CoreAnnotations.TokensAnnotation.class,
                CoreAnnotations.SentencesAnnotation.class,
                CoreAnnotations.PartOfSpeechAnnotation.class
        )));
    }

//    @Override
//    public Set<Requirement> requirementsSatisfied() {
//        return Collections.singleton(PikesAnnotations.SIMPLEPOS_REQUIREMENT);
//    }
//
//    @Override
//    public Set<Requirement> requires() {
//        return TOKENIZE_SSPLIT_POS;
//    }
}

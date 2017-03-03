package eu.fbk.fcw.utils.annotators;

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

public class FakePosAnnotator implements Annotator {

    String[] pos;

    public FakePosAnnotator(String annotatorName, Properties props) {
        pos = props.getProperty(annotatorName + ".pos", "").split("\\s+");
    }

    @Override
    public void annotate(Annotation annotation) {

        int tk = 0;

        if (annotation.containsKey(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

                for (int i = 0, sz = tokens.size(); i < sz; i++) {
                    CoreLabel thisToken = tokens.get(i);
                    thisToken.set(CoreAnnotations.PartOfSpeechAnnotation.class, pos[tk++].toUpperCase());
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
        return Collections.singleton(CoreAnnotations.PartOfSpeechAnnotation.class);
    }

    /**
     * Returns the set of tasks which this annotator requires in order
     * to perform.  For example, the POS annotator will return
     * "tokenize", "ssplit".
     */
    @Override public Set<Class<? extends CoreAnnotation>> requires() {
        return Collections.unmodifiableSet(new ArraySet<>(Arrays.asList(
                CoreAnnotations.TokensAnnotation.class,
                CoreAnnotations.SentencesAnnotation.class
        )));
    }

//    @Override
//    public Set<Requirement> requirementsSatisfied() {
//        return Collections.singleton(POS_REQUIREMENT);
//    }
//
//    @Override
//    public Set<Requirement> requires() {
//        return TOKENIZE_AND_SSPLIT;
//    }
}

package eu.fbk.fcw.mate;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.fcw.utils.AnnotatorUtils;
import is2.data.SentenceData09;
import is2.tag.Tagger;

import java.io.File;
import java.util.*;

/**
 * Created by alessio on 06/05/15.
 */

public class AnnaPosAnnotator implements Annotator {

    private Tagger tagger;
    public static final String MODEL_FOLDER = "models" + File.separator;
    public static final String ANNA_POS_MODEL = MODEL_FOLDER + "anna_pos.model";

    public AnnaPosAnnotator(String annotatorName, Properties props) {
        File posModel = new File(props.getProperty(annotatorName + ".model", ANNA_POS_MODEL));
        tagger = AnnaPosModel.getInstance(posModel).getTagger();
    }

    @Override
    public void annotate(Annotation annotation) {
        if (annotation.containsKey(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

                String[] annaTokens = new String[tokens.size() + 1];
                annaTokens[0] = "<ROOT>";

                for (int i = 0, sz = tokens.size(); i < sz; i++) {
                    CoreLabel thisToken = tokens.get(i);
                    annaTokens[i + 1] = thisToken.originalText();
                }

                SentenceData09 instance = new SentenceData09();
                instance.init(annaTokens);
                tagger.apply(instance);

                for (int i = 0, sz = tokens.size(); i < sz; i++) {
                    CoreLabel thisToken = tokens.get(i);
                    String pos = AnnotatorUtils.parenthesisToCode(instance.ppos[i + 1]);
                    thisToken.set(CoreAnnotations.PartOfSpeechAnnotation.class, pos);
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
}

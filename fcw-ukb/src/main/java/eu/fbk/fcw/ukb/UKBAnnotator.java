package eu.fbk.fcw.ukb;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.fcw.wnpos.WNPosAnnotations;
import eu.fbk.utils.core.PropertiesUtils;

import java.io.IOException;
import java.util.*;

/**
 * Created by alessio on 06/05/15.
 */

public class UKBAnnotator implements Annotator {

    private UKB_MT tagger;
    int maxLen;
    private static int MAXLEN = 200;

    public UKBAnnotator(String annotatorName, Properties props) {
        Properties newProps = PropertiesUtils.dotConvertedProperties(props, annotatorName);
        maxLen = PropertiesUtils.getInteger(newProps.getProperty("maxlen"), MAXLEN);
        try {
            tagger = UKBModel.getInstance(newProps).getTagger();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void annotate(Annotation annotation) {
        if (annotation.containsKey(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
                if (maxLen > 0 && tokens.size() > maxLen) {
                    continue;
                }

                ArrayList<HashMap<String, String>> terms = new ArrayList<>();
                for (CoreLabel token : tokens) {
                    HashMap<String, String> term = new HashMap<>();

                    term.put("simple_pos", token.get(WNPosAnnotations.WNPosAnnotation.class));
                    term.put("lemma", token.get(CoreAnnotations.LemmaAnnotation.class));

                    terms.add(term);
                }

                try {
                    tagger.run(terms);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                for (int i = 0, sz = tokens.size(); i < sz; i++) {
                    CoreLabel thisToken = tokens.get(i);
                    String wn = terms.get(i).get("wordnet");
                    thisToken.set(UKBAnnotations.UKBAnnotation.class, wn);
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
        return Collections.singleton(UKBAnnotations.UKBAnnotation.class);
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
                CoreAnnotations.LemmaAnnotation.class,
                WNPosAnnotations.WNPosAnnotation.class
        )));
    }

//	@Override
//	public Set<Requirement> requirementsSatisfied() {
//		return Collections.singleton(PikesAnnotations.WORDNET_REQUIREMENT);
//	}
//
//	@Override
//	public Set<Requirement> requires() {
//		return Collections.unmodifiableSet(new ArraySet<Requirement>(TOKENIZE_REQUIREMENT, SSPLIT_REQUIREMENT, LEMMA_REQUIREMENT, PikesAnnotations.SIMPLEPOS_REQUIREMENT));
//	}
}

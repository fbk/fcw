package eu.fbk.fcw.semafor;

import edu.cmu.cs.lti.ark.fn.Semafor;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Token;
import edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.dkm.pikes.depparseannotation.DepParseInfo;
import eu.fbk.dkm.pikes.depparseannotation.DepparseAnnotations;
import eu.fbk.utils.core.PropertiesUtils;

import java.io.File;
import java.util.*;

/**
 * Created by alessio on 06/05/15.
 */

public class SemaforAnnotator implements Annotator {

    private Semafor parser;
    int maxLen;

    public static final int MAXLEN = 200;
    public static final String MODEL_FOLDER = "models" + File.separator;
    public static final String SEMAFOR_MODEL_DIR = MODEL_FOLDER + "semafor" + File.separator;

    public SemaforAnnotator(String annotatorName, Properties props) {
        String semaforModelDir = props.getProperty(annotatorName + ".model_dir", SEMAFOR_MODEL_DIR);
        maxLen = PropertiesUtils.getInteger(props.getProperty(annotatorName + ".model_dir"), MAXLEN);
        parser = SemaforModel.getInstance(semaforModelDir).getParser();
    }

    @Override
    public void annotate(Annotation annotation) {

        if (annotation.containsKey(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreMap stanfordSentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {

                List<CoreLabel> tokens = stanfordSentence.get(CoreAnnotations.TokensAnnotation.class);
                if (maxLen > 0 && tokens.size() > maxLen) {
                    continue;
                }

                List<Token> sentenceTokens = new ArrayList<>();

                DepParseInfo depParseInfo = stanfordSentence.get(DepparseAnnotations.MstParserAnnotation.class);
                if (depParseInfo == null) {
                    continue;
                }

                for (int i = 0; i < tokens.size(); i++) {
                    CoreLabel token = tokens.get(i);
                    String form = token.get(CoreAnnotations.TextAnnotation.class);
                    String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                    String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);

                    Integer head = depParseInfo.getDepParents().get(i + 1);
                    String rel = depParseInfo.getDepLabels().get(i + 1);

                    Token fnToken = new Token(form, pos, head, rel);
                    fnToken.setLemma(lemma);
                    sentenceTokens.add(fnToken);
                }

                Sentence sentence = new Sentence(sentenceTokens);

                try {
                    SemaforParseResult results = parser.parseSentence(sentence);
                    stanfordSentence.set(SemaforAnnotations.SemaforAnnotation.class, results);
                } catch (Exception e) {
                    e.printStackTrace();
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
        return Collections.singleton(SemaforAnnotations.SemaforAnnotation.class);
    }

    /**
     * Returns the set of tasks which this annotator requires in order
     * to perform.  For example, the POS annotator will return
     * "tokenize", "ssplit".
     */
    @Override public Set<Class<? extends CoreAnnotation>> requires() {
        return Collections.singleton(DepparseAnnotations.MstParserAnnotation.class);
    }

//    @Override
//    public Set<Requirement> requirementsSatisfied() {
//        return Collections.singleton(PikesAnnotations.SEMAFOR_REQUIREMENT);
//    }
//
//    @Override
//    public Set<Requirement> requires() {
//        return Collections.singleton(PikesAnnotations.MSTPARSE_REQUIREMENT);
//    }
}

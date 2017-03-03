package eu.fbk.fcw.mate;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.fcw.utils.AnnotatorUtils;
import eu.fbk.utils.core.PropertiesUtils;
import is2fbk.data.SentenceData09;
import is2fbk.parser.Parser;

import java.io.File;
import java.util.*;

/**
 * Created by alessio on 06/05/15.
 */

public class AnnaParseAnnotator implements Annotator {

    private Parser parser;
    private int maxLen;
    private String language;

    public static final String MODEL_FOLDER = "models" + File.separator;
    public static final String ANNA_PARSE_MODEL = MODEL_FOLDER + "anna_parse.model";
    public static final int MAXLEN = 200;

    public AnnaParseAnnotator(String annotatorName, Properties props) {
        File posModel = new File(props.getProperty(annotatorName + ".model", ANNA_PARSE_MODEL));
        parser = AnnaParseModel.getInstance(posModel).getParser();
        maxLen = PropertiesUtils.getInteger(props.getProperty(annotatorName + ".maxlen"), MAXLEN);
        language = props.getProperty(annotatorName + ".language", "en");
    }

    @Override
    public void annotate(Annotation annotation) {
        if (annotation.containsKey(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreMap stanfordSentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                List<CoreLabel> tokens = stanfordSentence.get(CoreAnnotations.TokensAnnotation.class);
                if (maxLen > 0 && tokens.size() > maxLen) {
                    continue;
                }

                List<String> forms = new ArrayList<>();
                List<String> poss = new ArrayList<>();
                List<String> lemmas = new ArrayList<>();

                forms.add("<root>");
                poss.add("<root>");
                lemmas.add("<root>");

                for (CoreLabel token : tokens) {
                    String form = token.get(CoreAnnotations.TextAnnotation.class);
                    String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                    if (language.equals("es")) {
                        if (!pos.equals("sn")) {
                            pos = pos.substring(0, 1);
                            pos = pos.toLowerCase();
                        }
                    }
                    String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);

                    form = AnnotatorUtils.codeToParenthesis(form);
                    lemma = AnnotatorUtils.codeToParenthesis(lemma);
                    pos = AnnotatorUtils.codeToParenthesis(pos);

                    forms.add(form);
                    poss.add(pos);
                    lemmas.add(lemma);
                }

                SentenceData09 localSentenceData091 = new SentenceData09();
                localSentenceData091.init(forms.toArray(new String[forms.size()]));
                localSentenceData091.setPPos(poss.toArray(new String[poss.size()]));

                SentenceData09 localSentenceData092;
                synchronized (this) {
                    localSentenceData092 = parser.apply(localSentenceData091);
                }

                for (int i = 0; i < tokens.size(); i++) {
                    CoreLabel token = tokens.get(i);
                    token.set(CoreAnnotations.CoNLLDepTypeAnnotation.class, localSentenceData092.plabels[i]);
                    token.set(CoreAnnotations.CoNLLDepParentIndexAnnotation.class, localSentenceData092.pheads[i] - 1);
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
                CoreAnnotations.CoNLLDepParentIndexAnnotation.class,
                CoreAnnotations.CoNLLDepTypeAnnotation.class
        )));
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
                CoreAnnotations.PartOfSpeechAnnotation.class
        )));
    }
}

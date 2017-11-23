package eu.fbk.fcw.semafortranslate;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.util.ErasureUtils;
import eu.fbk.utils.gson.JSONLabel;

import java.util.Map;

/**
 * Created by alessio on 03/03/17.
 */

public class SemaforTranslateAnnotations {

    @JSONLabel("translation")
    public static class SemaforTranslateAnnotation implements CoreAnnotation<String> {

        @Override
        public Class<String> getType() {
            return String.class;
        }
    }

    @JSONLabel("alignments")
    public static class SemaforTranslateAlignmentsAnnotation implements CoreAnnotation<Map<String, String>> {
        @Override
        public Class<Map<String, String>> getType() {
            return ErasureUtils.<Class<Map<String, String>>>uncheckedCast(Map.class);
        }
    }
}

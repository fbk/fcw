package eu.fbk.fcw.stemmer.corenlp;

import edu.stanford.nlp.ling.CoreAnnotation;
import eu.fbk.utils.gson.JSONLabel;

public class StemAnnotations {

    @JSONLabel("stem")
    public static class StemAnnotation implements CoreAnnotation<String> {

        @Override
        public Class<String> getType() {
            return String.class;
        }
    }

}

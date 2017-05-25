package eu.fbk.fcw.udpipe.api;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.util.ErasureUtils;
import eu.fbk.utils.gson.JSONLabel;

/**
 * Created by giovannimoretti on 19/05/16.
 */
public class UDPipeAnnotations {

    @JSONLabel("udpipe_lemma")
    public static class LemmaAnnotation implements CoreAnnotation<String> {

        public Class<String> getType() {
            return ErasureUtils.uncheckedCast(String.class);
        }
    }

    @JSONLabel("udpipe_original")
    public static class UDPipeOriginalAnnotation implements CoreAnnotation<String> {

        public Class<String> getType() {
            return ErasureUtils.uncheckedCast(String.class);
        }
    }

}

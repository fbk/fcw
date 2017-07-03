package eu.fbk.fcw.wikipedia;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.util.ErasureUtils;
import eu.fbk.utils.gson.JSONLabel;

/**
 * Created by alessio on 27/06/17.
 */

public class WikipediaAnnotations {

    @JSONLabel("wikipediapage")
    public static class WikipediaPageAnnotation implements CoreAnnotation<String> {

        public WikipediaPageAnnotation() {
        }

        public Class<String> getType() {
            return (Class) ErasureUtils.uncheckedCast(String.class);
        }
    }
}

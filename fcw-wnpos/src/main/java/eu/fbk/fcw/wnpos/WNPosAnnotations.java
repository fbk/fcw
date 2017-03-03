package eu.fbk.fcw.wnpos;

import edu.stanford.nlp.ling.CoreAnnotation;

/**
 * Created by alessio on 03/03/17.
 */

public class WNPosAnnotations {

    public static class WNPosAnnotation implements CoreAnnotation<String> {

        @Override public Class<String> getType() {
            return String.class;
        }
    }
}

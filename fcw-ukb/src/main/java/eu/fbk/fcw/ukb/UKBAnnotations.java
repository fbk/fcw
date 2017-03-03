package eu.fbk.fcw.ukb;

import edu.stanford.nlp.ling.CoreAnnotation;

/**
 * Created by alessio on 03/03/17.
 */

public class UKBAnnotations {

    public static class UKBAnnotation implements CoreAnnotation<String> {

        @Override public Class<String> getType() {
            return String.class;
        }
    }

}

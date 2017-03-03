package eu.fbk.fcw.semafor;

import edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult;
import edu.stanford.nlp.ling.CoreAnnotation;

/**
 * Created by alessio on 03/03/17.
 */

public class SemaforAnnotations {

    public static class SemaforAnnotation implements CoreAnnotation<SemaforParseResult> {

        @Override public Class<SemaforParseResult> getType() {
            return SemaforParseResult.class;
        }
    }

}

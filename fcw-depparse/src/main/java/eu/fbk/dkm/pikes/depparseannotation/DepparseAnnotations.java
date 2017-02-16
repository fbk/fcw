package eu.fbk.dkm.pikes.depparseannotation;

import edu.stanford.nlp.ling.CoreAnnotation;

/**
 * Created by alessio on 22/09/16.
 */

public class DepparseAnnotations {

    public static class MstParserAnnotation implements CoreAnnotation<DepParseInfo> {

        @Override public Class<DepParseInfo> getType() {
            return DepParseInfo.class;
        }
    }

}

package eu.fbk.fcw.mate;

import edu.stanford.nlp.ling.CoreAnnotation;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Word;

/**
 * Created by alessio on 03/03/17.
 */

public class MateAnnotations {
    public static class MateAnnotation implements CoreAnnotation<Predicate> {

        @Override public Class<Predicate> getType() {
            return Predicate.class;
        }
    }

    public static class MateTokenAnnotation implements CoreAnnotation<Word> {

        @Override public Class<Word> getType() {
            return Word.class;
        }
    }

}

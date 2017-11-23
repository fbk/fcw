import eu.fbk.fcw.stemmer.SnowballStemmer;
import eu.fbk.fcw.stemmer.ext.englishStemmer;
import org.junit.Assert;
import org.junit.Test;

public class EnglishTest {

    @Test
    public void testSnowballEnglishStemmerAttaccare() {

        SnowballStemmer stemmer = new englishStemmer();

        String[] tokens = "attacked attacking attack".split(" ");
        for (String string : tokens) {
            stemmer.setCurrent(string);
            stemmer.stem();
            String stemmed = stemmer.getCurrent();
            Assert.assertEquals("attack", stemmed);
            System.out.println(stemmed);
        }

    }
}

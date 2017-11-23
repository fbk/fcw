import eu.fbk.fcw.stemmer.SnowballStemmer;
import eu.fbk.fcw.stemmer.ext.germanStemmer;
import org.junit.Assert;
import org.junit.Test;

public class GermanTest {

    @Test
    public void testSnowballGermanStemmerAttaccare() {

        SnowballStemmer stemmer = new germanStemmer();

        String[] tokens = "aufeinanderfolgen aufeinanderfolgende aufeinanderfolgender".split(" ");
        for (String string : tokens) {
            stemmer.setCurrent(string);
            stemmer.stem();
            String stemmed = stemmer.getCurrent();
            Assert.assertEquals("aufeinanderfolg", stemmed);
            System.out.println(stemmed);
        }

    }
}

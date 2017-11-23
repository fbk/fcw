import eu.fbk.fcw.stemmer.SnowballStemmer;
import eu.fbk.fcw.stemmer.ext.italianStemmer;
import org.junit.Assert;
import org.junit.Test;

public class ItalianTest {

    @Test
    public void testSnowballItalianStemmerAttaccare() {

        SnowballStemmer stemmer = new italianStemmer();

        String[] tokens = "attacco attacchi attacca attacchiamo attaccate attaccano".split(" ");
        for (String string : tokens) {
            stemmer.setCurrent(string);
            stemmer.stem();
            String stemmed = stemmer.getCurrent();
            Assert.assertEquals("attacc", stemmed);
            System.out.println(stemmed);
        }

    }
}

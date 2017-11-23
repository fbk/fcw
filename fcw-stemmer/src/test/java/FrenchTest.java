import eu.fbk.fcw.stemmer.SnowballStemmer;
import eu.fbk.fcw.stemmer.ext.frenchStemmer;
import org.junit.Assert;
import org.junit.Test;

public class FrenchTest {

    @Test
    public void testSnowballFrenchStemmerAttaccare() {

        SnowballStemmer stemmer = new frenchStemmer();

        String[] tokens = "accuse accuser accuseront".split(" ");
        for (String string : tokens) {
            stemmer.setCurrent(string);
            stemmer.stem();
            String stemmed = stemmer.getCurrent();
            Assert.assertEquals("accus", stemmed);
            System.out.println(stemmed);
        }

    }
}

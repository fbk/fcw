import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import eu.fbk.dh.tint.runner.outputters.JSONOutputter;
import eu.fbk.fcw.udpipe.api.UDPipeAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by alessio on 15/02/17.
 */

public class UDPipeTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UDPipeTest.class);

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.setProperty("annotators", "ita_toksent, udpipe");
        properties.setProperty("customAnnotatorClass.udpipe", "eu.fbk.fcw.udpipe.api.UDPipeAnnotator");
        properties.setProperty("customAnnotatorClass.ita_toksent",
                "eu.fbk.dh.tint.tokenizer.annotators.ItalianTokenizerAnnotator");

        properties.setProperty("udpipe.server", "gardner");
        properties.setProperty("udpipe.port", "50020");
        properties.setProperty("udpipe.keepOriginal", "1");

//        properties.setProperty("udpipe.model", "/Users/alessio/Desktop/model");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(properties);

        Annotation annotation = new Annotation("Mi trovo nella casa dove sono nato.\n"
                + "\n");
        pipeline.annotate(annotation);
        System.out.println(annotation.get(UDPipeAnnotations.UDPipeOriginalAnnotation.class));
        try {
            System.out.println(JSONOutputter.jsonPrint(annotation));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

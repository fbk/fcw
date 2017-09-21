import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

//import eu.fbk.dh.tint.runner.outputters.JSONOutputter;

/**
 * Created by alessio on 15/02/17.
 */

public class UDPipeTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UDPipeTest.class);

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.setProperty("annotators", "udpipe");
        properties.setProperty("customAnnotatorClass.udpipe", "eu.fbk.fcw.udpipe.api.UDPipeAnnotator");

        properties.setProperty("udpipe.server", "gardner");
        properties.setProperty("udpipe.port", "50021");
        properties.setProperty("udpipe.keepOriginal", "1");
        properties.setProperty("udpipe.alreadyTokenized", "0");

//        properties.setProperty("udpipe.model", "/Users/alessio/Desktop/model");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(properties);

        String text = "Donald Trump can't set off a fierce new controversy Tuesday with remarks about the right to bear arms that were interpreted by many as a threat of violence against Hillary Clinton.";
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);
        for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
            System.out.println(graph);
            System.out.println(graph.getFirstRoot().index());
            System.out.println();
        }

//        System.out.println(annotation.get(UDPipeAnnotations.UDPipeOriginalAnnotation.class));
//        try {
//            System.out.println(JSONOutputter.jsonPrint(annotation));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}

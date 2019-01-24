package eu.fbk.fcw.herodotos;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.utils.core.PropertiesUtils;
import eu.fbk.utils.gson.Network;

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Created by alessio on 06/05/15.
 */

public class HerodotosAnnotator implements Annotator {

    private static String HERODOTOS_SERVER = "localhost";
    private static Integer HERODOTOS_PORT = 9006;

    private String server;
    private Integer port;

    public HerodotosAnnotator(String annotatorName, Properties props) throws Exception {
        Properties newProps = PropertiesUtils.dotConvertedProperties(props, annotatorName);
        server = newProps.getProperty("server", HERODOTOS_SERVER);
        port = PropertiesUtils.getInteger(newProps.getProperty("port"), HERODOTOS_PORT);
    }

    @Override
    public void annotate(Annotation annotation) {

        JsonParser parser = new JsonParser();

        StringBuilder builder = new StringBuilder();
        for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                builder.append(token.originalText()).append("\n");
            }
            builder.append("\n");
        }
        builder.append("\n");

        Map<String, String> pars = new HashMap<>();
        pars.put("text", builder.toString());
        pars.put("outputFormat", "crf");

        List<List<String>> ners = new ArrayList<>();

        try {
            String output = Network.postRequest(server, port, pars);
            JsonObject object = parser.parse(output).getAsJsonObject();
            String string = object.get("output").getAsString();
            String[] sentences = string.split("\n{2,}");
            for (String sentence : sentences) {
                if (sentence.trim().length() == 0) {
                    continue;
                }
                String[] words = sentence.split("\n");
                List<String> sentenceNers = new ArrayList<>();
                for (String word : words) {
                    if (word.trim().length() == 0) {
                        continue;
                    }
                    String[] parts = word.split("\\s+");
                    sentenceNers.add(parts[0]);
                }
                ners.add(sentenceNers);
            }

            int sentenceID = 0;
            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                int tokenID = 0;
                for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                    String ner = ners.get(sentenceID).get(tokenID);
                    if (ner.equals("0")) {
                        ner = "O";
                    }
                    token.set(CoreAnnotations.NamedEntityTagAnnotation.class, ner);
                    tokenID++;
                }
                sentenceID++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a set of requirements for which tasks this annotator can
     * provide.  For example, the POS annotator will return "pos".
     */
    @Override
    public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
        return Collections.unmodifiableSet(new ArraySet<>(Arrays.asList(
                CoreAnnotations.NamedEntityTagAnnotation.class
        )));
    }

    /**
     * Returns the set of tasks which this annotator requires in order
     * to perform.  For example, the POS annotator will return
     * "tokenize", "ssplit".
     */
    @Override
    public Set<Class<? extends CoreAnnotation>> requires() {
        return Collections.unmodifiableSet(new ArraySet<>(Arrays.asList(
                CoreAnnotations.TokensAnnotation.class,
                CoreAnnotations.SentencesAnnotation.class
        )));
    }
}

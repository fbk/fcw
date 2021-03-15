package eu.fbk.fcw.ppp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.fcw.utils.Network;
import eu.fbk.utils.core.PropertiesUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class PPPAnnotator implements Annotator {

    private static String DEFAULT_SERVER = "dh.fbk.eu";
    private static String DEFAULT_PORT = "443";
    private static String DEFAULT_ADDRESS = "/dh-ppp";
    private static String DEFAULT_PROTOCOL = "https";

    private URL url;
    private String language;

    public PPPAnnotator(String annotatorName, Properties properties) throws MalformedURLException {
        Properties theseProperties = PropertiesUtils.dotConvertedProperties(properties, annotatorName);

        String server = theseProperties.getProperty("server", DEFAULT_SERVER);
        String port = theseProperties.getProperty("port", DEFAULT_PORT);
        String protocol = theseProperties.getProperty("protocol", DEFAULT_PROTOCOL);
        String address = theseProperties.getProperty("address", DEFAULT_ADDRESS);

        language = theseProperties.getProperty("language", "en");

        url = new URL(protocol, server, Integer.parseInt(port), address);
    }

    @Override
    public void annotate(Annotation annotation) {
        for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            String text = sentence.get(CoreAnnotations.TextAnnotation.class);
            Map<String, String> data = new HashMap<>();
            data.put("text", text);
            data.put("lang", language);

            Gson gson = new GsonBuilder().create();
            String json = gson.toJson(data);

            try {
                String response = Network.request(url.toString(), json);
                Map<String, Double> outputTmp = gson.fromJson(response, Map.class);
                Map<String, Double> output = new HashMap<>();
                for (String key : outputTmp.keySet()) {
                    String[] parts = key.split("_");
                    String newKey = parts[parts.length - 1];
                    output.put(newKey, outputTmp.get(key));
                }

                sentence.set(PPPAnnotations.PPPAnnotation.class, output);

//                System.out.println(output);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
        return Collections.singleton(PPPAnnotations.PPPAnnotation.class);
    }

    @Override
    public Set<Class<? extends CoreAnnotation>> requires() {
        return Collections.unmodifiableSet(new ArraySet<>(Arrays.asList(
                CoreAnnotations.TextAnnotation.class,
                CoreAnnotations.TokensAnnotation.class,
                CoreAnnotations.SentencesAnnotation.class
        )));
    }
}

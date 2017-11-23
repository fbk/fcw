package eu.fbk.dkm.pikes.twm;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import eu.fbk.utils.core.PropertiesUtils;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: alessio
 * Date: 21/07/14
 * Time: 17:15
 * To change this template use File | Settings | File Templates.
 */

public class MachineLinking extends Linking {

    public static final double ML_CONFIDENCE = 0.5;

    private static String LABEL = "ml-annotate";
    private Double minWeight;
    private String lang;

    public MachineLinking(Properties properties) {
        super(properties, properties.getProperty("address"));
        minWeight = PropertiesUtils.getDouble(properties.getProperty("min_confidence"), ML_CONFIDENCE);
        lang = properties.getProperty("lang", null);
    }

    public String lang(String text) throws IOException {

        // todo: this is really bad!
        String address = urlAddress.replace("annotate", "lang");

        Map<String, String> pars;

        pars = new HashMap<>();
        pars.put("include_text", "0");
        pars.put("app_id", "0");
        pars.put("app_key", "0");
        pars.put("text", text);

        LOGGER.debug("Text length: {}", text.length());
        LOGGER.debug("Pars: {}", pars);

        Map<String, Object> userData;
        String output = request(pars, address);

        ObjectMapper mapper = new ObjectMapper();
        userData = mapper.readValue(output, Map.class);
        LinkedHashMap annotation = (LinkedHashMap) userData.get(new String("annotation"));
        if (annotation != null) {
            String lang = annotation.get("lang").toString();
            return lang;
        }

        return null;
    }

    @Override
    public List<LinkingTag> tag(String text) throws Exception {

        ArrayList<LinkingTag> ret = new ArrayList<>();
        Map<String, String> pars;

        pars = new HashMap<>();
        pars.put("min_weight", minWeight.toString());
        pars.put("disambiguation", "1");
        pars.put("topic", "1");
        pars.put("include_text", "0");
        pars.put("image", "1");
        pars.put("class", "1");
        pars.put("app_id", "0");
        pars.put("app_key", "0");
        pars.put("text", text);
        if (lang != null) {
            pars.put("lang", lang);
        }

        LOGGER.debug("Text length: {}", text.length());
        LOGGER.debug("Pars: {}", pars);

        Map<String, Object> userData;
        String output = request(pars);

        ObjectMapper mapper = new ObjectMapper();
        userData = mapper.readValue(output, Map.class);

        LinkedHashMap annotation = (LinkedHashMap) userData.get(new String("annotation"));
        if (annotation != null) {
            String lang = annotation.get("lang").toString();
            String language = (lang == null || lang.equals("en")) ? "" : lang + ".";
            ArrayList<LinkedHashMap> keywords = (ArrayList<LinkedHashMap>) annotation.get(new String("keyword"));
            if (keywords != null) {
                for (LinkedHashMap keyword : keywords) {
                    LinkedHashMap sense = (LinkedHashMap) keyword.get("sense");
                    ArrayList dbpClass = (ArrayList) keyword.get("class");
                    ArrayList<LinkedHashMap> images = (ArrayList<LinkedHashMap>) keyword.get("image");
                    ArrayList<LinkedHashMap> ngrams = (ArrayList<LinkedHashMap>) keyword.get("ngram");
                    for (LinkedHashMap ngram : ngrams) {
                        String originalText = (String) ngram.get("form");
                        LinkedHashMap span = (LinkedHashMap) ngram.get("span");

                        Integer start = (Integer) span.get("start");
                        Integer end = (Integer) span.get("end");
                        Double rel = Double.parseDouble(keyword.get("rel").toString());
                        if (rel.isNaN()) {
                            rel = 0d;
                        }

                        LinkingTag tag = new LinkingTag(
                                start,
                                String.format("http://" + language + "dbpedia.org/resource/%s",
                                        (String) sense.get("page")),
                                rel,
                                originalText,
                                end - start,
                                LABEL
                        );

                        //todo: add to conf
                        if (images != null && images.size() > 0) {
                            try {
                                tag.setImage(images.get(0).get("image").toString());
                            } catch (Exception e) {
                                // ignored
                            }
                        }

                        if (extractTypes) {
                            tag.addTypesFromML(dbpClass);
                        }
                        ret.add(tag);
                    }
                }
            }
        }

        return ret;
    }

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.setProperty("address", "http://ml.apnetwork.it/annotate");
        properties.setProperty("min_confidence", "0.5");
        properties.setProperty("timeout", "2000");

        String fileName = args[0];

        MachineLinking s = new MachineLinking(properties);
        try {
            String text = Files.toString(new File(fileName), Charsets.UTF_8);
            List<LinkingTag> tags = s.tag(text);
            for (LinkingTag tag : tags) {
                System.out.println(tag);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
    }
}

package eu.fbk.fcw.semafortranslate;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import eu.fbk.dh.aligner.AlignerClient;
import eu.fbk.utils.core.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by alessio on 27/05/15.
 */

public class SemaforTranslateModel {

    private static SemaforTranslateModel instance;
    private AlignerClient alignerClient;
    private Properties stanfordProperties;
    private static final Logger LOGGER = LoggerFactory.getLogger(SemaforTranslateModel.class);

    private String ALIGNER_HOST = "localhost";
    private Integer ALIGNER_PORT = 9010;

    private SemaforTranslateModel(Properties properties) throws IOException {
        Properties alignerProperties = PropertiesUtils.dotConvertedProperties(properties, "aligner");
        stanfordProperties = PropertiesUtils.dotConvertedProperties(properties, "stanford");

        String alignerHost = alignerProperties.getProperty("host", ALIGNER_HOST);
        Integer alignerPort = PropertiesUtils.getInteger(alignerProperties.getProperty("port"), ALIGNER_PORT);

        try {
            LOGGER.info("Connecting to {}:{}", alignerHost, alignerPort);
            alignerClient = new AlignerClient(alignerHost, alignerPort);
//            System.out.println(alignerClient.runQuery("Il cane dorme sul tappeto . _#_The dog sleeps on the carpet ."));
            StanfordCoreNLP pipeline = new StanfordCoreNLP(stanfordProperties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SemaforTranslateModel getInstance(Properties properties) throws IOException {
        if (instance == null) {
            LOGGER.info("Loading model for SemaforTranslate");
            instance = new SemaforTranslateModel(properties);
        }

        return instance;
    }

    public Properties getStanfordProperties() {
        return stanfordProperties;
    }

    public AlignerClient getAlignerClient() {
        return alignerClient;
    }
}

package eu.fbk.fcw.udpipe.api;

import com.google.gson.Gson;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.ud.CoNLLUDocumentReader;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.fcw.utils.ConllToken;
import eu.fbk.fcw.utils.Network;
import eu.fbk.utils.core.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by alessio on 07/02/17.
 */

public class UDPipeAnnotator implements Annotator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UDPipeAnnotator.class);

    private static String DEFAULT_SERVER = "localhost";
    private static String DEFAULT_PORT = "12345";
    private static String DEFAULT_ADDRESS = "/process";
    private static String DEFAULT_PROTOCOL = "http";

    private URL url;
    private boolean alreadyTokenized;
    private boolean keepOriginal;

    public UDPipeAnnotator(String annotatorName, Properties properties) throws MalformedURLException {
        Properties theseProperties = PropertiesUtils.dotConvertedProperties(properties, annotatorName);

        String server = theseProperties.getProperty("server", DEFAULT_SERVER);
        String port = theseProperties.getProperty("port", DEFAULT_PORT);
        String protocol = theseProperties.getProperty("protocol", DEFAULT_PROTOCOL);
        String address = theseProperties.getProperty("address", DEFAULT_ADDRESS);

        alreadyTokenized = PropertiesUtils.getBoolean(theseProperties.getProperty("alreadyTokenized"), true);
        keepOriginal = PropertiesUtils.getBoolean(theseProperties.getProperty("keepOriginal"), false);

        url = new URL(protocol, server, Integer.parseInt(port), address);
    }

    @Override public void annotate(Annotation annotation) {
        Map<String, String> data = new HashMap<>();
        data.put("tagger", "1");
        data.put("parser", "1");
        String dataStr = annotation.get(CoreAnnotations.TextAnnotation.class);
        if (alreadyTokenized) {
            data.put("input", "horizontal");
            StringBuffer buffer = new StringBuffer();
            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                StringBuffer line = new StringBuffer();
                for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                    line.append(token.originalText().replaceAll("\\s+", "_")).append(" ");
                }
                buffer.append(line.toString().trim()).append("\n");
            }
            dataStr = buffer.toString().trim();
        } else {
            data.put("tokenizer", "1");
        }
        data.put("data", dataStr);

        try {
            String response = Network.request(url.toString(), data);
            Gson gson = new Gson();
            UDPipe udPipe = gson.fromJson(response, UDPipe.class);

            String result = udPipe.getResult();

            if (keepOriginal) {
                annotation.set(UDPipeAnnotations.UDPipeOriginalAnnotation.class, result);
            }
            List<List<ConllToken>> text = new ArrayList<>();
            List<ConllToken> thisSentence = new ArrayList<>();

            BufferedReader reader;

            reader = new BufferedReader(new StringReader(result));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) {
                    if (thisSentence.size() > 0) {
                        text.add(thisSentence);
                        thisSentence = new ArrayList<>();
                    }
                    continue;
                }

                try {
                    ConllToken token = new ConllToken(line);
                    thisSentence.add(token);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (thisSentence.size() > 0) {
                text.add(thisSentence);
            }
            reader.close();

            List<SemanticGraph> graphs = new ArrayList<>();
            reader = new BufferedReader(new StringReader(result));
            CoNLLUDocumentReader depReader = new CoNLLUDocumentReader();
            Iterator<SemanticGraph> it = depReader.getIterator(reader);
            graphs = new ArrayList<>();
            it.forEachRemaining(graphs::add);
            reader.close();

            List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
            for (int i = 0; i < sentences.size(); i++) {
                CoreMap sentence = sentences.get(i);

                try {
                    SemanticGraph semanticGraph = graphs.get(i);
                    sentence.set(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class, semanticGraph);
                } catch (Exception e) {
                    LOGGER.warn(e.getMessage());
                }

                List<CoreLabel> get1 = sentence.get(CoreAnnotations.TokensAnnotation.class);
                for (int j = 0; j < get1.size(); j++) {
                    CoreLabel token = get1.get(j);
                    try {
                        ConllToken conllToken = text.get(i).get(j);

                        token.set(CoreAnnotations.PartOfSpeechAnnotation.class, conllToken.getXpos());
                        token.set(CoreAnnotations.LemmaAnnotation.class, conllToken.getLemma());
                        token.set(UDPipeAnnotations.UPosAnnotation.class, conllToken.getUpos());
                        token.set(UDPipeAnnotations.FeaturesAnnotation.class, conllToken.getFeats());
                        token.set(UDPipeAnnotations.DepsAnnotation.class, conllToken.getDeps());
                        token.set(UDPipeAnnotations.LemmaAnnotation.class, conllToken.getLemma());
                        token.set(UDPipeAnnotations.MiscAnnotation.class, conllToken.getMisc());

                    } catch (Exception e) {
                        LOGGER.warn(e.getMessage());
                    }
                }

            }

//            for (List<ConllToken> sentence : text) {
//                System.out.println("SENTENCE");
//                for (ConllToken token : sentence) {
//                    System.out.println(token);
//                }
//            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
        return new HashSet<>(Arrays.asList(
                CoreAnnotations.TextAnnotation.class,
                CoreAnnotations.TokensAnnotation.class,
                CoreAnnotations.CharacterOffsetBeginAnnotation.class,
                CoreAnnotations.CharacterOffsetEndAnnotation.class,
                CoreAnnotations.BeforeAnnotation.class,
                CoreAnnotations.AfterAnnotation.class,
                CoreAnnotations.TokenBeginAnnotation.class,
                CoreAnnotations.TokenEndAnnotation.class,
                CoreAnnotations.PositionAnnotation.class,
                CoreAnnotations.IndexAnnotation.class,
                CoreAnnotations.OriginalTextAnnotation.class,
                CoreAnnotations.ValueAnnotation.class,
                CoreAnnotations.PartOfSpeechAnnotation.class,
                UDPipeAnnotations.UPosAnnotation.class,
                UDPipeAnnotations.FeaturesAnnotation.class,
                UDPipeAnnotations.DepsAnnotation.class,
                UDPipeAnnotations.LemmaAnnotation.class,
                CoreAnnotations.LemmaAnnotation.class,
                UDPipeAnnotations.MiscAnnotation.class,
                SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class
        ));
    }

    @Override public Set<Class<? extends CoreAnnotation>> requires() {
        if (alreadyTokenized) {
            return Collections.unmodifiableSet(new ArraySet<>(Arrays.asList(
                    CoreAnnotations.TextAnnotation.class,
                    CoreAnnotations.TokensAnnotation.class,
                    CoreAnnotations.CharacterOffsetBeginAnnotation.class,
                    CoreAnnotations.CharacterOffsetEndAnnotation.class,
                    CoreAnnotations.SentencesAnnotation.class
            )));
        }
        return Collections.emptySet();
    }

}

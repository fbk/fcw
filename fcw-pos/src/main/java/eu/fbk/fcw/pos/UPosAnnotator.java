package eu.fbk.fcw.pos;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArraySet;
import eu.fbk.utils.corenlp.CustomAnnotations;

import java.util.*;

public class UPosAnnotator implements Annotator {

    private Map<String, String> uPosMap;

    static Map<String, String> DEFAULT_POS_MAP = new HashMap<>();
    static String DEFAULT_UPOS = "X";

    static {
        DEFAULT_POS_MAP.put("A", "ADJ");
        DEFAULT_POS_MAP.put("AP", "DET");
        DEFAULT_POS_MAP.put("B", "ADV");
        DEFAULT_POS_MAP.put("BN", "ADV");
        DEFAULT_POS_MAP.put("CC", "CCONJ");
        DEFAULT_POS_MAP.put("CS", "SCONJ");
        DEFAULT_POS_MAP.put("DD", "DET");
        DEFAULT_POS_MAP.put("DE", "DET");
        DEFAULT_POS_MAP.put("DI", "DET");
        DEFAULT_POS_MAP.put("DQ", "DET");
        DEFAULT_POS_MAP.put("DR", "DET");
        DEFAULT_POS_MAP.put("E", "ADP");
        DEFAULT_POS_MAP.put("FB", "PUNCT");
        DEFAULT_POS_MAP.put("FC", "PUNCT");
        DEFAULT_POS_MAP.put("FF", "PUNCT");
        DEFAULT_POS_MAP.put("FS", "PUNCT");
        DEFAULT_POS_MAP.put("I", "INTJ");
        DEFAULT_POS_MAP.put("N", "NUM");
        DEFAULT_POS_MAP.put("NO", "ADJ");
        DEFAULT_POS_MAP.put("PART", "PART");
        DEFAULT_POS_MAP.put("PC", "PRON");
        DEFAULT_POS_MAP.put("PD", "PRON");
        DEFAULT_POS_MAP.put("PE", "PRON");
        DEFAULT_POS_MAP.put("PI", "PRON");
        DEFAULT_POS_MAP.put("PP", "PRON");
        DEFAULT_POS_MAP.put("PQ", "PRON");
        DEFAULT_POS_MAP.put("PR", "PRON");
        DEFAULT_POS_MAP.put("RD", "DET");
        DEFAULT_POS_MAP.put("RI", "DET");
        DEFAULT_POS_MAP.put("S", "NOUN");
        DEFAULT_POS_MAP.put("SP", "PROPN");
        DEFAULT_POS_MAP.put("SYM", "SYM");
        DEFAULT_POS_MAP.put("T", "DET");
        DEFAULT_POS_MAP.put("V", "VERB");
        DEFAULT_POS_MAP.put("VA", "AUX");
        DEFAULT_POS_MAP.put("VM", "AUX");
    }

    public UPosAnnotator(String annotatorName, Properties props) {
        String mapFile = props.getProperty(annotatorName + ".map");
        if (mapFile != null) {
            uPosMap = UPosModel.getInstance(mapFile).getUposMap();
        } else {
            uPosMap = DEFAULT_POS_MAP;
        }
    }

    @Override
    public void annotate(Annotation annotation) {
        for (CoreLabel token : annotation.get(CoreAnnotations.TokensAnnotation.class)) {
            String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);

            String[] parts = pos.split("\\+");
            StringBuffer upos = new StringBuffer();
            for (String part : parts) {
                String thisPos = uPosMap.getOrDefault(part, DEFAULT_UPOS);
                upos.append("+").append(thisPos);
            }
            token.set(CustomAnnotations.UPosAnnotation.class, upos.substring(1));
            token.set(CoreAnnotations.CoarseTagAnnotation.class, upos.substring(1));
        }

    }

    @Override
    public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
//        return Collections.singleton(CustomAnnotations.UPosAnnotation.class);
        return Collections.unmodifiableSet(new ArraySet<>(Arrays.asList(
                CoreAnnotations.CoarseTagAnnotation.class,
                CustomAnnotations.UPosAnnotation.class
        )));
    }

    @Override
    public Set<Class<? extends CoreAnnotation>> requires() {
        return Collections.unmodifiableSet(new ArraySet<>(Arrays.asList(
                CoreAnnotations.PartOfSpeechAnnotation.class,
                CoreAnnotations.TokensAnnotation.class
        )));
    }
}

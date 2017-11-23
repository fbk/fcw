package eu.fbk.fcw.semafortranslate;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class AlignmentSlot {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlignmentSlot.class);
    private String interval;
    private Annotation annotation;
    private Integer startIndex = null;
    private List<String> text = new ArrayList<>();
    private Integer length;

    public Integer getStartIndex() {
        return startIndex;
    }

    public List<String> getText() {
        return text;
    }

    public Integer getLength() {
        return length;
    }

    public AlignmentSlot(String interval, Annotation annotation) {
        this.interval = interval;
        this.annotation = annotation;

        String[] parts = interval.split(":");
        Integer start = Integer.parseInt(parts[0]);
        length = Integer.parseInt(parts[1]);
        Integer finish = start + length;

//        System.out.println("Start: " + start);
//        System.out.println("Finish: " + finish);

        List<CoreLabel> tokens = annotation.get(CoreAnnotations.TokensAnnotation.class);
        for (int i = 0; i < tokens.size(); i++) {
            CoreLabel token = tokens.get(i);
            Integer begin = token.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
            Integer end = token.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);

//            System.out.println(token.originalText());
//            System.out.println(begin);
//            System.out.println(end);
//            System.out.println();

            if (begin >= start && end <= finish) {
                text.add(token.originalText().replaceAll("\\s+", "_"));
                if (startIndex == null) {
                    startIndex = i;
                }
            }
        }

//        System.out.println(interval);
//        System.out.println(startIndex);
//        System.out.println(text);
//        System.out.println();
    }
}

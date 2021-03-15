package eu.fbk.fcw.ppp;

// Anger
// Anticipation
// Disgust
// Fear
// Joy
// Sadness
// Surprise
// Trust
// Valence
// Arousal
// Dominance
// Positive
// Negative

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.util.ErasureUtils;
import eu.fbk.utils.gson.JSONLabel;

import java.util.Map;

public class PPPAnnotations {
    @JSONLabel("sentiment")
    public static class PPPAnnotation implements CoreAnnotation<Map<String, Double>> {

        @Override
        public Class<Map<String, Double>> getType() {
            return ErasureUtils.uncheckedCast(Map.class);
        }
    }
}

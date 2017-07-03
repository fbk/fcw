package eu.fbk.fcw.wikipedia;

import com.google.common.collect.HashMultimap;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import eu.fbk.utils.core.FrequencyHashSet;
import eu.fbk.utils.corenlp.CustomAnnotations;

import java.util.*;

/**
 * Created by alessio on 14/06/17.
 */

public class WikipediaCorefAnnotator implements Annotator {

    static final HashMultimap<String, String> patterns = HashMultimap.create();

    static {
        patterns.put("m", "he");
        patterns.put("m", "his");
        patterns.put("m", "him");
        patterns.put("m", "himself");
        patterns.put("f", "she");
        patterns.put("f", "her");
        patterns.put("f", "hers");
        patterns.put("f", "herself");
        patterns.put("n", "it");
        patterns.put("n", "its");
        patterns.put("a", "they");
        patterns.put("a", "their");
        patterns.put("a", "them");
    }

    public WikipediaCorefAnnotator(String annotatorName, Properties prop) {

    }

    @Override public void annotate(Annotation annotation) {
        FrequencyHashSet<String> frequencies = new FrequencyHashSet<>();
        Map<String, HashMultimap<Integer, Integer>> words = new HashMap<>();

//        int totalMF = 0;
//        int total = 0;

        Map<String, String> reverse = new HashMap<>();
        for (String key : patterns.keySet()) {
            for (String value : patterns.get(key)) {
                reverse.put(value, key);
            }
        }

        Set<String> allowedTitles = new HashSet<>();

        if (annotation.containsKey(CoreAnnotations.DocTitleAnnotation.class)) {
            String title = annotation.get(CoreAnnotations.DocTitleAnnotation.class);
            title = title.replaceAll("\\([^()]*\\)", "");
            String[] strings = title.split("[_ ]");
            allowedTitles.addAll(Arrays.asList(strings));
        }

        List<String> ners = new ArrayList<>();
        FrequencyHashSet<String> tokenFrequencies = new FrequencyHashSet<>();
        Map<Integer, Map<Integer, List<String>>> candidates = new HashMap<>();

        if (annotation.containsKey(CoreAnnotations.TokensAnnotation.class)) {
            for (CoreLabel token : annotation.get(CoreAnnotations.TokensAnnotation.class)) {

                // todo: last ner missing if last token is not O
                if (token.containsKey(CoreAnnotations.NamedEntityTagAnnotation.class)) {
                    String ner = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                    if (!ner.equals("PERSON")) {
                        ner = "O";
                    }
                    if (ner.equals("O")) {
                        if (ners.size() > 0) {
                            boolean isContained = true;
                            for (String s : ners) {
                                if (!allowedTitles.contains(s)) {
                                    isContained = false;
                                }
                            }

                            if (isContained) {
                                for (String s : ners) {
                                    tokenFrequencies.add(s);
                                }

                                if (!candidates.containsKey(token.sentIndex())) {
                                    candidates.put(token.sentIndex(), new HashMap<>());
                                }
                                candidates.get(token.sentIndex()).put(token.index() - ners.size(), ners);
                            }
                            ners = new ArrayList<>();
                        }
                    } else {
                        ners.add(token.originalText());
                    }
                }

                String t = token.get(CoreAnnotations.OriginalTextAnnotation.class).toLowerCase();
                if (reverse.containsKey(t)) {
                    String sex = reverse.get(t);
                    frequencies.add(sex);
                    if (!words.containsKey(sex)) {
                        words.put(sex, HashMultimap.create());
                    }
                    words.get(sex).put(token.sentIndex(), token.index());
//                    total++;
//                    if (reverse.get(t).equals("m") || reverse.get(t).equals("f")) {
//                        totalMF++;
//                    }
                }
            }
        }

        // todo: check frequencies

//        System.out.println(candidates);
//        System.out.println(tokenFrequencies);

        String most = frequencies.mostFrequent();
        HashMultimap<Integer, Integer> corefChain = words.get(most);

        if (corefChain != null) {
            String mostFrequent = tokenFrequencies.mostFrequent();
            for (Integer sentID : candidates.keySet()) {
                for (Integer tokenID : candidates.get(sentID).keySet()) {
                    List<String> name = candidates.get(sentID).get(tokenID);
                    if (name.size() == 1 && !name.get(0).equals(mostFrequent)) {
                        continue;
                    }
                    for (int i = tokenID; i < tokenID + name.size(); i++) {
                        corefChain.put(sentID, i);
                    }
                }
            }
        }

        annotation.set(CustomAnnotations.SimpleCorefAnnotation.class, corefChain);

//        double f = frequencies.get("f") / (1.000 * annotation.get(CoreAnnotations.TokensAnnotation.class).size());
//        double m = frequencies.get("m") / (1.000 * annotation.get(CoreAnnotations.TokensAnnotation.class).size());
//        System.out.println(frequencies);
//        System.out.println(totalMF);
//        System.out.println(totalMF / (1.000 * annotation.get(CoreAnnotations.TokensAnnotation.class).size()));
//        for (String s : frequencies.keySet()) {
//            System.out.println(s + ": " + frequencies.get(s) / (1.000 * annotation.get(CoreAnnotations.TokensAnnotation.class).size()));
//        }

    }

    @Override public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
        return Collections.emptySet();
    }

    @Override public Set<Class<? extends CoreAnnotation>> requires() {
        return Collections.emptySet();
    }
}

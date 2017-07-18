package eu.fbk.fcw.wikipedia;

import com.google.common.collect.HashMultimap;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import eu.fbk.utils.core.FrequencyHashSet;
import eu.fbk.utils.core.PropertiesUtils;
import eu.fbk.utils.corenlp.CustomAnnotations;

import java.util.*;

/**
 * Created by alessio on 14/06/17.
 */

public class WikipediaCorefAnnotator implements Annotator {

    static final HashMultimap<String, String> patterns = HashMultimap.create();
    static final String CUSTOM_SEX = "C";

    static {
        // Leave "C" empty for custom sex
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
        patterns.put("i", "i");
        patterns.put("i", "my");
        patterns.put("i", "mine");
        patterns.put("i", "me");
        patterns.put("i", "myself");
    }

    static final boolean DEFAULT_SKIP_NAME = true;
    private boolean skipName = DEFAULT_SKIP_NAME;
    static final boolean DEFAULT_INCLUDE_PERSONS = true;
    private boolean includePersons = DEFAULT_INCLUDE_PERSONS;

    private String kind = null;
    private String wordListRaw = null;
    private Set<String> wordList = null;

    public WikipediaCorefAnnotator(String annotatorName, Properties prop) {

        // todo: use a singleton

        Properties localProperties = PropertiesUtils.dotConvertedProperties(prop, annotatorName);
        skipName = PropertiesUtils.getBoolean(localProperties.getProperty("skipName"), DEFAULT_SKIP_NAME);
        includePersons = PropertiesUtils.getBoolean(localProperties.getProperty("includePersons"), DEFAULT_INCLUDE_PERSONS);
        kind = localProperties.getProperty("kind");
        wordListRaw = localProperties.getProperty("wordList");
        if (kind != null && !patterns.containsKey(kind)) {
            kind = null;
        }
        if (wordListRaw != null) {
            String[] parts = wordListRaw.split("\\s*,\\s*");
            if (parts.length > 0) {
                wordList = new HashSet<>(Arrays.asList(parts));
            }

        }
    }

    @Override public void annotate(Annotation annotation) {
        FrequencyHashSet<String> frequencies = new FrequencyHashSet<>();
        Map<String, HashMultimap<Integer, Integer>> words = new HashMap<>();
        if (wordList != null) {
            words.put("C", HashMultimap.create());
        }

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
                if (includePersons && token.containsKey(CoreAnnotations.NamedEntityTagAnnotation.class)) {
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
                                int tokenIndex = token.index() - ners.size();
                                candidates.get(token.sentIndex()).put(tokenIndex, ners);

//                                System.out.println(String.format("NER: %s - %d %d", ners, token.sentIndex(), tokenIndex));
                            }
                            ners = new ArrayList<>();
                        }
                    } else {
                        ners.add(token.originalText());
                    }
                }

                String t = token.get(CoreAnnotations.OriginalTextAnnotation.class).toLowerCase();
                String sex = null;
                boolean add = false;
                if (wordList != null) {
                    sex = CUSTOM_SEX;
                    if (wordList.contains(t)) {
                        add = true;
                    }
                } else if (reverse.containsKey(t)) {
                    sex = reverse.get(t);
                    if (kind != null && !kind.equals(sex)) {
                        continue;
                    }

                    if (!words.containsKey(sex)) {
                        words.put(sex, HashMultimap.create());
                    }
                    add = true;
                }
                if (add) {
                    frequencies.add(sex);
                    words.get(sex).put(token.sentIndex(), token.index());
                }
            }
        }

        // todo: check frequencies

        String most = frequencies.mostFrequent();
        HashMultimap<Integer, Integer> corefChain = words.get(most);

        if (corefChain != null) {
            String mostFrequent = tokenFrequencies.mostFrequent();
            for (Integer sentID : candidates.keySet()) {
                for (Integer tokenID : candidates.get(sentID).keySet()) {
                    List<String> name = candidates.get(sentID).get(tokenID);
                    if (skipName) {
                        if (name.size() == 1 && !name.get(0).equals(mostFrequent)) {
                            continue;
                        }
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

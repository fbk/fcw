package eu.fbk.fcw.stemmer.corenlp;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import eu.fbk.fcw.stemmer.SnowballStemmer;
import eu.fbk.fcw.stemmer.ext.englishStemmer;
import eu.fbk.fcw.stemmer.ext.frenchStemmer;
import eu.fbk.fcw.stemmer.ext.germanStemmer;
import eu.fbk.fcw.stemmer.ext.italianStemmer;
import eu.fbk.utils.corenlp.CustomAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;

public class StemAnnotator implements Annotator {

    private static String DEFAULT_LANG = "en";
    private String lang;
    private static final Logger LOGGER = LoggerFactory.getLogger(StemAnnotator.class);

    public StemAnnotator(String annotatorName, Properties props) {
        lang = props.getProperty(annotatorName + ".lang");
    }

    @Override
    public void annotate(Annotation annotation) {
        if (lang == null) {
            lang = annotation.get(CustomAnnotations.LanguageAnnotation.class);
        }
        if (lang == null) {
            lang = DEFAULT_LANG;
        }

        SnowballStemmer stemmer;

        // For these languages Snowball is patched
        switch (lang) {
            case "en":
                stemmer = new englishStemmer();
                break;
            case "it":
                stemmer = new italianStemmer();
                break;
            case "fr":
                stemmer = new frenchStemmer();
                break;
            case "de":
                stemmer = new germanStemmer();
                break;
            default:
                LOGGER.warn("Language {} is not patched");
                return;
        }

        for (CoreLabel token : annotation.get(CoreAnnotations.TokensAnnotation.class)) {
            stemmer.setCurrent(token.originalText().replaceAll("\\s+", ""));
            stemmer.stem();
            String stemmed = stemmer.getCurrent();
            token.set(StemAnnotations.StemAnnotation.class, stemmed);
        }

    }

    @Override
    public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
        return Collections.singleton(StemAnnotations.StemAnnotation.class);
    }

    @Override
    public Set<Class<? extends CoreAnnotation>> requires() {
        return Collections.singleton(CoreAnnotations.TokensAnnotation.class);
    }
}

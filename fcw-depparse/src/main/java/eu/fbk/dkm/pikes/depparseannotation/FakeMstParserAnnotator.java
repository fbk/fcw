package eu.fbk.dkm.pikes.depparseannotation;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

import java.util.*;

/**
 * Created by alessio on 06/05/15.
 */

public class FakeMstParserAnnotator implements Annotator {

    public FakeMstParserAnnotator(String annotatorName, Properties props) {

    }

    @Override
    public void annotate(Annotation annotation) {
        if (annotation.containsKey(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                SemanticGraph dependencies = sentence.get(
                        SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
                if (dependencies != null) {
                    DepParseInfo info = new DepParseInfo(dependencies);
                    sentence.set(DepparseAnnotations.MstParserAnnotation.class, info);
                }
            }
        } else {
            throw new RuntimeException("unable to find words/tokens in: " + annotation);
        }
    }

    @Override public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
        return Collections.singleton(DepparseAnnotations.MstParserAnnotation.class);
    }

    @Override public Set<Class<? extends CoreAnnotation>> requires() {
        Set<Class<? extends CoreAnnotation>> requirements = new HashSet<>(Arrays.asList(
                CoreAnnotations.TextAnnotation.class,
                CoreAnnotations.TokensAnnotation.class,
                CoreAnnotations.CharacterOffsetBeginAnnotation.class,
                CoreAnnotations.CharacterOffsetEndAnnotation.class,
                CoreAnnotations.IndexAnnotation.class,
                CoreAnnotations.ValueAnnotation.class,
                CoreAnnotations.SentencesAnnotation.class,
                CoreAnnotations.SentenceIndexAnnotation.class,
                CoreAnnotations.PartOfSpeechAnnotation.class,
                CoreAnnotations.LemmaAnnotation.class,
                SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class
        ));
        return requirements;
    }

}
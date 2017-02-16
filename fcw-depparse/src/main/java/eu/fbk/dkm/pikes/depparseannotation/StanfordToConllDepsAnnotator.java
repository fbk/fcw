package eu.fbk.dkm.pikes.depparseannotation;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;

import java.util.*;

/**
 * Created by alessio on 06/05/15.
 */

public class StanfordToConllDepsAnnotator implements Annotator {

    public StanfordToConllDepsAnnotator(String annotatorName, Properties props) {

    }

    @Override
    public void annotate(Annotation annotation) {
        if (annotation.containsKey(CoreAnnotations.SentencesAnnotation.class)) {
            int sentOffset = 0;
            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                SemanticGraph dependencies = sentence.get(
                        SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
                DepParseInfo info = new DepParseInfo(dependencies);
                List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
                if (dependencies != null) {
                    for (int i = 0; i < tokens.size(); i++) {
                        CoreLabel token = tokens.get(i);
                        int j = i + sentOffset;

                        String label = info.getDepLabels().get(j + 1);
                        int head = info.getDepParents().get(j + 1) - 1 - sentOffset;
                        if (head < -1) {
                            head = -1;
                        }
                        token.set(CoreAnnotations.CoNLLDepTypeAnnotation.class, label);
                        token.set(CoreAnnotations.CoNLLDepParentIndexAnnotation.class, head);
                    }
                }
                sentOffset += tokens.size();
            }
        } else {
            throw new RuntimeException("unable to find words/tokens in: " + annotation);
        }
    }

    /**
     * Returns a set of requirements for which tasks this annotator can
     * provide.  For example, the POS annotator will return "pos".
     */
    @Override public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
        return Collections.singleton(DepparseAnnotations.MstParserAnnotation.class);
    }

    /**
     * Returns the set of tasks which this annotator requires in order
     * to perform.  For example, the POS annotator will return
     * "tokenize", "ssplit".
     */
    @Override public Set<Class<? extends CoreAnnotation>> requires() {
        return Collections.unmodifiableSet(new ArraySet<>(Arrays.asList(
                CoreAnnotations.TextAnnotation.class,
                CoreAnnotations.TokensAnnotation.class,
                CoreAnnotations.CharacterOffsetBeginAnnotation.class,
                CoreAnnotations.CharacterOffsetEndAnnotation.class,
                CoreAnnotations.IndexAnnotation.class,
                CoreAnnotations.ValueAnnotation.class,
                CoreAnnotations.SentencesAnnotation.class,
                CoreAnnotations.SentenceIndexAnnotation.class,
                CoreAnnotations.PartOfSpeechAnnotation.class,
                SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class
        )));
    }
}

package eu.fbk.fcw.pos;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import eu.fbk.fcw.pos.common.UPosAnnotatorInterface;
import eu.fbk.fcw.pos.common.Utils;
import eu.fbk.utils.corenlp.CustomAnnotations;

import java.util.*;

public class UPosAnnotator implements Annotator {

    private Map<String, String> posMap = new HashMap<>();
    private static String DEFAULT_LANGUAGE = "en";

    public UPosAnnotator(String annotatorName, Properties props) {
        String lang = Utils.getLanguage(annotatorName, props, DEFAULT_LANGUAGE);
        
        Class<?> myClass;
        try {
            String classToFind = this.getClass().getPackage().getName() + "." + lang + "." + this.getClass().getSimpleName();
            myClass = Class.forName(classToFind);
            if (!UPosAnnotatorInterface.class.isAssignableFrom(myClass)) {
                throw new ClassNotFoundException();
            }
        } catch (ClassNotFoundException e) {
            myClass = eu.fbk.fcw.pos.en.UPosAnnotator.class;
        }

        if (UPosAnnotatorInterface.class.isAssignableFrom(myClass)) {
            Object instance = null;
            try {
                instance = myClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
                return;
            }
            posMap = ((UPosAnnotatorInterface) instance).getMap();
        }
    }

    @Override public void annotate(Annotation annotation) {
        if (annotation.containsKey(CoreAnnotations.TokensAnnotation.class)) {
            for (CoreLabel token : annotation.get(CoreAnnotations.TokensAnnotation.class)) {
                if (token.containsKey(CoreAnnotations.PartOfSpeechAnnotation.class)) {
                    String genPos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                    String[] parts = genPos.split("\\+");
                    StringBuffer buffer = new StringBuffer();
                    for (String pos : parts) {
                        String upos = posMap.get(pos);
                        if (upos == null) {
                            upos = "X";
                        }
                        buffer.append("+").append(upos);
                    }

                    token.set(CustomAnnotations.UPosAnnotation.class, buffer.substring(1));
                }
            }

        }
    }

    @Override public Set<Class<? extends CoreAnnotation>> requirementsSatisfied() {
        return Collections.singleton(CustomAnnotations.UPosAnnotation.class);
    }

    @Override public Set<Class<? extends CoreAnnotation>> requires() {
        return new HashSet<>(Arrays.asList(
                CoreAnnotations.PartOfSpeechAnnotation.class,
                CoreAnnotations.TokensAnnotation.class
        ));
    }
}

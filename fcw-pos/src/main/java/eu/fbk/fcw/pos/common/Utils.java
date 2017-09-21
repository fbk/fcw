package eu.fbk.fcw.pos.common;

import java.util.Properties;

public class Utils {

    public static String getLanguage(String annotatorName, Properties props, String defaultlanguage) {
        String lang = props.getProperty(annotatorName + ".language");
        if (lang == null) {
            lang = props.getProperty(annotatorName + ".lang");
        }
        if (lang == null) {
            lang = props.getProperty("language");
        }
        if (lang == null) {
            lang = props.getProperty("lang");
        }
        if (lang == null) {
            lang = defaultlanguage;
        }
        return lang;
    }
}

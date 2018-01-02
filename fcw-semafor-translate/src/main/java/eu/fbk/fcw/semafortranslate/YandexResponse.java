package eu.fbk.fcw.semafortranslate;

import java.util.List;

public class YandexResponse {

    List<String> align;
    String code;
    String lang;
    List<String> text;

    @Override
    public String toString() {
        return "YandexResponse{" +
                "align=" + align +
                ", code='" + code + '\'' +
                ", lang='" + lang + '\'' +
                ", text=" + text +
                '}';
    }

    public List<String> getText() {
        return text;
    }
}

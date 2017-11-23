package eu.fbk.fcw.semafortranslate;

import java.util.List;

public class DeeplRequest {

    public static class Params {
        public int priority;
        public List<Job> jobs;
        Lang lang;

        public Params() {
        }
    }

    public static class Job {
        String kind;
        String raw_en_sentence;
    }

    public static class Lang {
        List<String> user_preferred_langs;
        String source_lang_user_selected;
        String target_lang;
    }

    public String jsonrc;
    public String method;
    public Integer id;
    public Params params;

    public DeeplRequest() {
    }
}

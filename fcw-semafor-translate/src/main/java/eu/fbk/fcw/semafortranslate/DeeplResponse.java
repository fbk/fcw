package eu.fbk.fcw.semafortranslate;

import java.util.List;

public class DeeplResponse {

    public static class Translation {
        public List<Beam> beams;
        public Integer timeAfterPreprocessing;
        public Integer timeReceivedFromEndpoint;
        public Integer timeSentToEndpoint;
        public Integer total_time_endpoint;
    }

    public static class Beam {
        public Integer num_symbols;
        public String postprocessed_sentence;
        public Double score;
        public Double totalLogProb;
    }

    public static class Result {
        public String source_lang;
        public String source_lang_is_confident;
        public String target_lang;
        public List<Translation> translations;
    }

    public Integer id;
    public String jsonrpc;
    public Result result;
}

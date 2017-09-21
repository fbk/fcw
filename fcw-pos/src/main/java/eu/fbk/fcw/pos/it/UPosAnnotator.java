package eu.fbk.fcw.pos.it;

import eu.fbk.fcw.pos.common.UPosAnnotatorInterface;

import java.util.HashMap;
import java.util.Map;

public class UPosAnnotator implements UPosAnnotatorInterface {

    private Map<String, String> posMap = new HashMap<>();

    public UPosAnnotator() {
        posMap.put("S", "NOUN");
        posMap.put("SP", "PROPN");

        posMap.put("E", "ADP");
        posMap.put("A", "ADJ");
        posMap.put("NO", "ADJ");

        posMap.put("RD", "DET");
        posMap.put("AP", "DET");
        posMap.put("DI", "DET");
        posMap.put("DD", "DET");
        posMap.put("RI", "DET");
        posMap.put("DQ", "DET");
        posMap.put("T", "DET");
        posMap.put("DR", "DET");
        posMap.put("DE", "DET");

        posMap.put("B", "ADV");
        posMap.put("BN", "ADV");

        posMap.put("V", "VERB");
        posMap.put("VA", "AUX");
        posMap.put("VM", "AUX");

        posMap.put("FF", "PUNCT");
        posMap.put("FS", "PUNCT");
        posMap.put("FB", "PUNCT");
        posMap.put("FC", "PUNCT");

        posMap.put("CC", "CCONJ");
        posMap.put("CS", "SCONJ");

        posMap.put("PC", "PRON");
        posMap.put("PI", "PRON");
        posMap.put("PR", "PRON");
        posMap.put("PQ", "PRON");
        posMap.put("PD", "PRON");
        posMap.put("PE", "PRON");
        posMap.put("PP", "PRON");

        posMap.put("PART", "PART");

        posMap.put("N", "NUM");
        posMap.put("SW", "X");
        posMap.put("X", "X");
        posMap.put("SYM", "SYM");
        posMap.put("I", "INTJ");
    }

    @Override public Map<String, String> getMap() {
        return posMap;
    }
}

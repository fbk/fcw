package eu.fbk.fcw.utils;

import com.google.common.collect.HashMultimap;
import edu.stanford.nlp.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by alessio on 07/02/17.
 */

public class ConllToken {

    String id;
    String form;
    String lemma = null;
    String upos = null;
    String xpos = null;
    HashMultimap<String, String> feats = null;

    Integer head = null;
    String deprel = null;

    List<Pair<Integer, String>> deps = null;
    String misc = null;
    String originalString = null;

    public ConllToken(String conllLine) throws Exception {
        conllLine = conllLine.trim();
        originalString = conllLine;
        String[] parts = conllLine.split("\t");
        if (parts.length < 10) {
            throw new Exception("Input line in wrong format: " + conllLine);
        }

        setId(parts[0]);
        setForm(parts[1]);

        if (!parts[2].equals("_")) {
            setLemma(parts[2]);
        }
        if (!parts[3].equals("_")) {
            setUpos(parts[3]);
        }
        if (!parts[4].equals("_")) {
            setXpos(parts[4]);
        }

        if (!parts[5].equals("_")) {
            String[] feats = parts[5].split("\\|");
            HashMultimap<String, String> featsMap = HashMultimap.create();
            for (String feat : feats) {
                String[] featParts = feat.split("=");
                if (featParts.length >= 2) {
                    String[] values = featParts[1].split(",");
                    for (String value : values) {
                        value = value.trim();
                        featsMap.put(featParts[0], value);
                    }
                }
            }
            setFeats(featsMap);
        }

        if (!parts[6].equals("_")) {
            setHead(Integer.parseInt(parts[6]));
        }
        if (!parts[7].equals("_")) {
            setDeprel(parts[7]);
        }

        if (!parts[8].equals("_")) {
            List<Pair<Integer, String>> depList = new ArrayList<>();
            String[] deps = parts[8].split("\\|");
            for (String dep : deps) {
                String[] depParts = dep.split(":");
                if (depParts.length >= 2) {
                    Pair<Integer, String> thisDep = new Pair(Integer.parseInt(depParts[0]), depParts[1]);
                    depList.add(thisDep);
                }
            }
            setDeps(depList);
        }

        if (!parts[9].equals("_")) {
            setMisc(parts[9]);
        }
    }

    @Override public String toString() {
        return "Token{" +
                "id='" + id + '\'' +
                ", form='" + form + '\'' +
                ", lemma='" + lemma + '\'' +
                ", upos='" + upos + '\'' +
                ", xpos='" + xpos + '\'' +
                ", feats=" + feats +
                ", head=" + head +
                ", deprel='" + deprel + '\'' +
                ", deps=" + deps +
                ", misc='" + misc + '\'' +
                '}';
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public String getUpos() {
        return upos;
    }

    public void setUpos(String upos) {
        this.upos = upos;
    }

    public String getXpos() {
        return xpos;
    }

    public void setXpos(String xpos) {
        this.xpos = xpos;
    }

    public Map<String, Collection<String>> getFeats() {
        return feats.asMap();
    }

    public void setFeats(HashMultimap<String, String> feats) {
        this.feats = feats;
    }

    public Integer getHead() {
        return head;
    }

    public void setHead(Integer head) {
        this.head = head;
    }

    public String getDeprel() {
        return deprel;
    }

    public void setDeprel(String deprel) {
        this.deprel = deprel;
    }

    public List<Pair<Integer, String>> getDeps() {
        return deps;
    }

    public void setDeps(List<Pair<Integer, String>> deps) {
        this.deps = deps;
    }

    public String getMisc() {
        return misc;
    }

    public void setMisc(String misc) {
        this.misc = misc;
    }
}

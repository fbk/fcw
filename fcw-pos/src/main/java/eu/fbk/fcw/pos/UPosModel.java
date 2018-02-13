package eu.fbk.fcw.pos;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class UPosModel {

    private static Map<String, UPosModel> instances = new HashMap<>();
    private Map<String, String> uposMap;

    public static UPosModel getInstance(String fileName) {
        if (!instances.containsKey(fileName)) {
            Map<String, String> uposMap = new HashMap<>();

            try {
                BufferedReader reader = new BufferedReader(new FileReader(fileName));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\t");
                    if (parts.length < 2) {
                        continue;
                    }
                    String pos1 = parts[0].trim();
                    String pos2 = parts[1].trim();
                    if (pos2.equals(".")) {
                        pos2 = "PUNCT";
                    }
                    uposMap.put(pos1, pos2);
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            instances.put(fileName, new UPosModel(uposMap));
        }
        return instances.get(fileName);
    }

    public UPosModel(Map<String, String> uposMap) {
        this.uposMap = uposMap;
    }

    public Map<String, String> getUposMap() {
        return uposMap;
    }
}

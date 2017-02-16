package eu.fbk.fcw.udpipe.api;

/**
 * Created by alessio on 07/02/17.
 */

public class UDPipe {

    private String model;
    private String[] acknowledgements;
    private String result;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String[] getAcknowledgements() {
        return acknowledgements;
    }

    public void setAcknowledgements(String[] acknowledgements) {
        this.acknowledgements = acknowledgements;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}

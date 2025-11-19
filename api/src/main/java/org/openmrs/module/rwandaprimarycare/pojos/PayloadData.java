package org.openmrs.module.rwandaprimarycare.pojos;

public class PayloadData {

    private String url;
    private String method;
    private String payload;
    private String type;
    private Integer patientId;

    public PayloadData() {
    }

    public PayloadData(String url, String method, String payload, String type, Integer patientId) {
        this.url = url;
        this.method = method;
        this.payload = payload;
        this.type = type;
        this.patientId = patientId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getPatientId() {
        return patientId;
    }

    public void setPatientId(Integer patientId) {
        this.patientId = patientId;
    }
}

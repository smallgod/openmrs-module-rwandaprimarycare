package org.openmrs.module.rwandaprimarycare.pojos.upiIdGeneratorResponse;

public class UPIDGeneratorResponse {

    public String status;
    public UPIDGeneratorResponseData data;
    public String message;
    public boolean isOffline = false;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UPIDGeneratorResponseData getData() {
        return data;
    }

    public void setData(UPIDGeneratorResponseData data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

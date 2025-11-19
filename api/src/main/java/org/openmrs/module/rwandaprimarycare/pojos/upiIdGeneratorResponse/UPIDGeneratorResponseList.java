package org.openmrs.module.rwandaprimarycare.pojos.upiIdGeneratorResponse;

import java.util.List;

public class UPIDGeneratorResponseList {

    public String status;
    public List<UPIDGeneratorResponseData> data;
    public String message;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<UPIDGeneratorResponseData> getData() {
        return data;
    }

    public void setData(List<UPIDGeneratorResponseData> data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

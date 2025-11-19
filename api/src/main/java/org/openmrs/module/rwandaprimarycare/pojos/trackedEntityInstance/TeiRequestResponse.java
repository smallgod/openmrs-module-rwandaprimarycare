package org.openmrs.module.rwandaprimarycare.pojos.trackedEntityInstance;

/**
 * Placeholder for DHIS2 Tracked Entity Instance response
 * Used for DHIS2 integration (optional feature)
 */
public class TeiRequestResponse {

    private String status;
    private Object data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}

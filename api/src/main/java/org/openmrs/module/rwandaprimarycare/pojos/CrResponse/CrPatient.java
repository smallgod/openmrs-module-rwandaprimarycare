package org.openmrs.module.rwandaprimarycare.pojos.CrResponse;

import java.util.List;

public class CrPatient {

    private String resourceType;
    private String id;
    //private Meta meta;
    private String type;
    private Integer total;
    //private List<Link> link = null;
    private List<Entry> entry = null;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<Entry> getEntry() {
        return entry;
    }

    public void setEntry(List<Entry> entry) {
        this.entry = entry;
    }
}

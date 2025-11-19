package org.openmrs.module.rwandaprimarycare.pojos.patient;

public class Identifier {

    private String system;
    private String value;
    private String use;

    public Identifier() {
    }

    public Identifier(String system, String value) {
        this.system = system;
        this.value = value;
    }

    public Identifier(String system, String value, String use) {
        this.system = system;
        this.value = value;
        this.use = use;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUse() {
        return use;
    }

    public void setUse(String use) {
        this.use = use;
    }
}

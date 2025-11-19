package org.openmrs.module.rwandaprimarycare.pojos.CrResponse;

import java.util.List;

public class Contact {

    private List<String> relationship;
    public Name name;
    private List<Telecom> telecom;
    private Address address;
    private String gender;

    public List<String> getRelationship() {
        return relationship;
    }

    public void setRelationship(List<String> relationship) {
        this.relationship = relationship;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public List<Telecom> getTelecom() {
        return telecom;
    }

    public void setTelecom(List<Telecom> telecom) {
        this.telecom = telecom;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}

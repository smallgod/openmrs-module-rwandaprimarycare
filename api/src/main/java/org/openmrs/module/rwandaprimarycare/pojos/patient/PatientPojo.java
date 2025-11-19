package org.openmrs.module.rwandaprimarycare.pojos.patient;

import java.util.List;

public class PatientPojo {

    private String openMrsId;
    private String surName;
    private String postNames;
    private String fatherName;
    private String motherName;
    private String spouse;
    private String dateOfBirth;
    private String age;
    private String gender;
    private String maritalStatus;
    private List<Identifier> identifiers;
    private String phoneNumber;
    private String nationality;
    private List<Address> addressList;
    private String origin;
    private String originRank;
    private String educationalLevel;
    private String profession;
    private String religion;
    private Boolean citizenStatus;
    private String registeredOn;

    public String getOpenMrsId() {
        return openMrsId;
    }

    public void setOpenMrsId(String openMrsId) {
        this.openMrsId = openMrsId;
    }

    public String getSurName() {
        return surName == null ? "" : surName;
    }

    public void setSurName(String surName) {
        this.surName = surName;
    }

    public String getPostNames() {
        return postNames == null ? "" : postNames;
    }

    public void setPostNames(String postNames) {
        this.postNames = postNames;
    }

    public String getFatherName() {
        return fatherName == null ? "" : fatherName;
    }

    public void setFatherName(String fatherName) {
        this.fatherName = fatherName;
    }

    public String getMotherName() {
        return motherName == null ? "" : motherName;
    }

    public void setMotherName(String motherName) {
        this.motherName = motherName;
    }

    public String getSpouse() {
        return spouse == null ? "" : spouse;
    }

    public void setSpouse(String spouse) {
        this.spouse = spouse;
    }

    public String getDateOfBirth() {
        return dateOfBirth == null ? "" : dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAge() {
        return age == null ? "" : age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getGender() {
        return gender == null ? "" : gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMaritalStatus() {
        return maritalStatus == null ? "" : maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public List<Identifier> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(List<Identifier> identifiers) {
        this.identifiers = identifiers;
    }

    public String getPhoneNumber() {
        return phoneNumber == null ? "" : phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getNationality() {
        return nationality == null ? "" : nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public List<Address> getAddressList() {
        return addressList;
    }

    public void setAddressList(List<Address> addressList) {
        this.addressList = addressList;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getOriginRank() {
        return originRank;
    }

    public void setOriginRank(String originRank) {
        this.originRank = originRank;
    }

    public String getEducationalLevel() {
        return educationalLevel == null ? "" : educationalLevel;
    }

    public void setEducationalLevel(String educationalLevel) {
        this.educationalLevel = educationalLevel;
    }

    public String getProfession() {
        return profession == null ? "" : profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public String getReligion() {
        return religion == null ? "" : religion;
    }

    public void setReligion(String religion) {
        this.religion = religion;
    }

    public Boolean getCitizenStatus() {
        return citizenStatus ;
    }

    public void setCitizenStatus(Boolean citizenStatus) {
        this.citizenStatus = citizenStatus;
    }

    public String getRegisteredOn() {
        return registeredOn;
    }

    public void setRegisteredOn(String registeredOn) {
        this.registeredOn = registeredOn;
    }
}

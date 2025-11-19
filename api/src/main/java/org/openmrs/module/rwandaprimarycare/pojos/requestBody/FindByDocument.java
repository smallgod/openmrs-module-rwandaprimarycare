package org.openmrs.module.rwandaprimarycare.pojos.requestBody;

public class FindByDocument {

    private String documentNumber;
    private String documentType;
    private String fosaid;

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getFosaid() {
        return fosaid;
    }

    public void setFosaid(String fosaid) {
        this.fosaid = fosaid;
    }
}

package org.openmrs.module.rwandaprimarycare;

import com.google.gson.JsonObject;
import org.openmrs.Patient;
import org.openmrs.module.rwandaprimarycare.pojos.patient.Address;
import org.openmrs.module.rwandaprimarycare.pojos.patient.PatientPojo;
import org.openmrs.module.rwandaprimarycare.pojos.upiIdGeneratorResponse.UPIDGeneratorResponse;
import org.openmrs.module.rwandaprimarycare.pojos.upiIdGeneratorResponse.UPIDGeneratorResponseData;

import java.util.List;

public class PatientDataTransfer {

    /**
     * Generate UPID request from search parameters
     *
     * @param docType     Document type (NID, PASSPORT, etc.)
     * @param search      Search value (document number)
     * @param fosaid      Facility ID
     * @param patientPojo Optional patient data for offline generation
     * @return UPID generator request object
     */
    public static UPIDGeneratorResponse generateUpidRequest(String docType, String search,
                                                            String fosaid, PatientPojo patientPojo) {
        UPIDGeneratorResponse request = new UPIDGeneratorResponse();
        UPIDGeneratorResponseData data = new UPIDGeneratorResponseData();

        if (patientPojo != null) {
            // Populate from patient POJO
            data.setSurName(patientPojo.getSurName());
            data.setPostNames(patientPojo.getPostNames());
            data.setSex(patientPojo.getGender());
            data.setDateOfBirth(patientPojo.getDateOfBirth());
            data.setFatherName(patientPojo.getFatherName());
            data.setMotherName(patientPojo.getMotherName());
            data.setSpouseName(patientPojo.getSpouse());
        }

        // Set document information
        if ("NID".equalsIgnoreCase(docType)) {
            data.setNid(search);
        } else if ("NIN".equalsIgnoreCase(docType)) {
            data.setNin(search);
        } else if ("NID_APPLICATION_NUMBER".equalsIgnoreCase(docType)) {
            data.setApplicationNumber(search);
        }

        request.setData(data);
        return request;
    }

    /**
     * Reformat Patient object to Client Registry FHIR JSON format
     *
     * @param patient Patient object
     * @param nationalId National ID
     * @param pcId Primary Care ID
     * @param givenName Given name
     * @param familyName Family name
     * @param gender Gender
     * @param age Age
     * @param birthdateDay Birth day
     * @param birthdateMonth Birth month
     * @param birthdateYear Birth year
     * @param addressList Address list
     * @param mothersName Mother's name
     * @param fathersName Father's name
     * @param spouse Spouse name
     * @param maritalStatus Marital status
     * @return JsonObject formatted for Client Registry
     */
    public JsonObject reformatPatientToCr(Patient patient, String nationalId, String pcId,
                                          String givenName, String familyName, String gender,
                                          Integer age, Integer birthdateDay, Integer birthdateMonth,
                                          Integer birthdateYear, List<Address> addressList,
                                          String mothersName, String fathersName,
                                          String spouse, String maritalStatus) {
        JsonObject patientJson = new JsonObject();

        // Resource type
        patientJson.addProperty("resourceType", "Patient");

        // Basic demographics
        if (givenName != null) patientJson.addProperty("givenName", givenName);
        if (familyName != null) patientJson.addProperty("familyName", familyName);
        if (gender != null) patientJson.addProperty("gender", gender.toLowerCase());

        // Birth information
        if (patient != null && patient.getBirthdate() != null) {
            patientJson.addProperty("birthDate", new java.text.SimpleDateFormat("yyyy-MM-dd")
                .format(patient.getBirthdate()));
        }

        // Identifiers
        if (pcId != null && !pcId.isEmpty()) {
            patientJson.addProperty("primaryCareId", pcId);
        }
        if (nationalId != null && !nationalId.isEmpty()) {
            patientJson.addProperty("nationalId", nationalId);
        }

        // Contact information
        if (mothersName != null) patientJson.addProperty("mothersName", mothersName);
        if (fathersName != null) patientJson.addProperty("fathersName", fathersName);
        if (spouse != null) patientJson.addProperty("spouse", spouse);
        if (maritalStatus != null) patientJson.addProperty("maritalStatus", maritalStatus);

        // Address information
        if (addressList != null && !addressList.isEmpty()) {
            Address primaryAddress = addressList.get(0);
            if (primaryAddress.getCountry() != null) patientJson.addProperty("country", primaryAddress.getCountry());
            if (primaryAddress.getState() != null) patientJson.addProperty("province", primaryAddress.getState());
            if (primaryAddress.getDistrict() != null) patientJson.addProperty("district", primaryAddress.getDistrict());
            if (primaryAddress.getSector() != null) patientJson.addProperty("sector", primaryAddress.getSector());
            if (primaryAddress.getCell() != null) patientJson.addProperty("cell", primaryAddress.getCell());
        }

        return patientJson;
    }
}

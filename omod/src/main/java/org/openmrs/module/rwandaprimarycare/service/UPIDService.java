package org.openmrs.module.rwandaprimarycare.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.rwandaprimarycare.pojos.patient.PatientPojo;
import org.openmrs.module.rwandaprimarycare.pojos.upiIdGeneratorResponse.UPIDGeneratorResponse;
import org.openmrs.module.rwandaprimarycare.pojos.upiIdGeneratorResponse.UPIDGeneratorResponseData;

import java.util.UUID;

/**
 * Service for generating UPID (Unique Patient ID) offline
 * when NPR service is unavailable
 */
public class UPIDService {

    protected final Log log = LogFactory.getLog(UPIDService.class);

    private static UPIDService instance;

    private UPIDService() {
    }

    public static synchronized UPIDService getInstance() {
        if (instance == null) {
            instance = new UPIDService();
        }
        return instance;
    }

    /**
     * Build offline UPID response from patient data
     *
     * @param docType     Document type
     * @param search      Search value
     * @param patientPojo Patient data
     * @return UPID generator response (offline fallback)
     */
    public UPIDGeneratorResponse build(String docType, String search, PatientPojo patientPojo) {
        UPIDGeneratorResponse response = new UPIDGeneratorResponse();
        UPIDGeneratorResponseData data = new UPIDGeneratorResponseData();

        try {
            // Generate offline UPID (temporary)
            String offlineUPID = generateOfflineUPID();
            data.setUpi(offlineUPID);

            // Copy patient data
            if (patientPojo != null) {
                data.setSurName(patientPojo.getSurName());
                data.setPostNames(patientPojo.getPostNames());
                data.setSex(patientPojo.getGender());
                data.setDateOfBirth(patientPojo.getDateOfBirth());
                data.setNationality(patientPojo.getNationality());
                data.setMaritalStatus(patientPojo.getMaritalStatus());
                data.setFatherName(patientPojo.getFatherName());
                data.setMotherName(patientPojo.getMotherName());
            }

            // Set document identifier
            if ("NID".equalsIgnoreCase(docType)) {
                data.setNid(search);
            } else if ("NIN".equalsIgnoreCase(docType)) {
                data.setNin(search);
            } else if ("NID_APPLICATION_NUMBER".equalsIgnoreCase(docType)) {
                data.setApplicationNumber(search);
            }

            response.setData(data);
            response.setStatus("ok");
            response.setMessage("Offline UPID generated");

        } catch (Exception e) {
            log.error("Error generating offline UPID: " + e.getMessage(), e);
            response.setStatus("error");
            response.setMessage("Failed to generate offline UPID");
        }

        return response;
    }

    /**
     * Generate offline UPID (temporary identifier)
     * Format: OFFLINE-{UUID}
     */
    private String generateOfflineUPID() {
        return "OFFLINE-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    /**
     * Save UPID for offline synchronization
     * Stores the UPID response for later syncing when online
     *
     * @param upidGeneratorResponse Response to save
     */
    public void saveForOffline(UPIDGeneratorResponse upidGeneratorResponse) {
        try {
            // TODO: Implement offline storage logic
            // This would typically save to a queue or database for later sync
            log.info("Saving UPID for offline sync: " + upidGeneratorResponse.data.upi);
        } catch (Exception e) {
            log.error("Error saving UPID for offline: " + e.getMessage(), e);
        }
    }
}

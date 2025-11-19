package org.openmrs.module.rwandaprimarycare.service;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandaprimarycare.PrimaryCareConstants;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

/**
 * Service for DHIS2 integration
 * Handles authentication and HTTP entity creation for DHIS2 requests
 */
public class DhisService {

    protected static final Log log = LogFactory.getLog(DhisService.class);

    /**
     * Create HTTP entity with OpenHIM authentication headers for DHIS2 requests
     *
     * @return HTTP entity with authentication headers
     */
    public static HttpEntity<String> openHIMHttpEntity() {
        try {
            String username = Context.getAdministrationService()
                    .getGlobalProperty(PrimaryCareConstants.GLOBAL_PROPERTY_OPENHIM_USER_NAME);
            String password = Context.getAdministrationService()
                    .getGlobalProperty(PrimaryCareConstants.GLOBAL_PROPERTY_OPENHIM_USER_PWD);

            if (username == null || password == null) {
                log.error("OpenHIM credentials not configured for DHIS2 access");
                return new HttpEntity<String>(new HttpHeaders());
            }

            // Create Basic Auth header
            String plainCreds = username + ":" + password;
            byte[] plainCredsBytes = plainCreds.getBytes();
            byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
            String base64Creds = new String(base64CredsBytes);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Basic " + base64Creds);
            headers.add("Content-Type", "application/json");

            return new HttpEntity<String>(headers);

        } catch (Exception e) {
            log.error("Error creating OpenHIM HTTP entity: " + e.getMessage(), e);
            return new HttpEntity<String>(new HttpHeaders());
        }
    }
}

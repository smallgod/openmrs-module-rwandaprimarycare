package org.openmrs.module.rwandaprimarycare.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;

public class FhirUtils {

    protected static final Log log = LogFactory.getLog(FhirUtils.class);

    /**
     * Check if the system is configured to use FHIR format
     *
     * This can be controlled via global property for flexibility
     *
     * @return true if FHIR format is enabled, false otherwise
     */
    public static boolean isFhirFormat() {
        try {
            String fhirEnabled = Context.getAdministrationService()
                    .getGlobalProperty("primaryCare.fhir.enabled");

            return "true".equalsIgnoreCase(fhirEnabled);
        } catch (Exception e) {
            log.warn("Error checking FHIR format setting: " + e.getMessage());
            return false;
        }
    }
}

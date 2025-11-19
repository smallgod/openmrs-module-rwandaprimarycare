package org.openmrs.module.rwandaprimarycare.constants;

public class AppConstants {

    // Response statuses
    public static final String RESPONSE_SUCCESS = "SUCCESS";
    public static final String RESPONSE_FAILURE = "FAILURE";

    // OpenHIM connection statuses
    public static final String OPENHIM_DEFINED = "DEFINED";
    public static final String OPENHIM_URL_UNDEFINED = "URL_UNDEFINED";
    public static final String OPENHIM_CLIENT_ID_UNDEFINED = "CLIENT_ID_UNDEFINED";
    public static final String OPENHIM_PASSWORD_UNDEFINED = "PASSWORD_UNDEFINED";

    // Patient origin
    public static final String ORIGIN_CR = "CR";
    public static final String ORIGIN_NPR = "NPR";
    public static final String ORIGIN_LOCAL = "LOCAL";

    // Patient origin ranks
    public static final String RANK_CR_ONLY = "CR_ONLY";
    public static final String RANK_NPR_ONLY = "NPR_ONLY";
    public static final String RANK_LOCAL_ONLY = "LOCAL_ONLY";
    public static final String RANK_CR_LOCAL = "CR_LOCAL";

    // Address types
    public static final String DOMICILE_ADDRESS = "DOMICILE";
    public static final String RESIDENTIAL_ADDRESS = "RESIDENTIAL";
    public static final String POSTAL_ADDRESS = "POSTAL";

    // Contact relationship types
    public static final String FATHER_NAME = "Father";
    public static final String MOTHER_NAME = "Mother";
    public static final String SPOUSE_NAME = "Spouse";

    // Patient extensions (FHIR)
    public static final String EDUCATIONAL_LEVEL = "educationalLevel";
    public static final String PROFESSION = "profession";
    public static final String RELIGION = "religion";
    public static final String NATIONALITY = "nationality";
    public static final String REGISTERED_ON = "registeredOn";

    // Name indexes
    public static final int POST_NAME_INDEX = 0;

    // Operation statuses
    public static final String COMPLETED_STATUS = "COMPLETED";
}

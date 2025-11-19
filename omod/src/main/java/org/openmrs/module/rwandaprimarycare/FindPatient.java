package org.openmrs.module.rwandaprimarycare;


import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandaprimarycare.constants.AppConstants;
import org.openmrs.module.rwandaprimarycare.pojos.globalResponse.ResponseDTO;
import org.openmrs.module.rwandaprimarycare.pojos.location.LocationPojo;
import org.openmrs.module.rwandaprimarycare.pojos.patient.Identifier;
import org.openmrs.module.rwandaprimarycare.pojos.patient.PatientPojo;
import org.openmrs.module.rwandaprimarycare.pojos.requestBody.FindByDocument;
import org.openmrs.module.rwandaprimarycare.pojos.requestBody.FindByName;
import org.openmrs.module.rwandaprimarycare.pojos.trackedEntityInstance.TeiRequestResponse;
import org.openmrs.module.rwandaprimarycare.service.DhisService;
import org.openmrs.module.rwandaprimarycare.service.FindPatientService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@Controller
public class FindPatient {

    protected final Log log = LogFactory.getLog(getClass());
    

    @RequestMapping(value = "/module/rwandaprimarycare/findPatient", method = RequestMethod.GET)
    public String get(HttpSession session) {
        if (!Context.isAuthenticated() || PrimaryCareBusinessLogic.getLocationLoggedIn(session) == null) {
            return "redirect:/module/rwandaprimarycare/login/login.form";
        } else {
            return null;
        }
    }


    @RequestMapping(value = "/rwandaprimarycare/findPatient/byDocument", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<Object> findPatientByDocumentAjax(
            @RequestParam("documentNumber") String documentNumber,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "fosaid", required = false) String fosaid
    ) {

        if (documentType.equalsIgnoreCase("TEMPID")) {
            documentNumber = CustomUtils.generateTempId();
        }
        FindPatientService findPatientService = new FindPatientService();
        ResponseDTO result = findPatientService.findPatientAjax(documentNumber, documentType, fosaid);
        ResponseEntity<Object> response = new ResponseEntity<Object>(result, HttpStatus.OK);
        return response;

    }

    @RequestMapping(value = "/rwandaprimarycare/findPatient/byNames", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<Object> findPatientByNameAjax(
            @RequestParam("surName") String surName,
            @RequestParam(value = "postName", required = false) String postName,
            @RequestParam(value = "yearOfBirth", required = false) String yearOfBirth,
            @RequestParam(value = "origin", required = false) String origin
    ) {
        FindPatientService findPatientService = new FindPatientService();
        ResponseDTO result = findPatientService.findPatientAjax(surName, postName, yearOfBirth, origin);
        return new ResponseEntity<Object>(result, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/rwandaprimarycare/findPatient/generateTracnet", produces = "application/json")
	@ResponseBody
	public ResponseEntity<String> generateTracnet(@RequestParam("patientId") int patientId){
        String fosaID = Context.getAdministrationService().getGlobalProperty("registration.defaultLocationCode");
        String locationId = Context.getAdministrationService().getGlobalProperty("encounter.defaultLocation");
        String prefix = Context.getAdministrationService().getGlobalProperty("registration.tracknetid.patientpart.prefix");
        return new ResponseEntity<String>(PrimaryCareBusinessLogic.getService().createLastTracnetId(fosaID, patientId, locationId,prefix), HttpStatus.CREATED);
	}

    @RequestMapping(method = RequestMethod.POST, value = "/rwandaprimarycare/findPatient/create")
    @ResponseBody
    public ResponseEntity createPatient(
            @RequestBody PatientPojo patient,
            HttpSession session
    ) {
        ResponseEntity<Object> response = null;
        FindPatientService findPatientService = new FindPatientService();
        String result = findPatientService.createPatient(patient, session);
        if (result != null && !result.isEmpty()) {
            response = new ResponseEntity<Object>(result, HttpStatus.CREATED);
        } else {
            response = new ResponseEntity<Object>(result, HttpStatus.BAD_REQUEST);
        }

        return response;

    }

    @RequestMapping(method = RequestMethod.PUT, value = "/rwandaprimarycare/findPatient/update")
    @ResponseBody
    public ResponseEntity updatePatient(
            @RequestBody PatientPojo patient,
            HttpSession session
    ) {
        ResponseEntity<Object> response = null;
        FindPatientService findPatientService = new FindPatientService();
        Boolean flag = false;
        String identifier = "";

        for (Identifier id : patient.getIdentifiers()) {
            if (id.getSystem().contains("UPI")) {
                identifier = id.getValue();
                flag = true;
            }
        }


        if (identifier.isEmpty()) {
            for (Identifier id : patient.getIdentifiers()) {
                if (id.getSystem().contains("PRIMARY_CARE_ID")) {
                    identifier = id.getValue();
                    flag = true;
                }
            }
        }
        log.debug("identifier flag== - " + flag);
        log.debug("identifier value== - identifier" + identifier);
        if (flag) {
            String result = findPatientService.update(patient, identifier, true, session);
            if (result != null && !result.isEmpty()) {
                response = new ResponseEntity<Object>(result, HttpStatus.OK);
            } else {
                log.debug("Conflict== - " + flag);
                response = new ResponseEntity<Object>(result, HttpStatus.CONFLICT);
            }
        } else {
            log.debug("Conflict== - " + flag);
            return new ResponseEntity<Object>("Patient Primary Care Id Not found!", HttpStatus.CONFLICT);
        }

        return response;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/rwandaprimarycare/orgUnit")
    @ResponseBody
    public ResponseEntity<String> getDhisOrgUnit(){
        log.error("Checking the List Of Organization Units");
        final String dhis2ServerPrefix = Context.getAdministrationService().getGlobalProperty(PrimaryCareConstants.GLOBAL_PROPERTY_DHIS2_PREFIX);
        final String openhimDhisUrl = Context.getAdministrationService().getGlobalProperty(PrimaryCareConstants.GLOBAL_PROPERTY_OPENHIM_NIDA_API) + (dhis2ServerPrefix.toLowerCase().indexOf("null") != 0 ?dhis2ServerPrefix:"") + "/api";

        try{
            HttpEntity<String> orgUnitRequest = DhisService.openHIMHttpEntity();

            String url = openhimDhisUrl + "/organisationUnits?paging=false&fields=id,name,displayName,code";
            log.error("URL: " + url);
            return (new RestTemplate()).exchange(url, HttpMethod.GET, orgUnitRequest, String.class);
        } catch(Exception e){
            log.error(e.getMessage());
        }
        return null;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/rwandaprimarycare/findPatient/location")
    @ResponseBody
    public ResponseEntity<ResponseDTO> getLocationByDhis2UUID(@RequestParam(value = "facility_code", required = false) Integer locationId, HttpSession session) {

        log.info("facility_code" + locationId);
        ResponseDTO responseDTO = new ResponseDTO();
        ResponseEntity<ResponseDTO> response = null;
        try {
            FindPatientService findPatientService = new FindPatientService();
            if (locationId != null) {
                Location location = findPatientService.getLocationById(locationId);
                responseDTO.setStatus(AppConstants.RESPONSE_SUCCESS);
                responseDTO.setResults(new LocationPojo(location));
            } else {
                List<Location> locations = findPatientService.getAllLocation();
                responseDTO.setStatus(AppConstants.RESPONSE_SUCCESS);
                List<LocationPojo> locationPojoList = new ArrayList<LocationPojo>();
                for (Location location : locations) {
                    locationPojoList.add(new LocationPojo(location));
                }
                responseDTO.setResults(locationPojoList);
            }

            response = new ResponseEntity<ResponseDTO>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            responseDTO.setStatus(AppConstants.RESPONSE_FAILURE);
            response = new ResponseEntity<ResponseDTO>(responseDTO, HttpStatus.NOT_FOUND);
        }

        return response;
    }

    @RequestMapping(value = "/rwandaprimarycare/findPatient/byDocument/fhir", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<Object> findPatientByDocumentFhir(
            @RequestParam("documentNumber") String documentNumber,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "fosaid", required = false) String fosaid
    ) {

        FindPatientService findPatientService = new FindPatientService();
        ResponseDTO result = findPatientService.findPatientAjax(documentNumber, documentType, fosaid);
        ResponseEntity<Object> response = new ResponseEntity<Object>(result, HttpStatus.OK);
        return response;

    }

}

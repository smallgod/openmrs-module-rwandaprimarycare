package org.openmrs.module.rwandaprimarycare.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.LocalDate;
import org.joda.time.Years;
import org.openmrs.*;
import org.openmrs.api.IdentifierNotUniqueException;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Credentials;
import org.openmrs.api.context.UsernamePasswordCredentials;
import org.openmrs.module.mohbilling.businesslogic.InsurancePolicyUtil;
import org.openmrs.module.mohbilling.model.InsurancePolicy;
import org.openmrs.module.rwandaprimarycare.*;
import org.openmrs.module.rwandaprimarycare.constants.AppConstants;
import org.openmrs.module.rwandaprimarycare.pojos.CrResponse.CodeValue;
import org.openmrs.module.rwandaprimarycare.pojos.CrResponse.Contact;
import org.openmrs.module.rwandaprimarycare.pojos.CrResponse.CrPatient;
import org.openmrs.module.rwandaprimarycare.pojos.CrResponse.Entry;
import org.openmrs.module.rwandaprimarycare.pojos.CrResponse.Extension;
import org.openmrs.module.rwandaprimarycare.pojos.CrResponse.Name;
import org.openmrs.module.rwandaprimarycare.pojos.CrResponse.Resource;
import org.openmrs.module.rwandaprimarycare.pojos.CrResponse.Telecom;
import org.openmrs.module.rwandaprimarycare.pojos.OfflineTransaction;
import org.openmrs.module.rwandaprimarycare.pojos.OpenHimConnection;
import org.openmrs.module.rwandaprimarycare.pojos.PayloadData;
import org.openmrs.module.rwandaprimarycare.pojos.globalResponse.ResponseDTO;
import org.openmrs.module.rwandaprimarycare.pojos.patient.Address;
import org.openmrs.module.rwandaprimarycare.pojos.patient.Identifier;
import org.openmrs.module.rwandaprimarycare.pojos.patient.PatientPojo;
import org.openmrs.module.rwandaprimarycare.pojos.upiIdGeneratorResponse.UPIDGeneratorResponse;
import org.openmrs.module.rwandaprimarycare.pojos.upiIdGeneratorResponse.UPIDGeneratorResponseData;
import org.openmrs.module.rwandaprimarycare.pojos.upiIdGeneratorResponse.UPIDGeneratorResponseList;
import org.openmrs.module.rwandaprimarycare.utils.FhirUtils;
import org.openmrs.util.PrivilegeConstants;
import org.openmrs.web.WebConstants;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.openmrs.module.rwandaprimarycare.PatientDataTransfer.generateUpidRequest;
import static org.openmrs.module.rwandaprimarycare.constants.AppConstants.*;
import static org.springframework.util.StringUtils.hasText;

public class FindPatientService {
    protected final Log log = LogFactory.getLog(FindPatientService.class);
    OpenHimConnection openHimConnection = getOpenHimConnection();

    public ResponseDTO findPatientAjax(String identifier, String identifierType, String fosaid) {

        log.info("Print from FindPatientService" + identifier + " | " + identifierType);
        openHimConnection = getOpenHimConnection();
        ResponseDTO responseDTO = null;
        List<PatientPojo> results = new ArrayList<PatientPojo>();


                /*TODO 1.Local Search #RFC-84
                       2.Check and add Orgin and Rank
                * */
        //Search in Local
        if (!identifierType.equals("TEMPID")) responseDTO = checkPatientLocal(identifier, identifierType, results);

        if (responseDTO != null) {
            log.info("retuning LOCAL Response");
            return responseDTO;
        }

        if(identifierType.equals("PRIMARY_CARE")){
            responseDTO = new ResponseDTO();
            responseDTO.setStatus(AppConstants.RESPONSE_FAILURE);
            return responseDTO;
        }

        if (CustomUtils.isOnline() && openHimConnection.getStatus().equals(AppConstants.OPENHIM_DEFINED)) {
            //Check CR
            CrPatient crPatient = checkPatientCR(identifier, identifierType);
            log.info("Print from checkPatientCR " + crPatient);
            if (crPatient != null && crPatient.getEntry() != null) {

                /*TODO 1.getCrResponse
                 * */
                responseDTO = getCrResponse(crPatient, results);
                log.info("Print from getCrResponse " + responseDTO);
                if (responseDTO != null) {
                    log.info("retuning CR response");
                    return responseDTO;
                }
            } else {
                log.info("retuning CR openHimConnection.getStatus().equals(AppConstants.OPENHIM_DEFINED)");
            }

            if (!identifierType.equals("INSURANCE_POLICY_NUMBER") && !identifierType.equals("PASSPORT")) {
                UPIDGeneratorResponse upidGeneratorResponse = generateUpiIdWithFosaId(identifier, identifierType, null);
                responseDTO = getNprResponse(upidGeneratorResponse, results);
                if (responseDTO != null) {
                    return responseDTO;
                }
            }
        }
        //OpenHim Connection Failure
        responseDTO = new ResponseDTO();
        responseDTO.setStatus(AppConstants.RESPONSE_FAILURE);
        return responseDTO;
    }

    private ResponseDTO getNprResponse(UPIDGeneratorResponse upidGeneratorResponse, List<PatientPojo> results) {
        ResponseDTO responseDTO = new ResponseDTO();
        if (upidGeneratorResponse != null && upidGeneratorResponse.status.equalsIgnoreCase("ok")) {
//                        model.addAttribute("nidaResult", "NOTFOUND");
            PatientPojo patientPojo = new PatientPojo();
            patientPojo.setSurName(upidGeneratorResponse.data.surName);
            patientPojo.setPostNames(upidGeneratorResponse.data.postNames);
            List<Identifier> ids = new ArrayList<Identifier>();
            if (upidGeneratorResponse.data.upi != null && !upidGeneratorResponse.data.upi.isEmpty())
                ids.add(new Identifier("UPI", upidGeneratorResponse.data.upi));
            if (upidGeneratorResponse.data.nid != null && !upidGeneratorResponse.data.nid.isEmpty())
                ids.add(new Identifier("NID", upidGeneratorResponse.data.nid));
            if (upidGeneratorResponse.data.nin != null && !upidGeneratorResponse.data.nin.isEmpty())
                ids.add(new Identifier("NIN", upidGeneratorResponse.data.nin));
            if (upidGeneratorResponse.data.applicationNumber != null && !upidGeneratorResponse.data.applicationNumber.isEmpty())
                ids.add(new Identifier("NID_APPLICATION_NUMBER", upidGeneratorResponse.data.applicationNumber));

            patientPojo.setIdentifiers(ids);
            if (upidGeneratorResponse.data.citizenStatus != null && upidGeneratorResponse.data.citizenStatus.equals("13"))
                patientPojo.setCitizenStatus(true);
            else patientPojo.setCitizenStatus(false);

            patientPojo.setGender(upidGeneratorResponse.data.sex);
            patientPojo.setNationality(upidGeneratorResponse.data.nationality);
            patientPojo.setMaritalStatus(upidGeneratorResponse.data.maritalStatus);
            patientPojo.setDateOfBirth(upidGeneratorResponse.data.dateOfBirth);
            patientPojo.setFatherName(upidGeneratorResponse.data.fatherName);
            patientPojo.setMotherName(upidGeneratorResponse.data.motherName);
            patientPojo.setOrigin(AppConstants.ORIGIN_NPR);
            patientPojo.setOriginRank(AppConstants.RANK_NPR_ONLY);

            Address address = new Address();

            address.setType(AppConstants.DOMICILE_ADDRESS);
            address.setPostalCode(upidGeneratorResponse.data.villageId);
            address.setCity(upidGeneratorResponse.data.domicileVillage);
            address.setCell(upidGeneratorResponse.data.domicileCell);
            address.setSector(upidGeneratorResponse.data.domicileSector);
            address.setDistrict(upidGeneratorResponse.data.domicileDistrict);
            address.setState(upidGeneratorResponse.data.domicileProvince);
            address.setCountry(upidGeneratorResponse.data.domicileCountry);
            patientPojo.setAddressList(Collections.singletonList(address));
            results.add(patientPojo);
            responseDTO.setResults(results);
            responseDTO.setStatus(results.isEmpty() ? AppConstants.RESPONSE_FAILURE : AppConstants.RESPONSE_SUCCESS);
            responseDTO.setRecordsCount(results.size());
            return responseDTO;
        } else {
            return null;
        }
    }

    private ResponseDTO getCrResponse(CrPatient crPatient, List<PatientPojo> results) {
        ResponseDTO responseDTO = new ResponseDTO();
        for (Entry entry : crPatient.getEntry()) {

            PatientPojo patientPojo = new PatientPojo();
            List<Name> names = entry.getResource().getName();
            for (Name name : names) {
                patientPojo.setSurName(name.getFamily());
                patientPojo.setPostNames(name.getGiven().get(AppConstants.POST_NAME_INDEX));
            }
            /* Reformat primary care id
                Eg: from 0001-O|123xxxxx to 123xxxxx
            * */
            /*for (Identifier id : entry.getResource().getIdentifier()) {
                if (id.getSystem().equalsIgnoreCase(PrimaryCareBusinessLogic.getPrimaryPatientIdentiferType().toString())) {
                    String[] reformatId = id.getValue().split("|");
                    id.setValue(reformatId[1]);
                }
            }*/
            patientPojo.setIdentifiers(entry.getResource().getIdentifier());

            patientPojo.setGender(entry.getResource().getGender() == null ? "" : entry.getResource().getGender().toUpperCase());

            for (Extension extension : entry.getResource().getExtension()) {
                try {
                    for(CodeValue param: extension.parseValues()) {
                        switch (param.getCode()) {
                            case AppConstants.EDUCATIONAL_LEVEL: {
                                patientPojo.setEducationalLevel(param.getValue());
                                break;
                            }
                            case AppConstants.PROFESSION: {
                                patientPojo.setProfession(param.getValue());
                                break;
                            }
                            case AppConstants.RELIGION: {
                                patientPojo.setReligion(param.getValue());
                                break;
                            }
                            case AppConstants.NATIONALITY: {
                                patientPojo.setNationality(param.getValue());
                                break;
                            }
                            case AppConstants.REGISTERED_ON: {
                                patientPojo.setRegisteredOn(param.getValue());
                                break;
                            }
                        }
                    }
                } catch  (Exception e) {
                    //e.printStackTrace();
                    debugError(e);
                }
            }

            //TODO: Code to Nationality conversion to be implemented
            patientPojo.setNationality("Rwanda");
//            patientPojo.setMaritalStatus("Test Married");
            patientPojo.setDateOfBirth(entry.getResource().getBirthDate());
            /*
            for (Telecom telecom : entry.getResource().getTelecom()) {
                patientPojo.setPhoneNumber(telecom.getValue());
            }
             */

            if (entry.getResource().getTelecom() != null && !entry.getResource().getTelecom().isEmpty()){
                patientPojo.setPhoneNumber(entry.getResource().getTelecom().get(0).getValue());
            }

            patientPojo.setOriginRank(AppConstants.RANK_CR_ONLY);
            patientPojo.setOrigin(AppConstants.ORIGIN_CR);

            // Process FHIR addresses from CR response
            List<Address> patientAddresses = new ArrayList<Address>();
            for (org.openmrs.module.rwandaprimarycare.pojos.CrResponse.Address crAddress : entry.getResource().getAddress()) {
                Address patientAddress = new Address();
                if (crAddress.getLine() != null && crAddress.getLine().size() > 0) {
                    String line = crAddress.getLine().get(0);
                    String[] lineArray = line.split("\\|");
                    if (lineArray.length > 0) patientAddress.setSector(lineArray[0]);
                    if (lineArray.length > 1) patientAddress.setCell(lineArray[1]);
                    if (lineArray.length > 2) patientAddress.setType(lineArray[2]);
                }
                patientAddress.setUse(crAddress.getUse());
                patientAddress.setText(crAddress.getText());
                patientAddress.setCity(crAddress.getCity());
                patientAddress.setDistrict(crAddress.getDistrict());
                patientAddress.setState(crAddress.getState());
                patientAddress.setPostalCode(crAddress.getPostalCode());
                patientAddress.setCountry(crAddress.getCountry());
                patientAddresses.add(patientAddress);
            }

            patientPojo.setAddressList(patientAddresses);
            patientPojo.setCitizenStatus(entry.getResource().getDeceasedBoolean());
            if (entry.getResource().getContact() != null) {
                log.info("entry.getResource().getContacts() inside contacts");
                for (Contact contact : entry.getResource().getContact()) {
                    log.info("entry.getResource().getContacts() inside contacts  : " + contact.name.family);
                    if (contact.name.family.equals(MOTHER_NAME)) {
                        if (contact.name.given != null)
                            if (!contact.name.given.isEmpty() && !contact.name.given.get(0).isEmpty())
                                patientPojo.setMotherName(contact.name.given.get(0));
                    }

                    if (contact.name.family.equals(FATHER_NAME)) {
                        if (contact.name.given != null)
                            if (!contact.name.given.isEmpty() && !contact.name.given.get(0).isEmpty())
                                patientPojo.setFatherName(contact.name.given.get(0));
                    }

                    if (contact.name.family.equals(SPOUSE_NAME)) {
                        patientPojo.setSpouse(contact.name.given == null || contact.name.given.isEmpty() ? "" : contact.name.given.get(0));
                    }
                }
            }

            if (entry.getResource().getMaritalStatus() != null) {
                if (entry.getResource().getMaritalStatus().getCoding() != null && !entry.getResource().getMaritalStatus().getCoding().isEmpty()) {
                    patientPojo.setMaritalStatus(entry.getResource().getMaritalStatus().getCoding().get(0).getDisplay());
                }
            }

            log.info(" entry.getResource().getAddress() address patientPojo----patientPojo---------" + patientPojo);
            results.add(patientPojo);
        }
        responseDTO.setResults(results);
        responseDTO.setStatus(results.isEmpty() ? AppConstants.RESPONSE_FAILURE : AppConstants.RESPONSE_SUCCESS);
        responseDTO.setRecordsCount(results.size());
        log.info("Print from results.size() " + results.size());
        if (responseDTO.getStatus().equals(AppConstants.RESPONSE_SUCCESS)) return responseDTO;
        else return null;
    }

    private ResponseDTO getLocalResponse(List<Patient> patients, List<PatientPojo> results) {

        ResponseDTO responseDTO = new ResponseDTO();
        log.info("getLocalResponse crResponse - =====3" + patients.size());
        if (patients != null) {
            for (Patient localPatient : patients) {
                PatientPojo patientPojo = new PatientPojo();

                patientPojo.setSurName(localPatient.getFamilyName());
                patientPojo.setPostNames(localPatient.getGivenName());
                //Add Local Identifiers
                List<Identifier> ids = new ArrayList<Identifier>();

                Set<PatientIdentifier> identifiers = localPatient.getIdentifiers();
                for (PatientIdentifier pi : identifiers) {


                    if (pi.getIdentifierType().getPatientIdentifierTypeId() == 3) {
                        ids.add(new Identifier("PRIMARY_CARE_ID", pi.getIdentifier()));
                    }
                    if (pi.getIdentifierType().getPatientIdentifierTypeId() == 5) {
                        ids.add(new Identifier("NID", pi.getIdentifier()));
                    }
                    if (pi.getIdentifierType().getPatientIdentifierTypeId() == 13) {
                        ids.add(new Identifier("UPI", pi.getIdentifier()));
                    }
                    if (pi.getIdentifierType().getPatientIdentifierTypeId() == 14) {
                        ids.add(new Identifier("NIN", pi.getIdentifier()));
                    }
                    if (pi.getIdentifierType().getPatientIdentifierTypeId() == 15) {
                        ids.add(new Identifier("NID_APPLICATION_NUMBER", pi.getIdentifier()));
                    }
                    if (pi.getIdentifierType().getPatientIdentifierTypeId() == 4) {
                        ids.add(new Identifier("TRACNET_NUMBER", pi.getIdentifier()));
                    }

                }

                try {
                    List<InsurancePolicy> insurances = InsurancePolicyUtil.getInsurancePoliciesByPatient(localPatient);
                    for (InsurancePolicy policy : insurances) {
                        ids.add(new Identifier("INSURANCE_POLICY_NUMBER", policy.getInsuranceCardNo()));
                    }
                    if (insurances.size() == 0) {
                        ids.add(new Identifier("INSURANCE_POLICY_NUMBER", ""));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ids.add(new Identifier("INSURANCE_POLICY_NUMBER", ""));
                }


                patientPojo.setIdentifiers(ids);
                //Add Local Address
                Set<PersonAddress> allAddresses = localPatient.getAddresses();
                List<Address> responseAddressList = new ArrayList<Address>();
                for (PersonAddress address : allAddresses) {
                    Address responseAddress = new Address();
                    responseAddress.setAddressId(String.valueOf(address.getId()));
                    responseAddress.setType(address.getAddress15());
                    responseAddress.setText(address.getAddress1());
                    responseAddress.setPostalCode(address.getPostalCode());
                    responseAddress.setCity(address.getCityVillage());
                    responseAddress.setDistrict(address.getCountyDistrict());
                    responseAddress.setState(address.getStateProvince());
                    responseAddress.setCountry(address.getCountry());
                    responseAddress.setCell(address.getAddress3());
                    responseAddress.setSector(address.getAddress2());
                    responseAddressList.add(responseAddress);
                }
                patientPojo.setAddressList(responseAddressList);

                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

                patientPojo.setOpenMrsId(String.valueOf(localPatient.getPatientId()));

                patientPojo.setGender(localPatient.getGender());
                patientPojo.setNationality("Rwanda");
                PersonAttribute personAttribute = localPatient.getAttribute(PrimaryCareConstants.GLOBAL_PROPERTY_CIVIL_STATUS_CONCEPT);
                if (personAttribute != null) patientPojo.setMaritalStatus(personAttribute.getValue());
                personAttribute = localPatient.getAttribute(PrimaryCareConstants.GLOBAL_PROPERTY_PHONE_NUMBER_CONCEPT);
                if (personAttribute != null) patientPojo.setPhoneNumber(personAttribute.getValue());
                personAttribute = localPatient.getAttribute(PrimaryCareConstants.FATHER_NAME_ATTRIBUTE_TYPE);
                if (personAttribute != null) patientPojo.setFatherName(personAttribute.getValue());
                personAttribute = localPatient.getAttribute(PrimaryCareConstants.MOTHER_NAME_ATTRIBUTE_TYPE);
                if (personAttribute != null) patientPojo.setMotherName(personAttribute.getValue());
                personAttribute = localPatient.getAttribute(PrimaryCareConstants.GLOBAL_PROPERTY_EDUCATION_LEVEL_CONCEPT);
                if (personAttribute != null) patientPojo.setEducationalLevel(personAttribute.getValue());
                personAttribute = localPatient.getAttribute(PrimaryCareConstants.GLOBAL_PROPERTY_PROFESSION_CONCEPT);
                if (personAttribute != null) patientPojo.setProfession(personAttribute.getValue());
                personAttribute = localPatient.getAttribute(PrimaryCareConstants.GLOBAL_PROPERTY_RELIGION_CONCEPT);
                if (personAttribute != null) patientPojo.setReligion(personAttribute.getValue());
                personAttribute = localPatient.getAttribute(PrimaryCareConstants.GLOBAL_PROPERTY_SPOUSE_NAME_CONCEPT);
                if (personAttribute != null) patientPojo.setSpouse(personAttribute.getValue());
                patientPojo.setDateOfBirth(formatter.format(localPatient.getBirthdate()));
                patientPojo.setCitizenStatus(localPatient.getDead());
                results.add(patientPojo);
            }
            responseDTO.setResults(results);
            responseDTO.setStatus(results.isEmpty() ? AppConstants.RESPONSE_FAILURE : AppConstants.RESPONSE_SUCCESS);
            responseDTO.setRecordsCount(results.size());
            if (responseDTO.getStatus().equals(AppConstants.RESPONSE_SUCCESS)) return responseDTO;
        }

        return null;
    }

    public ResponseDTO checkPatientLocal(String identifier, String identifierType, List<PatientPojo> results) {
        List<PatientPojo> results1 = new ArrayList<PatientPojo>();
        ResponseDTO responseDTO = null;
        List<Patient> singletonPatient = new ArrayList<Patient>();
        if (identifierType.equals("INSURANCE_POLICY_NUMBER")) {
            Patient patient = getPatientByInsurance(identifier);
            if (patient != null) singletonPatient.add(patient);
        } else {
            List<Patient> patients = PrimaryCareBusinessLogic.getService().findPatientByIdentifier(identifier);
            if (patients != null) {
                singletonPatient.addAll(patients);
            }

        }
        String traceNetID = "";
        responseDTO = getLocalResponse(singletonPatient, results);
        if (responseDTO != null) {
            results = (List<PatientPojo>) responseDTO.getResults();
            PatientPojo patientPojo = results.get(0);
            try {
                PatientIdentifier patientIdentifier = singletonPatient.get(0).getPatientIdentifier(4);
                if (patientIdentifier != null) {
                    traceNetID = patientIdentifier.getIdentifier();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            patientPojo.setOrigin(AppConstants.ORIGIN_LOCAL);
            String postalAddress = "", resistantAdd = "";
            if (patientPojo.getAddressList() != null && !patientPojo.getAddressList().isEmpty()) {
                for (Address a : patientPojo.getAddressList()) {
                    if (a.getType() != null && a.getType().equalsIgnoreCase("postal")) {
                        postalAddress = a.getAddressId();
                    }
                    if (a.getType() != null && a.getType().equalsIgnoreCase("RESIDENTIAL")) {
                        resistantAdd = a.getAddressId();
                    }


                }

            }
            CrPatient crEntry = checkPatientCR(identifier, identifierType);
            if (crEntry != null && crEntry.getEntry() != null) {
                ResponseDTO crResponse = getCrResponse(crEntry, results1);
                if (crResponse != null) {
                    String localId = ((List<PatientPojo>) responseDTO.getResults()).get(0).getOpenMrsId();
                    log.info("getLocalResponse crResponse - zzzzzzzzzz" + crResponse);
                    patientPojo = ((List<PatientPojo>) crResponse.getResults()).get(0);
                    patientPojo.setOpenMrsId(localId);

                    boolean isTracentPresent = false;
                    for(Identifier id: patientPojo.getIdentifiers()){
                        if("TRACNET_NUMBER".equals(id.getSystem())){
                            isTracentPresent = true;
                        }
                    }

                    if(!isTracentPresent && traceNetID != null && !traceNetID.isEmpty()){
                        patientPojo.getIdentifiers().add(new Identifier("TRACNET_NUMBER", traceNetID));
                    }
                }
                if (patientPojo.getAddressList() != null && !patientPojo.getAddressList().isEmpty()) {
                    for (Address a : patientPojo.getAddressList()) {
                        log.info(" entry.getResource().getAddress() address type------a.getType()-------" + a.getType());
                        if (a.getType() != null && a.getType().equalsIgnoreCase("postal")) {
                            a.setAddressId(postalAddress);
                        }
                        if (a.getType() != null && a.getType().equalsIgnoreCase("RESIDENTIAL")) {
                            a.setAddressId(resistantAdd);
                        }
                    }
                }
                patientPojo.setOriginRank(AppConstants.RANK_CR_LOCAL);
            } else {
                patientPojo.setOriginRank(AppConstants.RANK_LOCAL_ONLY);
            }
            responseDTO.setResults(Collections.singletonList(patientPojo));
            return responseDTO;
        }
        return responseDTO;

    }

    public Patient getPatientByInsurance(String insuranceNo) {
        Patient patient1 = Context.getPatientService().getPatient(46);
        //List<InsurancePolicy> insurances =  InsurancePolicyUtil.getInsurancePoliciesByPatient(patient1);
        try {


            List<InsurancePolicy> insurances = InsurancePolicyUtil.getInsurancePoliciesByPatient(patient1);

            InsurancePolicy allCards = InsurancePolicyUtil.getBeneficiaryByCardNo(null, insuranceNo);

            // InsurancePolicy insurances1 = InsurancePolicyUtil.getInsurancePolicyByCardNo(insuranceNo);
            log.info("getPatientByInsurance insurances - " + insurances.size());
            log.info("getPatientByInsurance id - " + allCards.getOwner().getId());
            log.info("getPatientByInsurance id - " + allCards.getInsuranceCardNo());
            return Context.getPatientService().getPatient(allCards.getOwner().getPatientId());
        } catch (Exception e) {
            log.error("getPatientByInsurance: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

    }

    private CrPatient checkPatientCR(String identifier, String identifierType) {
        if (!CustomUtils.isOnline())
            return null;
        if (openHimConnection.getStatus().equals(AppConstants.OPENHIM_DEFINED)) {
            if (identifier != null && !identifier.isEmpty()) {

                String UPID = "";
                //model.addAttribute("search", search);
                //model.addAttribute("results", PrimaryCareBusinessLogic.findPatientsByIdentifier(search, PrimaryCareBusinessLogic.getLocationLoggedIn(session)));
                //model.addAttribute("identifierTypes", PrimaryCareBusinessLogic.getPatientIdentifierTypesToUse());


                identifier = identifier.trim().replaceAll(" ", "");
                final String uri = openHimConnection.getOpenhimUrl() + "/clientregistry/Patient?identifier=" + identifier;
                RestTemplate restTemplate = new RestTemplate();

                String plainCreds = openHimConnection.getOpenhimClientId() + ":" + openHimConnection.getOpenhimPassword();
                byte[] plainCredsBytes = plainCreds.getBytes();
                byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
                String base64Creds = new String(base64CredsBytes);

                HttpHeaders headers = new HttpHeaders();
                headers.add("Authorization", "Basic " + base64Creds);
                HttpEntity<String> request = new HttpEntity<String>(headers);
                ResponseEntity<CrPatient> response = null;
                CrPatient result = null;
                try {
                    response = restTemplate.exchange(uri, HttpMethod.GET, request, CrPatient.class);
                    result = response.getBody();
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                    log.error("checkPatientCR#491 " + ignored.toString());
                }


                //Check CR
                if (result != null && result.getEntry() != null) {
                    return result;
                } else {
                    return null;
                }
            }
        }
        return null;
    }


    public ResponseDTO findPatientAjax(String surName, String postName, String yearOfBirth, String origin) {


        ResponseDTO responseDTO = new ResponseDTO();
        List<PatientPojo> results = new ArrayList<PatientPojo>();
        openHimConnection = getOpenHimConnection();
        if (!CustomUtils.isOnline()) {
            origin = "LOCAL";
        }
        /*TODO 1.Local Search #RFC-84
                       2.Check and add Orgin and Rank
                * */
        if (openHimConnection.getStatus().equals(AppConstants.OPENHIM_DEFINED)
                || (FhirUtils.isFhirFormat() && origin.equals("CR"))) {
            if (origin == null) origin = "";
            switch (origin) {
                case "NPR":
                    UPIDGeneratorResponseList upidGeneratorResponseList = getPatientDetailsFromNPR(surName, postName, yearOfBirth);

                    if (upidGeneratorResponseList != null && upidGeneratorResponseList.status.equalsIgnoreCase("ok")) {
//                        model.addAttribute("nidaResult", "NOTFOUND");
                        for (UPIDGeneratorResponseData upidGeneratorResponse : upidGeneratorResponseList.data) {
                            PatientPojo patientPojo = new PatientPojo();
                            patientPojo.setSurName(upidGeneratorResponse.surName);
                            patientPojo.setPostNames(upidGeneratorResponse.postNames);
                            List<Identifier> ids = new ArrayList<Identifier>();
                            if (upidGeneratorResponse.upi != null && !upidGeneratorResponse.upi.isEmpty())
                                ids.add(new Identifier("UPI", upidGeneratorResponse.upi));
                            if (upidGeneratorResponse.nid != null && !upidGeneratorResponse.nid.isEmpty())
                                ids.add(new Identifier("NID", upidGeneratorResponse.nid));
                            if (upidGeneratorResponse.nin != null && !upidGeneratorResponse.nin.isEmpty())
                                ids.add(new Identifier("NIN", upidGeneratorResponse.nin));
                            if (upidGeneratorResponse.applicationNumber != null && !upidGeneratorResponse.applicationNumber.isEmpty())
                                ids.add(new Identifier("NID_APPLICATION_NUMBER", upidGeneratorResponse.applicationNumber));

                            patientPojo.setIdentifiers(ids);
                            patientPojo.setGender(upidGeneratorResponse.sex);
                            patientPojo.setNationality(upidGeneratorResponse.nationality);
                            patientPojo.setMaritalStatus(upidGeneratorResponse.maritalStatus);
                            patientPojo.setDateOfBirth(upidGeneratorResponse.dateOfBirth);
                            patientPojo.setPhoneNumber("");

                            Address address = new Address();
                            address.setPostalCode(upidGeneratorResponse.villageId);
                            address.setCity(upidGeneratorResponse.domicileSector);
                            address.setDistrict(upidGeneratorResponse.domicileDistrict);
                            address.setState(upidGeneratorResponse.domicileProvince);
                            address.setCountry(upidGeneratorResponse.domicileCountry);
                            patientPojo.setAddressList(Collections.singletonList(address));
                            results.add(patientPojo);
                        }
                        responseDTO.setResults(results);
                        responseDTO.setStatus(results.isEmpty() ? AppConstants.RESPONSE_FAILURE : AppConstants.RESPONSE_SUCCESS);
                        responseDTO.setRecordsCount(results.size());
                        return responseDTO;
                    } else {
                        //Patient Not Found
                        responseDTO.setStatus(AppConstants.RESPONSE_FAILURE);
                        return responseDTO;
                    }
                case "CR":
                    String uri = openHimConnection.getOpenhimUrl() + "/Patient?family=" + surName + "&given=" + postName + "&birthdate=" + yearOfBirth;
                    RestTemplate restTemplate = new RestTemplate();

                    String plainCreds = openHimConnection.getOpenhimClientId() + ":" + openHimConnection.getOpenhimPassword();
                    byte[] plainCredsBytes = plainCreds.getBytes();
                    byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
                    String base64Creds = new String(base64CredsBytes);

                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Authorization", "Basic " + base64Creds);
                    HttpEntity<String> request = new HttpEntity<String>(headers);
                    ResponseEntity<CrPatient> response = null;
                    CrPatient result = null;
                    try {
                        response = restTemplate.exchange(uri, HttpMethod.GET, request, CrPatient.class);
                        result = response.getBody();

                        if (result != null && result.getEntry() != null) {
                            return getCrResponse(result, results);
                        } else {
                            responseDTO = new ResponseDTO();
                            responseDTO.setStatus(AppConstants.RESPONSE_FAILURE);
                            return responseDTO;
                        }


                    } catch (Exception ignored) {
                        ignored.printStackTrace();
                        log.error("findPatientAjax CR Exception" + ignored.toString());
                    }

                    break;
                default:
                    List<Patient> patients;
                    if (yearOfBirth != null && !yearOfBirth.isEmpty()) {
                        // Search with age filter
                        LocalDate birthdate = new LocalDate(Integer.parseInt(yearOfBirth), 1, 1);
                        LocalDate now = new LocalDate();
                        Years age = Years.yearsBetween(birthdate, now);
                        patients = PrimaryCareBusinessLogic.getService().getPatients(surName, postName, null, (float) age.getYears(), null, null, null, null, null, null, null, null, null, null, null, null, null, null);
                    } else {
                        // Search without age filter
                        patients = PrimaryCareBusinessLogic.getService().getPatients(surName, postName, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
                    }
                    responseDTO = getLocalResponse(patients, results);
                    log.info("----results ---" + patients.size());
                    if (responseDTO != null) return responseDTO;

                    log.info("----null ---");
            }
        }

        responseDTO = new ResponseDTO();
        responseDTO.setStatus(AppConstants.RESPONSE_FAILURE);
        return responseDTO;
    }

    public OpenHimConnection getOpenHimConnection() {
        OpenHimConnection openHimConnection = new OpenHimConnection();
        try {
            final String openhimPatientUrl = Context.getAdministrationService().getGlobalProperty(PrimaryCareConstants.GLOBAL_PROPERTY_OPENHIM_NIDA_API);
            if (openhimPatientUrl == null || openhimPatientUrl.isEmpty()) {
                log.info("[error]------ Openhim patient report URL is not defined on administration settings.");
                //model.addAttribute("nidaResult", "NOAPI");
                openHimConnection.setStatus(AppConstants.OPENHIM_URL_UNDEFINED);
                return openHimConnection;
            } else {
                openHimConnection.setOpenhimUrl(openhimPatientUrl);
            }

            final String openhimClientID = Context.getAdministrationService().getGlobalProperty(PrimaryCareConstants.GLOBAL_PROPERTY_OPENHIM_USER_NAME);
            if (openhimClientID == null || openhimClientID.isEmpty()) {
                log.error("[error]------ Openhim client ID is not defined on administration settings.");
                //model.addAttribute("nidaResult", "NOAPI");
                openHimConnection.setStatus(AppConstants.OPENHIM_CLIENT_ID_UNDEFINED);
                return openHimConnection;
            } else {
                openHimConnection.setOpenhimClientId(openhimClientID);
            }
            final String openhimPwd = Context.getAdministrationService().getGlobalProperty(PrimaryCareConstants.GLOBAL_PROPERTY_OPENHIM_USER_PWD);
            if (openhimPwd == null || openhimPwd.isEmpty()) {
                log.error("[error]------ Openhim client Basic Auth Password is not defined on administration settings.");
                //model.addAttribute("nidaResult", "NOAPI");
                openHimConnection.setStatus(AppConstants.OPENHIM_PASSWORD_UNDEFINED);
                return openHimConnection;
            } else {
                openHimConnection.setOpenhimPassword(openhimPwd);
            }

            openHimConnection.setStatus(AppConstants.OPENHIM_DEFINED);
        } catch (Exception e) {
            log.error(this.getClass().getName() + "#getOpenHimConnection :" + e.getMessage());
        }
        return openHimConnection;
    }

    public UPIDGeneratorResponse generateUpiIdWithFosaId(String search, String docType, PatientPojo patientPojo) {
        UPIDGeneratorResponse result = null;
        if (CustomUtils.isOnline()) {

            final String openhimPatientUrl = Context.getAdministrationService().getGlobalProperty(PrimaryCareConstants.GLOBAL_PROPERTY_OPENHIM_NIDA_API);
            String plainCreds = openHimConnection.getOpenhimClientId() + ":" + openHimConnection.getOpenhimPassword();
            byte[] plainCredsBytes = plainCreds.getBytes();
            byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
            String base64Creds = new String(base64CredsBytes);

            try {
                String fosaid = Context.getAdministrationService().getGlobalProperty(PrimaryCareConstants.GLOBAL_PROPERTY_FACILITY_ID);
                RestTemplate restTemplate1 = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.add("Content-Type", "application/json");
                headers.add("Authorization", "Basic " + base64Creds);
                UPIDGeneratorResponse upidRequest = generateUpidRequest(docType, search, fosaid, patientPojo);
                HttpEntity<String> request = new HttpEntity<String>(new Gson().toJson(upidRequest.data), headers);
                ResponseEntity<UPIDGeneratorResponse> response = restTemplate1.exchange(openhimPatientUrl + "/api/v1/citizens/getCitizen", HttpMethod.POST, request, UPIDGeneratorResponse.class);
                result = response.getBody();
                log.error("*********************************************MMMM********: " + new Gson().toJson(result));
//        JsonObject resp = (JsonObject) result.get("data");
//        String UPID = resp.get("upi").toString();
//
//        log.error("*********************************************MMMM********");
//        log.error(UPID);
//        log.error("*********************************************MMMMM********");
            } catch (Exception e) {
                e.printStackTrace();
                log.error("*********************************************UPID GENERATION FAILED********: " + e.getMessage());
                if (patientPojo != null)
                    result = UPIDService.getInstance().build(docType, search, patientPojo);
            }
        } else if (patientPojo != null) {
            result = UPIDService.getInstance().build(docType, search, patientPojo);
        }

        return result;
    }

    public UPIDGeneratorResponseList getPatientDetailsFromNPR(String surName, String postName, String yearOfBirth) {
        String facilityId = Context.getAdministrationService().getGlobalProperty(PrimaryCareConstants.GLOBAL_PROPERTY_FACILITY_ID);
        UPIDGeneratorResponseList result = null;
        final String openhimPatientUrl = Context.getAdministrationService().getGlobalProperty(PrimaryCareConstants.GLOBAL_PROPERTY_OPENHIM_NIDA_API);
        String plainCreds = openHimConnection.getOpenhimClientId() + ":" + openHimConnection.getOpenhimPassword();
        byte[] plainCredsBytes = plainCreds.getBytes();
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        String base64Creds = new String(base64CredsBytes);
        try {
            RestTemplate restTemplate1 = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            JsonObject reqBody = new JsonObject();
            reqBody.addProperty("documentType", "OTHERS");
            reqBody.addProperty("surName", "%" + surName + "%");
            reqBody.addProperty("postNames", "%" + postName + "%");
            reqBody.addProperty("yearOfBirth", yearOfBirth);
            reqBody.addProperty("fosaid", facilityId);
            headers.add("Content-Type", "application/json");
            headers.add("Authorization", "Basic " + base64Creds);
            HttpEntity<String> request = new HttpEntity<String>(reqBody.toString(), headers);
            ResponseEntity<UPIDGeneratorResponseList> response = restTemplate1.exchange(openhimPatientUrl + "/api/v1/citizens/getCitizen", HttpMethod.POST, request, UPIDGeneratorResponseList.class);
            result = response.getBody();
            log.info("getPatientDetailsFromNPR" + new Gson().toJson(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public String createPatient(PatientPojo patientPojo, HttpSession session) {

        /*TODO 1.Create Local Only
               2.Create Online
        * */

        openHimConnection = getOpenHimConnection();

        String givenName = patientPojo.getPostNames();
        String familyName = patientPojo.getSurName();
        List<Identifier> identifierPojos = patientPojo.getIdentifiers();
        String addPCIdentifier = "";
        String gender = patientPojo.getGender();
        String dateOfBirth = patientPojo.getDateOfBirth();
        Integer age = null;

        try {
            if (patientPojo.getAge() != null && !patientPojo.getAge().isEmpty()) {
                age = Integer.parseInt(patientPojo.getAge());
            }
        } catch (Exception e) {
            //ignored
        }

        Integer birthdateDay = null;
        Integer birthdateMonth = null;
        Integer birthdateYear = null;
        String mothersName = patientPojo.getMotherName();
        String fathersName = patientPojo.getFatherName();
        String spouse = patientPojo.getSpouse();
        String educationLevel = patientPojo.getEducationalLevel();
        String profession = patientPojo.getProfession();
        String religion = patientPojo.getReligion();
        String phoneNumber = patientPojo.getPhoneNumber();
        Boolean isDead = patientPojo.getCitizenStatus();
        String civilStatus = patientPojo.getMaritalStatus();


        boolean needToSaveOnCR = false;
        boolean needToSaveLocal = false;
        boolean isLocalOnly = false;

        switch (patientPojo.getOrigin()) {
            case "CR": {
                needToSaveOnCR = false;
                needToSaveLocal = true;
                isLocalOnly = false;
                break;
            }
            case "NPR": {
                needToSaveOnCR = true;
                needToSaveLocal = true;
                isLocalOnly = false;
                break;
            }
            case "LOCAL": {
                needToSaveOnCR = false;
                needToSaveLocal = true;
                isLocalOnly = true;
                break;
            }
        }

        try {
            log.info("LOCAL|CR|LocalOnly - " + needToSaveLocal + " | " + needToSaveOnCR + " | " + isLocalOnly);
            // CreateNewPatientController createNewPatientController = new CreateNewPatientController();
            // String redirect = createNewPatientController.createPatient("",addPCIdentifier,givenName,familyName,gender,age,birthdateDay,birthdateMonth,birthdateYear,country,province,district,sector,cell,address1, sourceId,mothersName,fathersName,session,needToSaveOnCR,identifierPojos,needToSaveLocal);
            String redirect = createPatient(patientPojo, addPCIdentifier, givenName, familyName, gender, age, dateOfBirth, birthdateDay, birthdateMonth, birthdateYear, patientPojo.getAddressList(), mothersName, fathersName, civilStatus, spouse, educationLevel, profession, religion, phoneNumber, session, needToSaveOnCR, identifierPojos, needToSaveLocal, isLocalOnly, isDead);
            log.info("Redirect " + redirect);
            return redirect;
        } catch (PrimaryCareException e) {
            log.error("Error: createPatient : " + e.getMessage());
            e.printStackTrace();
        }


        return "/module/rwandaprimarycare/findPatient.form";
    }

    public String createPatient(PatientPojo patientPojo, String addPCIdentifier, String givenName, String familyName, String gender, //NOTE: uses original search param, not 'Create' param
                                Integer age, String dateOfBirth, //uses original search param
                                Integer birthdateDay, Integer birthdateMonth, Integer birthdateYear, List<Address> addressList, String mothersName, String fathersName, String civilStatus, String spouse, String educationLevel, String profession, String religion, String phoneNumber, HttpSession session, boolean needToSaveOnCR, List<Identifier> identifierPojos, boolean needToSaveLocal, boolean isLocalOnly, boolean isDead) throws PrimaryCareException {
        log.info("Inside Create Patient 1");
        //LK: Need to ensure that all primary care methods only throw a PrimaryCareException
        //So that errors will be directed to a touch screen error page
        Calendar c = Calendar.getInstance();
        String addIdentifier = "";
        try {
            addIdentifier = PrimaryCareBusinessLogic.getNewPrimaryIdentifierString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Patient newPatient = new Patient();
        //TODO set id for local patient
        // for(IdentifierPojo identifierPojo : identifierPojos){
        //    PatientIdentifier pi = new PatientIdentifier(identifierPojo.getValue(), PrimaryCareBusinessLogic.getPrimaryPatientIdentiferType(), PrimaryCareWebLogic.getCurrentLocation(session));
        //    pi.setPreferred(true);
        //    newPatient.addIdentifier(new PatientIdentifier("",""));
        // }
        try {
            log.info("Inside Create Patient");
            String addNationalIdentifier = "";
            if (!hasText(givenName) || !hasText(familyName) || !hasText(gender) || age == null) {
                throw new RuntimeException("Programming error: this shouldn't happen because params are required");
            }

            newPatient.setGender(gender);

            if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
                try {
                    log.info("Inside Create Patient dateOfBirth if");

                    Date date = null;
                    try {
                        SimpleDateFormat simpleformat = new SimpleDateFormat("dd/MM/yyyy");
                        date = simpleformat.parse(dateOfBirth);
                    } catch (Exception e) {
                        log.info("Date parse error +" + e.getMessage());
                        SimpleDateFormat simpleformat = new SimpleDateFormat("yyyy-MM-dd");
                        date = simpleformat.parse(dateOfBirth);
                    }

                    c.setTime(date);
                    if (birthdateDay != null) {
                        c.set(Calendar.DAY_OF_MONTH, birthdateDay);
                    }
                    if (birthdateMonth != null) {
                        c.set(Calendar.MONTH, birthdateMonth - 1);
                    }
                    if (birthdateYear != null) {
                        c.set(Calendar.YEAR, birthdateYear);
                    }
                    newPatient.setBirthdate(c.getTime());
                    newPatient.setBirthdateEstimated(false);
                } catch (Exception e) {
                    c.add(Calendar.YEAR, -age);
                    c.add(Calendar.DATE, -183);
                    if (birthdateDay != null) {
                        c.set(Calendar.DAY_OF_MONTH, birthdateDay);
                    }
                    if (birthdateMonth != null) {
                        c.set(Calendar.MONTH, birthdateMonth - 1);
                    }
                    if (birthdateYear != null) {
                        c.set(Calendar.YEAR, birthdateYear);
                    }
                    newPatient.setBirthdate(c.getTime());
                    if (birthdateDay == null || birthdateMonth == null) {
                        newPatient.setBirthdateEstimated(true);
                    }
                    log.info("Inside Create Patient dateOfBirth if exception age");
                }

            } else {

                c.add(Calendar.YEAR, -age);
                c.add(Calendar.DATE, -183);
                if (birthdateDay != null) {
                    c.set(Calendar.DAY_OF_MONTH, birthdateDay);
                }
                if (birthdateMonth != null) {
                    c.set(Calendar.MONTH, birthdateMonth - 1);
                }
                if (birthdateYear != null) {
                    c.set(Calendar.YEAR, birthdateYear);
                }
                newPatient.setBirthdate(c.getTime());
                if (birthdateDay == null || birthdateMonth == null) {
                    newPatient.setBirthdateEstimated(true);
                }
                log.info("Inside Create Patient dateOfBirth else");
            }


            //save Education Level person attribute type
            PersonAttributeType educationLevelAttributeType = Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.GLOBAL_PROPERTY_EDUCATION_LEVEL_CONCEPT);
            PersonAttribute educationLevelAttribute = new PersonAttribute();
            educationLevelAttribute.setAttributeType(educationLevelAttributeType);
            educationLevelAttribute.setValue(educationLevel);
            newPatient.addAttribute(educationLevelAttribute);

            //save Profession person attribute type
            PersonAttributeType professionAttributeType = Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.GLOBAL_PROPERTY_PROFESSION_CONCEPT);
            PersonAttribute professionAttribute = new PersonAttribute();
            professionAttribute.setAttributeType(professionAttributeType);
            professionAttribute.setValue(profession);
            newPatient.addAttribute(professionAttribute);

            //save Religion person attribute type
            PersonAttributeType religionAttributeType = Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.GLOBAL_PROPERTY_RELIGION_CONCEPT);
            PersonAttribute religionAttribute = new PersonAttribute();
            religionAttribute.setAttributeType(religionAttributeType);
            religionAttribute.setValue(religion);
            newPatient.addAttribute(religionAttribute);

            //save Phone Number person attribute type
            PersonAttributeType phoneNumberAttributeType = Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.GLOBAL_PROPERTY_PHONE_NUMBER_CONCEPT);
            PersonAttribute phoneNumberAttribute = new PersonAttribute();
            phoneNumberAttribute.setAttributeType(phoneNumberAttributeType);
            phoneNumberAttribute.setValue(phoneNumber);
            newPatient.addAttribute(phoneNumberAttribute);

            //save Spouse Name person attribute type
            PersonAttributeType spouseNameAttributeType = Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.GLOBAL_PROPERTY_SPOUSE_NAME_CONCEPT);
            PersonAttribute spouseNameAttribute = new PersonAttribute();
            spouseNameAttribute.setAttributeType(spouseNameAttributeType);
            spouseNameAttribute.setValue(spouse);
            newPatient.addAttribute(spouseNameAttribute);

            //save Spouse Name person attribute type
            PersonAttributeType civilStatusAttributeType = Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.GLOBAL_PROPERTY_CIVIL_STATUS_CONCEPT);
            PersonAttribute civilStatusAttribute = new PersonAttribute();
            civilStatusAttribute.setAttributeType(civilStatusAttributeType);
            civilStatusAttribute.setValue(civilStatus);
            newPatient.addAttribute(civilStatusAttribute);


            PersonAttributeType nationalityAttributeType = Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.NATIONALITY_ATTRIBUTE_TYPE);
            PersonAttribute nationalityAttribute = new PersonAttribute();
            nationalityAttribute.setAttributeType(nationalityAttributeType);
            nationalityAttribute.setValue(patientPojo.getNationality());
            newPatient.addAttribute(nationalityAttribute);


            //captialize first letter of christian name
            givenName = capitalizeFirstLetterOfString(givenName);

            //capitalize after space character in name
            int pos = givenName.trim().indexOf(" ");
            if (pos > 0) {
                try {
                    if (givenName.charAt(pos + 1) != ' ') {
                        String firstPart = givenName.substring(0, pos + 1);
                        String secondPart = givenName.substring(pos + 1);
                        secondPart = capitalizeFirstLetterOfString(secondPart);
                        givenName = firstPart + secondPart;
                    }
                } catch (Exception ex) {
                    log.info("Exception ocurred: ", ex);
                }
            }

            PersonName pn = new PersonName(givenName, null, familyName);
            pn.setPreferred(true);
            newPatient.addName(pn);
            {
                for (Address address : addressList) {
                    PersonAddress pa = new PersonAddress();
                    pa.setCountry(address.getCountry());
                    pa.setStateProvince(address.getState());
                    pa.setCountyDistrict(address.getDistrict());
                    pa.setCityVillage(address.getSector());
                    //pa.setCityVillage(address.getCity());
                    pa.setAddress3(address.getCell());
                    pa.setAddress2(address.getSector());
                    //pa.setAddress1(address.getText());
                    pa.setAddress4(address.getText());
                    pa.setPostalCode(address.getPostalCode());
                    pa.setAddress15(address.getType());

                    //Here need to set village to address1 as OpenMRS requires it
                    pa.setAddress1(address.getCity());
                    if (address.getType().equalsIgnoreCase(AppConstants.RESIDENTIAL_ADDRESS)) pa.setPreferred(true);
                    newPatient.addAddress(pa);
                }
            }

            //new patients get a new ID:
            try {

                boolean newIdNeeded = true;
                //TODO:  i think this is wrong.  this could wreck sync?
                if (addPCIdentifier != null && !addPCIdentifier.equals("") && addPCIdentifier.length() > 3) {
                    //if the passed-in identifier looks like a valid id for the id type:
                    if (PrimaryCareUtil.isIdentifierStringAValidIdentifier(addPCIdentifier, PrimaryCareWebLogic.getCurrentLocation(session))) {
                        //if no patient already exists with this id.  TODO:  what if the patient already does exist??
                        if (Context.getPatientService().getPatients(null, addPCIdentifier, Collections.singletonList(PrimaryCareBusinessLogic.getPrimaryPatientIdentiferType()), true).size() == 0) {
                            //if the ID's 3 digits correspond to the current location:
                            if (addPCIdentifier.substring(0, 3).equals(PrimaryCareUtil.getPrimaryCareLocationCode())) {
                                PatientIdentifier pi = new PatientIdentifier(addPCIdentifier, PrimaryCareBusinessLogic.getPrimaryPatientIdentiferType(), PrimaryCareWebLogic.getCurrentLocation(session));
                                pi.setPreferred(true);
                                newPatient.addIdentifier(pi);
                                newIdNeeded = false;
                            } else {

                                //the location code is different than the local location code, i.e. the id is from somwhere else and is not in the db
                                //case:  we can find a location out of our location list for this id:
                                Location thisIdsLocation = PrimaryCareUtil.getPrimaryCareLocationFromCodeList(addPCIdentifier.substring(0, 3));
                                if (thisIdsLocation != null) {
                                    PatientIdentifier pi = new PatientIdentifier(addPCIdentifier, PrimaryCareBusinessLogic.getPrimaryPatientIdentiferType(), thisIdsLocation);
                                    pi.setPreferred(true);
                                    newPatient.addIdentifier(pi);
                                } else {
                                    //case:  we can't find a location out of our location list for this id:
                                    //TODO:  when id.location stops being mandatory, we can create...
                                    // for now, pass} catch (Exception e) {
                                }

                            }

                        }
                    }
                }
                if (newIdNeeded) {

                    PatientIdentifier pi = new PatientIdentifier(addIdentifier, PrimaryCareBusinessLogic.getPrimaryPatientIdentiferType(), PrimaryCareWebLogic.getCurrentLocation(session));
                    pi.setPreferred(true);
                    newPatient.addIdentifier(pi);
                    log.error("Generated ID - " + pi.getIdentifier());
                }

            } catch (Exception ex) {
                log.error("Exception ocurred: ", ex);
                throw new RuntimeException("Couldn't generate new ID.  Check idgen settings.");
            }


            //Patient is Dead logic
            if (isDead) {
                newPatient.setDead(isDead);
            }

            PersonAttributeType pat = PrimaryCareUtil.getHealthCenterAttributeType();
            //TODO:  is this right -- verify with Cheryl?
            if (pat != null) {
                PersonAttribute pa = PrimaryCareUtil.newPersonAttribute(pat, PrimaryCareWebLogic.getCurrentLocation(session).getLocationId().toString(), newPatient);
                newPatient.addAttribute(pa);
            }

            // Find UPI identifier (Java 6 compatible - no lambdas)
            Identifier upid = null;
            for (Identifier id : identifierPojos) {
                if (id.getSystem().equalsIgnoreCase("UPI")) {
                    upid = id;
                    break;
                }
            }

            boolean isUpidGenerated = true;
            UPIDGeneratorResponse upidGeneratorResponse = null;
            if (upid == null || upid.getValue() == null || upid.getValue().isEmpty()) {
                upidGeneratorResponse = generateUpiIdWithFosaId(CustomUtils.generateTempId(), "TEMPID", patientPojo);
                Identifier newUpid = new Identifier();
                newUpid.setValue(upidGeneratorResponse.data.upi);
                newUpid.setSystem("UPI");
                if (upid != null) {
                    upid.setValue(upidGeneratorResponse.data.upi);
                    upid.setSystem("UPI");
                } else {
                    identifierPojos.add(newUpid);
                }
            } else
                isUpidGenerated = false;

            for (Identifier ip : identifierPojos) {
                if (ip.getSystem().equalsIgnoreCase("NID") && ip.getValue() != null && !ip.getValue().isEmpty())
                    addNationalIdentifier = ip.getValue();

                try {

                    if (ip.getSystem().equalsIgnoreCase("NIN") && ip.getValue() != null && !ip.getValue().isEmpty()) {
                        PatientIdentifierType natIdType = new PatientIdentifierType(14);//14 is the PatientIdentifierType id
                        if (natIdType != null) {
                            newPatient.addIdentifier(new PatientIdentifier(ip.getValue(), natIdType, PrimaryCareWebLogic.getCurrentLocation(session)));
                        }
                    }
                    if (ip.getSystem().equalsIgnoreCase("UPI") && ip.getValue() != null && !ip.getValue().isEmpty()) {
                        PatientIdentifierType natIdType = new PatientIdentifierType(13);//14 is the PatientIdentifierType id
                        if (natIdType != null) {
                            newPatient.addIdentifier(new PatientIdentifier(ip.getValue(), natIdType, PrimaryCareWebLogic.getCurrentLocation(session)));
                        }
                    }
                    if (ip.getSystem().equalsIgnoreCase("NID_APPLICATION_NUMBER") && ip.getValue() != null && !ip.getValue().isEmpty()) {
                        PatientIdentifierType natIdType = new PatientIdentifierType(15);//14 is the PatientIdentifierType id
                        if (natIdType != null) {
                            newPatient.addIdentifier(new PatientIdentifier(ip.getValue(), natIdType, PrimaryCareWebLogic.getCurrentLocation(session)));
                        }
                    }
                    if (ip.getSystem().equalsIgnoreCase("PASSPORT") && ip.getValue() != null && !ip.getValue().isEmpty()) {
                        PatientIdentifierType natIdType = new PatientIdentifierType(16);//14 is the PatientIdentifierType id
                        if (natIdType != null) {
                            newPatient.addIdentifier(new PatientIdentifier(ip.getValue(), natIdType, PrimaryCareWebLogic.getCurrentLocation(session)));
                        }
                    }
                    if (ip.getSystem().equalsIgnoreCase("FOREIGNER_ID") && ip.getValue() != null && !ip.getValue().isEmpty()) {
                        PatientIdentifierType natIdType = new PatientIdentifierType(17);//14 is the PatientIdentifierType id
                        if (natIdType != null) {
                            newPatient.addIdentifier(new PatientIdentifier(ip.getValue(), natIdType, PrimaryCareWebLogic.getCurrentLocation(session)));
                        }
                    }
                    if (ip.getSystem().equalsIgnoreCase("TRACNET_NUMBER") && ip.getValue() != null && !ip.getValue().isEmpty()) {
                        PatientIdentifierType natIdType = new PatientIdentifierType(4);//4 is the PatientIdentifierType id
                        if (natIdType != null) {
                            newPatient.addIdentifier(new PatientIdentifier(ip.getValue(), natIdType, PrimaryCareWebLogic.getCurrentLocation(session)));
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }


            }

            if (addNationalIdentifier != null && !addNationalIdentifier.trim().equals("")) {
                //PatientIdentifierType natIdType = PrimaryCareUtil.getNationalIdIdentifierType();
                PatientIdentifierType natIdType = new PatientIdentifierType(5);//5 is the PatientIdentifierType id
                if (natIdType != null) {
                    newPatient.addIdentifier(new PatientIdentifier(PrimaryCareUtil.getIdNumFromNationalId(addNationalIdentifier), natIdType, PrimaryCareWebLogic.getCurrentLocation(session)));

                    if (isLocalOnly) {
                        String givenNameNI = PrimaryCareUtil.getGivenNameFromNationalId(addNationalIdentifier);
                        String familyNameNI = PrimaryCareUtil.getFamilyNameFromNationalId(addNationalIdentifier);

                        //if the name the patient gave doens't match their ID card, create a secondary person name:
                        if (!givenNameNI.equals(newPatient.getGivenName()) || familyNameNI.equals(newPatient.getFamilyName())) {
                            PersonName pnNI = new PersonName(givenNameNI, null, familyNameNI);
                            pnNI.setPreferred(false);
                            newPatient.addName(pnNI);
                        }
                    }
                }
            }

            boolean isCRFails = false;
            boolean isUPIPresent = false;

            JsonObject patientJsonObject = new PatientDataTransfer().reformatPatientToCr(newPatient, addNationalIdentifier, addIdentifier, givenName, familyName, gender, age, birthdateDay, birthdateMonth, birthdateYear, addressList, mothersName, fathersName, spouse, patientPojo.getMaritalStatus());
            List<Identifier> crIds = new ArrayList<Identifier>();

            for (Identifier identifier : identifierPojos) {
                if (!PrimaryCareBusinessLogic.getPrimaryPatientIdentiferType().getName().equals(identifier.getSystem()))
                    crIds.add(identifier);
                if ("UPI".equals(identifier.getSystem()) && identifier.getValue() != null && !identifier.getValue().isEmpty()) {
                    isUPIPresent = true;
                }
            }
            JsonArray jsonArray = new Gson().fromJson(new Gson().toJson(crIds), JsonArray.class);
            // jsonArray.addAll(patientJsonObject.get("identifier").getAsJsonArray());
            patientJsonObject.add("identifier", jsonArray);

            log.info("==>CR-CALL: LOCAL SAVE " + patientJsonObject.toString());
            if (!identifierPojos.isEmpty()) {
                // Find UPI identifier (Java 6 compatible - no lambdas)
                Identifier upiIdentifier = null;
                for (Identifier id : identifierPojos) {
                    if (id.getSystem().equalsIgnoreCase("UPI")) {
                        upiIdentifier = id;
                        break;
                    }
                }
                log.info("==>CR-CALL: LOCAL SAVE UPI GENERATING condition");
                if (upiIdentifier != null) {
                    log.info("==>CR-CALL: LOCAL SAVE UPI GENERATING" + upiIdentifier.getValue());
                    patientJsonObject.addProperty("id", upiIdentifier.getValue());
                }
            }
            final String openhimPatientUrl = Context.getAdministrationService().getGlobalProperty(PrimaryCareConstants.GLOBAL_PROPERTY_OPENHIM_NIDA_API);
            try {
                if (needToSaveOnCR && !addIdentifier.isEmpty()) {


                    try {
                        // Helper hel = new Helper();

                        // JsonObject patientJsonObject = hel.formatPatient(newPatient, addNationalIdentifier, addIdentifier, givenName, familyName, gender, age, birthdateDay, birthdateMonth, birthdateYear, country, province, district, sector, cell, address1, mothersName, fathersName);

                        if (CustomUtils.isOnline()) {

                            String plainCreds = openHimConnection.getOpenhimClientId() + ":" + openHimConnection.getOpenhimPassword();
                            byte[] plainCredsBytes = plainCreds.getBytes();
                            byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
                            String base64Creds = new String(base64CredsBytes);

                            RestTemplate restTemplate = new RestTemplate();
                            HttpHeaders headers = new HttpHeaders();
                            headers.add("Content-Type", "application/json");
                            headers.add("Authorization", "Basic " + base64Creds);
                            HttpEntity<String> request = new HttpEntity<String>(patientJsonObject.toString(), headers);
                            ResponseEntity<String> response = restTemplate.exchange(openhimPatientUrl + "/clientregistry/Patient", HttpMethod.POST, request, String.class);
                            String result = response.getBody();
                        } else {
                            isCRFails = true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        isCRFails = true;
                    }


                }
                if (needToSaveLocal) {
                    PrimaryCareUtil.setupParentNames(newPatient, mothersName, fathersName);

                    log.info("------LOCAL PATIENT GENDER" + newPatient.getGender());
                    // for(PersonName name : newPatient.getNames()){
                    //     log.error("------LOCAL PATIENT"+name. +""+ newPatient.getNames().toString());
                    // }

                    // PersonAttribute aa =  newPatient.getAttribute("Mother's name");

                    // newPatient.getAttributes()
                    // if(aa != null)
                    //     log.error("------LOCAL PATIENT"+aa.getValue() +" ===:"+ newPatient.getNames().toString());

                    Patient patient = PrimaryCareBusinessLogic.preferredIdentifierSafeSavePatient(newPatient);

                    if (isCRFails && !addIdentifier.isEmpty() && isUPIPresent) {

                        String localData = new Gson().toJson(new PayloadData(openhimPatientUrl + "/clientregistry/Patient", HttpMethod.POST.name(), patientJsonObject.toString(), "patient_sync", patient.getPatientId()));
                        OfflineTransaction offlineTransaction = new OfflineTransaction();
                        offlineTransaction.setPayload(localData);
                        offlineTransaction.setNationalIdType(identifierPojos.get(0).getSystem());
                        offlineTransaction.setNationalId(identifierPojos.get(0).getValue());
                        offlineTransaction.setType("patient_sync_in");
                        offlineTransaction.setTimestamp(new Date());
                        offlineTransaction.setIsUpdated(0);
                        offlineTransaction.setRetryCount(0);
                        offlineTransaction.setUuid(UUID.randomUUID().toString());
                        PrimaryCareBusinessLogic.getService().saveOfflineTransaction(offlineTransaction);
                        // PrimaryCareBusinessLogic.getService().saveTrans(localData,"patient_sync");
                    }
                    //store of offline sync
                    if(upidGeneratorResponse != null && upidGeneratorResponse.isOffline){
                        UPIDService.getInstance().saveForOffline(upidGeneratorResponse);
                    }
                }
            } catch (IdentifierNotUniqueException ex) {
                MessageSourceAccessor msa = new MessageSourceAccessor(Context.getMessageSourceService().getActiveMessageSource());
                session.setAttribute(WebConstants.OPENMRS_MSG_ATTR, msa.getMessage("rwandaprimarycare.idAlreadyUsed"));
                log.error("Exception ocurred: ", ex);
                return "/module/rwandaprimarycare/createNewPatient";
            } catch (Exception ex) {
                log.error("Exception ocurred: ", ex);
                throw new RuntimeException(ex.getMessage());
            }

            if (isUpidGenerated) {
                syncCall(newPatient.getPatientId());
            }

            return "redirect:/module/rwandaprimarycare/patient.form?skipPresentQuestion=false&patientId=" + newPatient.getPatientId();
        } catch (Exception e) {
            log.error("Error - " + e.getMessage());
            throw new PrimaryCareException(e);
        }
    }

    private void syncCall(Integer patientId) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> request = new HttpEntity<String>(headers);
            ResponseEntity<String> response = null;
            String result = null;
            try {
                response = restTemplate.exchange(PrimaryCareConstants.MIGRATE_SHR_ENDPOINT + patientId, HttpMethod.GET, request, String.class);
                result = response.getBody();
            } catch (Exception e) {
                log.error("Error -syncCall  " + e.getMessage());
                e.printStackTrace();
            }
            log.error("Error -syncCall  RESPONSE ===>" + result);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void debugError(final Throwable th) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        th.printStackTrace(printWriter);
        printWriter.flush();

        String stackTrace = writer.toString();

        log.error("stackTracestackTracestackTracestackTrace== - " + stackTrace);

    }

    private String capitalizeFirstLetterOfString(String givenName) {
        if (givenName != null && givenName.length() > 0) {
            String firstLetter = givenName.substring(0, 1).toUpperCase();
            String rest = "";
            if (givenName.length() > 1) {
                rest = givenName.substring(1);
            }
            givenName = firstLetter + rest;
        }
        return givenName;
    }

    public boolean updatePatientUPID(String oldIdentifier, String newId){
        try {
            List<Patient> singletonPatient = PrimaryCareBusinessLogic.getService().findPatientByIdentifier(oldIdentifier);
            if(singletonPatient.isEmpty()){
                return true;
            }
            Patient localPatient = singletonPatient.get(0);
            for (PatientIdentifier id : localPatient.getIdentifiers()) {
                if (id.getIdentifierType().getName().equals("UPID")) {
                    id.setIdentifier(newId);
                }
            }
            PrimaryCareBusinessLogic.preferredIdentifierSafeSavePatient(localPatient);
        }catch (Exception e){
            e.printStackTrace();
        }

        return true;
    }


    public String update(PatientPojo patient, String identifier, Boolean updateCrFlag, HttpSession session) {

        openHimConnection = getOpenHimConnection();

        List<Patient> singletonPatient = PrimaryCareBusinessLogic.getService().findPatientByIdentifier(identifier);
        Patient localPatient = null;
        if (singletonPatient == null) {
            if (patient.getOpenMrsId() != null && !patient.getOpenMrsId().isEmpty())
                localPatient = Context.getPatientService().getPatient(Integer.valueOf(patient.getOpenMrsId()));
        } else {
            localPatient = singletonPatient.get(0);
        }
        if (localPatient == null) {
            return "NO patient found with given ID";
        }

        boolean alreadyHaveUpi = false;
        for (PatientIdentifier id : localPatient.getIdentifiers()) {
            if (id.getIdentifierType().getName().equals("UPID") && id.getIdentifier() != null && !id.getIdentifier().isEmpty()) {
                alreadyHaveUpi = true;
            }
        }

        PersonAttribute personAttribute = null;
        // Find UPI identifier (Java 6 compatible - no lambdas)
        Identifier upid = null;
        for (Identifier id : patient.getIdentifiers()) {
            if (id.getSystem().equalsIgnoreCase("UPI")) {
                upid = id;
                break;
            }
        }
        boolean upidGenerate = true;
        UPIDGeneratorResponse upidGeneratorResponse = null;
        if (!alreadyHaveUpi && (upid == null || upid.getValue() == null || upid.getValue().isEmpty())) {
            upidGeneratorResponse = generateUpiIdWithFosaId(CustomUtils.generateTempId(), "TEMPID", patient);
            Identifier newUpid = new Identifier();
            newUpid.setValue(upidGeneratorResponse.data.upi);
            newUpid.setSystem("UPI");
            PatientIdentifierType natIdType = Context.getPatientService().getPatientIdentifierTypeByName("UPID");
            if (upid != null) {
                upid.setValue(upidGeneratorResponse.data.upi);
                upid.setSystem("UPI");
                localPatient.addIdentifier(new PatientIdentifier(upid.getValue(), natIdType, PrimaryCareWebLogic.getCurrentLocation(session)));
            } else {
                patient.getIdentifiers().add(newUpid);
                localPatient.addIdentifier(new PatientIdentifier(newUpid.getValue(), natIdType, PrimaryCareWebLogic.getCurrentLocation(session)));
            }
        } else {
            upidGenerate = false;
        }

        if (!upidGenerate && (upid == null || upid.getValue() == null || upid.getValue().isEmpty())) {
            PatientIdentifier pi = localPatient.getPatientIdentifier("UPID");
            if (upid != null) {
                upid.setValue(pi.getIdentifier());
                upid.setSystem("UPI");
            } else {
                Identifier newUpid = new Identifier();
                newUpid.setValue(pi.getIdentifier());
                newUpid.setSystem("UPI");
                patient.getIdentifiers().add(newUpid);
            }
        }

        /*for (PatientIdentifier ip : localPatient.getIdentifiers()) {
            log.error("stackTracestackTracestackTracestackTrace== 3- getIdentifier " + ip.getIdentifier());
            log.error("stackTracestackTracestackTracestackTrace== 3- getIdentifierType " + ip.getIdentifierType().getName());
        }*/

        for (Identifier ip : patient.getIdentifiers()) {

            try {
                /*if (ip.getSystem().equalsIgnoreCase("NIN") && ip.getValue() != null && !ip.getValue().isEmpty()) {
                    PatientIdentifierType natIdType = new PatientIdentifierType(14);//14 is the PatientIdentifierType id
                    if (natIdType != null) {
                        newPatient.addIdentifier(new PatientIdentifier(ip.getValue(),
                                natIdType, PrimaryCareWebLogic.getCurrentLocation(session)));
                    }
                }
                if (ip.getSystem().equalsIgnoreCase("UPI") && ip.getValue() != null && !ip.getValue().isEmpty()) {
                    PatientIdentifierType natIdType = new PatientIdentifierType(13);//14 is the PatientIdentifierType id
                    if (natIdType != null) {
                        newPatient.addIdentifier(new PatientIdentifier(ip.getValue(),
                                natIdType, PrimaryCareWebLogic.getCurrentLocation(session)));
                    }
                }
                if (ip.getSystem().equalsIgnoreCase("NID_APPLICATION_NUMBER") && ip.getValue() != null && !ip.getValue().isEmpty()) {
                    PatientIdentifierType natIdType = new PatientIdentifierType(15);//14 is the PatientIdentifierType id
                    if (natIdType != null) {
                        newPatient.addIdentifier(new PatientIdentifier(ip.getValue(),
                                natIdType, PrimaryCareWebLogic.getCurrentLocation(session)));
                    }
                }*/

                log.debug("stackTracestackTracestackTracestackTrace== 4- getSystem " + ip.getSystem());
                log.debug("stackTracestackTracestackTracestackTrace== 4- getValue " + ip.getValue());
                if (localPatient.getPatientIdentifier(16) == null) {
                    if (ip.getSystem().equalsIgnoreCase("PASSPORT") && ip.getValue() != null && !ip.getValue().isEmpty()) {
                        PatientIdentifierType natIdType = new PatientIdentifierType(16);//14 is the PatientIdentifierType id
                        if (natIdType != null) {
                            localPatient.addIdentifier(new PatientIdentifier(ip.getValue(), natIdType, PrimaryCareWebLogic.getCurrentLocation(session)));
                        }
                    }
                }
                if (localPatient.getPatientIdentifier(17) == null) {
                    if (ip.getSystem().equalsIgnoreCase("FOREIGNER_ID") && ip.getValue() != null && !ip.getValue().isEmpty()) {
                        PatientIdentifierType natIdType = new PatientIdentifierType(17);//14 is the PatientIdentifierType id
                        if (natIdType != null) {
                            localPatient.addIdentifier(new PatientIdentifier(ip.getValue(), natIdType, PrimaryCareWebLogic.getCurrentLocation(session)));
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }


        }


        if (patient.getSurName() != null && !patient.getSurName().isEmpty() && patient.getPostNames() != null && !patient.getPostNames().isEmpty()) {

            localPatient.removeName(localPatient.getPersonName());
            PersonName pn = new PersonName(patient.getPostNames(), null, patient.getSurName());
            pn.setPreferred(true);
            localPatient.addName(pn);
        }

        if (patient.getFatherName() != null && !patient.getFatherName().isEmpty()) {
            personAttribute = localPatient.getAttribute(PrimaryCareConstants.FATHER_NAME_ATTRIBUTE_TYPE);
            if (personAttribute != null) personAttribute.setValue(patient.getFatherName());
            else {
                PersonAttributeType fatherNameAttributeType = Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.FATHER_NAME_ATTRIBUTE_TYPE);
                PersonAttribute fatherNameAttribute = new PersonAttribute();
                fatherNameAttribute.setAttributeType(fatherNameAttributeType);
                fatherNameAttribute.setValue(patient.getFatherName());
                localPatient.addAttribute(fatherNameAttribute);
            }
        }
        if (patient.getMotherName() != null && !patient.getMotherName().isEmpty()) {
            personAttribute = localPatient.getAttribute(PrimaryCareConstants.MOTHER_NAME_ATTRIBUTE_TYPE);
            if (personAttribute != null) personAttribute.setValue(patient.getMotherName());
            else {
                PersonAttributeType motherNameAttributeType = Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.MOTHER_NAME_ATTRIBUTE_TYPE);
                PersonAttribute motherNameAttribute = new PersonAttribute();
                motherNameAttribute.setAttributeType(motherNameAttributeType);
                motherNameAttribute.setValue(patient.getMotherName());
                localPatient.addAttribute(motherNameAttribute);
            }
        }
        if (patient.getSpouse() != null && !patient.getSpouse().isEmpty()) {
            personAttribute = localPatient.getAttribute(PrimaryCareConstants.GLOBAL_PROPERTY_SPOUSE_NAME_CONCEPT);
            if (personAttribute != null) personAttribute.setValue(patient.getSpouse());
            else {
                PersonAttributeType spouseNameAttributeType = Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.GLOBAL_PROPERTY_SPOUSE_NAME_CONCEPT);
                PersonAttribute spouseNameAttribute = new PersonAttribute();
                spouseNameAttribute.setAttributeType(spouseNameAttributeType);
                spouseNameAttribute.setValue(patient.getSpouse());
                localPatient.addAttribute(spouseNameAttribute);
            }
        }


        localPatient.setGender(patient.getGender());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            localPatient.setBirthdate(formatter.parse(patient.getDateOfBirth()));
        } catch (ParseException e) {
            e.printStackTrace();
        }


        Set<PersonAddress> addressSet = localPatient.getAddresses();
        PersonAddress personAddress = null;
        for (Address newAddress : patient.getAddressList()) {
            if (newAddress.getType().contains(AppConstants.RESIDENTIAL_ADDRESS)) {
                personAddress = new PersonAddress();
                personAddress.setAddress1(newAddress.getText());
                personAddress.setAddress2(newAddress.getSector());
                personAddress.setAddress3(newAddress.getCell());
                personAddress.setCityVillage(newAddress.getCity());
                personAddress.setCountyDistrict(newAddress.getDistrict());
                personAddress.setStateProvince(newAddress.getState());
                personAddress.setCountry(newAddress.getCountry());
                personAddress.setPostalCode(newAddress.getPostalCode());
                personAddress.setPreferred(true);
            }
        }
        if (personAddress != null) addressSet.add(personAddress);
        localPatient.setAddresses(addressSet);

        if (patient.getMaritalStatus() != null && !patient.getMaritalStatus().isEmpty()) {
            personAttribute = localPatient.getAttribute(PrimaryCareConstants.GLOBAL_PROPERTY_CIVIL_STATUS_CONCEPT);
            if (personAttribute != null) personAttribute.setValue(patient.getMaritalStatus());
            else {
                PersonAttributeType civilStatusAttributeType = Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.GLOBAL_PROPERTY_CIVIL_STATUS_CONCEPT);
                if (civilStatusAttributeType != null) {
                    PersonAttribute civilStatusAttribute = new PersonAttribute();
                    civilStatusAttribute.setAttributeType(civilStatusAttributeType);
                    civilStatusAttribute.setValue(patient.getMaritalStatus());
                    localPatient.addAttribute(civilStatusAttribute);
                }
            }
        }


        if (patient.getEducationalLevel() != null && !patient.getEducationalLevel().isEmpty()) {
            personAttribute = localPatient.getAttribute(PrimaryCareConstants.GLOBAL_PROPERTY_EDUCATION_LEVEL_CONCEPT);
            if (personAttribute != null) personAttribute.setValue(patient.getEducationalLevel());
            else {
                PersonAttributeType educationalLevelAttributeType = Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.GLOBAL_PROPERTY_EDUCATION_LEVEL_CONCEPT);
                PersonAttribute educationalLevelAttribute = new PersonAttribute();
                educationalLevelAttribute.setAttributeType(educationalLevelAttributeType);
                educationalLevelAttribute.setValue(patient.getEducationalLevel());
                localPatient.addAttribute(educationalLevelAttribute);
            }

        }

        if (patient.getProfession() != null && !patient.getProfession().isEmpty()) {
            personAttribute = localPatient.getAttribute(PrimaryCareConstants.GLOBAL_PROPERTY_PROFESSION_CONCEPT);
            if (personAttribute != null) personAttribute.setValue(patient.getProfession());
            else {
                PersonAttributeType professionAttributeType = Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.GLOBAL_PROPERTY_PROFESSION_CONCEPT);
                PersonAttribute professionAttribute = new PersonAttribute();
                professionAttribute.setAttributeType(professionAttributeType);
                professionAttribute.setValue(patient.getProfession());
                localPatient.addAttribute(professionAttribute);
            }

        }

        if (patient.getReligion() != null && !patient.getReligion().isEmpty()) {
            personAttribute = localPatient.getAttribute(PrimaryCareConstants.GLOBAL_PROPERTY_RELIGION_CONCEPT);
            if (personAttribute != null) personAttribute.setValue(patient.getReligion());
            else {
                PersonAttributeType religionAttributeType = Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.GLOBAL_PROPERTY_RELIGION_CONCEPT);
                PersonAttribute religionAttribute = new PersonAttribute();
                religionAttribute.setAttributeType(religionAttributeType);
                religionAttribute.setValue(patient.getReligion());
                localPatient.addAttribute(religionAttribute);
            }

        }

        if (patient.getPhoneNumber() != null && !patient.getPhoneNumber().isEmpty()) {
            personAttribute = localPatient.getAttribute(PrimaryCareConstants.GLOBAL_PROPERTY_PHONE_NUMBER_CONCEPT);
            if (personAttribute != null) personAttribute.setValue(patient.getPhoneNumber());
            else {
                PersonAttributeType phoneNumberAttributeType = Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.GLOBAL_PROPERTY_PHONE_NUMBER_CONCEPT);
                PersonAttribute phoneNumberAttribute = new PersonAttribute();
                phoneNumberAttribute.setAttributeType(phoneNumberAttributeType);
                phoneNumberAttribute.setValue(patient.getPhoneNumber());
                localPatient.addAttribute(phoneNumberAttribute);
            }

        }

        if (patient.getNationality() != null && !patient.getNationality().isEmpty()) {
            personAttribute = localPatient.getAttribute(PrimaryCareConstants.NATIONALITY_ATTRIBUTE_TYPE);
            if (personAttribute != null) personAttribute.setValue(patient.getNationality());
            else {
                PersonAttributeType nationalityAttributeType = Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.NATIONALITY_ATTRIBUTE_TYPE);
                PersonAttribute nationalityAttribute = new PersonAttribute();
                nationalityAttribute.setAttributeType(nationalityAttributeType);
                nationalityAttribute.setValue(patient.getNationality());
                localPatient.addAttribute(nationalityAttribute);
            }
        }


        if (patient.getCitizenStatus()) {
            localPatient.setDead(patient.getCitizenStatus());
        }

        try {
            PrimaryCareBusinessLogic.preferredIdentifierSafeSavePatient(localPatient);
            if (updateCrFlag) updateCr(patient, localPatient);

            if (upidGenerate && !alreadyHaveUpi) {
                syncCall(localPatient.getPatientId());
            }
            if(upidGeneratorResponse != null && upidGeneratorResponse.isOffline){
                UPIDService.getInstance().saveForOffline(upidGeneratorResponse);
            }
        } catch (Exception e) {
            e.printStackTrace();
            debugError(e);
            return null;
        }

        return AppConstants.COMPLETED_STATUS;
    }

    private void updateCr(PatientPojo patient, Patient localPatient) {

        String openhimPatientUrl = openHimConnection.getOpenhimUrl();
        boolean isCrFails = false;

        String addIdentifier = "";
        String traceNetID = "";
        try {
            PatientIdentifier primaryCareIdentifier = localPatient.getPatientIdentifier(3);
            if (primaryCareIdentifier != null) {
                addIdentifier = primaryCareIdentifier.getIdentifier();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            PatientIdentifier tracnetIdentifier = localPatient.getPatientIdentifier(4);
            if (tracnetIdentifier != null) {
                traceNetID = tracnetIdentifier.getIdentifier();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String givenName = patient.getPostNames();
        String familyName = patient.getSurName();
        String gender = patient.getGender().toLowerCase(Locale.ROOT);
        Integer age = Integer.parseInt(patient.getAge());
        Integer birthdateDay = null;
        Integer birthdateMonth = null;
        Integer birthdateYear = null;
        List<Address> addressList = patient.getAddressList();
        String mothersName = patient.getMotherName();
        String fathersName = patient.getFatherName();
        String spouse = patient.getSpouse();

        boolean isUPIPresent = false;
        JsonObject patientJsonObject = new PatientDataTransfer().reformatPatientToCr(localPatient, "", addIdentifier, givenName, familyName, gender, age, birthdateDay, birthdateMonth, birthdateYear, addressList, mothersName, fathersName, spouse, patient.getMaritalStatus());
        try {
//                    Helper hel = new Helper();

//                    JsonObject patientJsonObject = hel.formatPatient(newPatient, addNationalIdentifier, addIdentifier, givenName, familyName, gender, age, birthdateDay, birthdateMonth, birthdateYear, country, province, district, sector, cell, address1, mothersName, fathersName);

            List<Identifier> crIds = new ArrayList<Identifier>();


            for (Identifier identifier : patient.getIdentifiers()) {
                if (!"PRIMARY_CARE_ID".equals(identifier.getSystem())) crIds.add(identifier);
                if ("UPI".equals(identifier.getSystem()) && identifier.getValue() != null && !identifier.getValue().isEmpty()) {
                    isUPIPresent = true;
                }
            }

            if(traceNetID != null && !traceNetID.isEmpty()){
                crIds.add(new Identifier("TRACNET_NUMBER", traceNetID));
            }


            JsonArray jsonArray = new Gson().fromJson(new Gson().toJson(crIds), JsonArray.class);
            //jsonArray.addAll(patientJsonObject.get("identifier").getAsJsonArray());
            patientJsonObject.add("identifier", jsonArray);

            log.debug("==>CR-CALL: " + patientJsonObject.toString());
            if (!patient.getIdentifiers().isEmpty()) {
                // Find UPI identifier (Java 6 compatible - no lambdas)
                Identifier upiId = null;
                for (Identifier id : patient.getIdentifiers()) {
                    if (id.getSystem().equalsIgnoreCase("UPI")) {
                        upiId = id;
                        break;
                    }
                }
                if (upiId != null) {
                    patientJsonObject.addProperty("id", upiId.getValue());
                }
            }
            if (CustomUtils.isOnline() && !addIdentifier.isEmpty()) {

                String plainCreds = openHimConnection.getOpenhimClientId() + ":" + openHimConnection.getOpenhimPassword();
                byte[] plainCredsBytes = plainCreds.getBytes();
                byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
                String base64Creds = new String(base64CredsBytes);

                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.add("Content-Type", "application/json");
                headers.add("Authorization", "Basic " + base64Creds);
                HttpEntity<String> request = new HttpEntity<String>(patientJsonObject.toString(), headers);
                ResponseEntity<String> response = restTemplate.exchange(openhimPatientUrl + "/clientregistry/Patient", HttpMethod.POST, request, String.class);
                String result = response.getBody();
            } else {
                isCrFails = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            isCrFails = true;
        } finally {
            if (isCrFails & !addIdentifier.isEmpty() && isUPIPresent) {
                log.info("=======patientJsonObject======>" + patientJsonObject);
                log.info("=========localPatient====>" + localPatient);
                String localData = new Gson().toJson(new PayloadData(openhimPatientUrl + "/clientregistry/Patient", HttpMethod.POST.name(), patientJsonObject.toString(), "patient_sync", localPatient.getPatientId()));
                OfflineTransaction offlineTransaction = new OfflineTransaction();
                offlineTransaction.setPayload(localData);
                offlineTransaction.setNationalIdType("NID");
                try {
                    PatientIdentifier nidIdentifier = localPatient.getPatientIdentifier(5);
                    if (nidIdentifier != null) {
                        offlineTransaction.setNationalId(nidIdentifier.getIdentifier());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                offlineTransaction.setType("patient_sync_up");
                offlineTransaction.setTimestamp(new Date());
                offlineTransaction.setIsUpdated(0);
                offlineTransaction.setRetryCount(0);
                offlineTransaction.setUuid(UUID.randomUUID().toString());
                PrimaryCareBusinessLogic.getService().saveOfflineTransaction(offlineTransaction);
                // PrimaryCareBusinessLogic.getService().saveTrans(localData,"patient_sync");
            }
        }
    }

    public Location getLocationById(Integer locationId) {
        openHimConnection = getOpenHimConnection();

        Context.addProxyPrivilege(PrivilegeConstants.MANAGE_LOCATIONS);
        return Context.getLocationService().getLocation(locationId);
    }

    public List<Location> getAllLocation() {
        openHimConnection = getOpenHimConnection();

        Context.addProxyPrivilege(PrivilegeConstants.MANAGE_LOCATIONS);
        return Context.getLocationService().getAllLocations(false);
    }
}

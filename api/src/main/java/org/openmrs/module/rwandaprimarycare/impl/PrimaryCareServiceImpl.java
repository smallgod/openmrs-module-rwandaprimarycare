package org.openmrs.module.rwandaprimarycare.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.rwandaprimarycare.PrimaryCareBusinessLogic;
import org.openmrs.module.rwandaprimarycare.PrimaryCareConstants;
import org.openmrs.module.rwandaprimarycare.PrimaryCareService;
import org.openmrs.module.rwandaprimarycare.db.PrimaryCareDAO;

public class PrimaryCareServiceImpl extends BaseOpenmrsService implements PrimaryCareService {

    protected final Log log = LogFactory.getLog(getClass());

    private PrimaryCareDAO dao;

    public void setDao(PrimaryCareDAO dao) {
        this.dao = dao;
    }
   
    /**
     * Returns a list of possible values for a given patient search as follows:
     * french/eng name: list of distinct patients' given names
     * Rwandan and mother's Rwandan names: list of distinct patients' family names
     * Umudugudu: list of distinct 'address1' elements
     * 
     */
    public List<String>  getPatientSearchList(String search, PatientSearchType searchType, Integer previousId) {
        
        AddressHierarchyService ahs = Context.getService(AddressHierarchyService.class);
        
    	List<String> results = null;
    	if (searchType == PatientSearchType.FANAME){
    		results = dao.getPatientGivenNamesList(search); //FANAME = given_name
    	} else if (searchType == PatientSearchType.RWNAME) {
    		results = dao.getPatientFamilyNamesList(search);  //RWNAME = family_name
    	} else if (searchType == PatientSearchType.MRWNAME && Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.MOTHER_NAME_ATTRIBUTE_TYPE) != null) {
    		results = dao.getPatientFamilyNamesList(search);  //MRWNAME = Mother's family_name
    		PersonAttributeType motherNameAttributeType = Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.MOTHER_NAME_ATTRIBUTE_TYPE);
    		results.addAll(dao.getParentsFamilyNamesList(search,motherNameAttributeType.getPersonAttributeTypeId()));
    	} else if (searchType == PatientSearchType.FATHERSRWNAME && Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.FATHER_NAME_ATTRIBUTE_TYPE) != null) {
    		results = dao.getPatientFamilyNamesList(search);  //FATHERSRWNAME = Father's family_name
    		PersonAttributeType fatherNameAttributeType = Context.getPersonService().getPersonAttributeTypeByName(PrimaryCareConstants.FATHER_NAME_ATTRIBUTE_TYPE);
    		results.addAll(dao.getParentsFamilyNamesList(search,fatherNameAttributeType.getPersonAttributeTypeId()));
    	} else if (searchType == PatientSearchType.COUNTRY){
    	    List<AddressHierarchyEntry> aList = ahs.getTopOfHierarchyList();
    	    if (aList != null && aList.size() > 0)
    	        results = new ArrayList<String>();
    	    for (AddressHierarchyEntry a : aList){
    	        if (a != null && (a.getLocationName().toLowerCase().contains(search.toLowerCase()) || search.equals("")))
    	            results.add(a.getAddressHierarchyEntryId() + "|" + a.getLocationName());   
    	    }
    	} else if (searchType == PatientSearchType.PROVINCE){
    	    results = addressHierarchyListtoStringList(ahs.getNextComponent(previousId), search);
    	} else if (searchType == PatientSearchType.DISTRICT){
            results = addressHierarchyListtoStringList(ahs.getNextComponent(previousId), search);
        } else if (searchType == PatientSearchType.SECTOR){
            results = addressHierarchyListtoStringList(ahs.getNextComponent(previousId), search);
        } else if (searchType == PatientSearchType.CELL){
            results = addressHierarchyListtoStringList(ahs.getNextComponent(previousId), search);
        } else if (searchType == PatientSearchType.UMUDUGUDU){
            results = addressHierarchyListtoStringList(ahs.getNextComponent(previousId), search);
        }
    	
    	return results;

    }
    
    
    //TODO: only include patients who's health center is the location in the session (at hibernate level)
    // include mothers'name or father's name?
    
    public List<Patient> getPatients(
    		String frenchEnglishName, 
    		String rwandanName,
    		String gender,
    		Float age, 
    		String mothersRwandanName,
    		String fathersRwandanName,
			String patientEducationLevel,
			String patientProfession,
			String patientReligion,
			String phoneNumber,
    		String country,
            String province,
            String district,
            String sector,
            String cell,
            String umudugudu,
    		PersonAttributeType healthCenterPat,
    		Location userLocation) {
    	
    	//for now pull range from hard-coded values
    	int range = PrimaryCareConstants.ageRange; //currently set to 10
    	String restrictByHealthCenter = Context.getAdministrationService().getGlobalProperty(PrimaryCareConstants.GLOBAL_PROPERTY_RESTRICT_BY_HEALTH_CENTER);
    	if (restrictByHealthCenter == null || restrictByHealthCenter.equals(""))
    	    throw new RuntimeException("Please set the value for the global property registration.restrictSearchByHealthCenter");
    	List<Patient> ret = dao.getPatients(frenchEnglishName, rwandanName, gender, age, range, umudugudu, healthCenterPat, userLocation, Boolean.valueOf(restrictByHealthCenter));
    	PrimaryCareBusinessLogic.sortResultsForUser(ret, userLocation);
    	PrimaryCareBusinessLogic.sortResultsForUmudugudu(ret, umudugudu);
    	
    	

    	//TODO:  this is pretty hefty...
    	//ACTIVATE THIS AND TEST....
//    	if ((mothersRwandanName != null && !mothersRwandanName.equals("")) || 
//    	        (fathersRwandanName != null && !fathersRwandanName.equals("")) ){
//    	    
//    	    Cohort c = new Cohort(ret);
//          RelationshipType rt = Context.getPersonService().getRelationshipType(PrimaryCareConstants.CHILD_TO_PARENT_RELATIONSHIP_TYPE_ID);
//          Map<Integer, List<Relationship>> relationshipMap = Context.getPatientSetService().getRelationships(c, rt);
//          List<Person> parentList = Context.getPersonService().getPeople(mothersRwandanName, null);
//          parentList.addAll(Context.getPersonService().getPeople(mothersRwandanName, null));
//        	List<Patient> restrictedRet = new ArrayList<Patient>();
//        	
//        	//for each patient
//        	for (Patient p : ret){
//        	    List<Relationship> patientsRelationships = relationshipMap.get(p.getPatientId());
//        	    //for each patient's parents:
//    	        //personA is the parent, personB is the child
//        	    for (Relationship r:patientsRelationships){
//        	        if (!r.getPersonA().getPersonId().equals(p.getPatientId()) && parentList.contains(r.getPersonA())){
//        	            restrictedRet.add(p);
//        	            break;
//        	        }  
//        	}
//        	return restrictedRet;
//    	}

    	return ret;
    }
    
    //TODO:   fill in cells of the hierarchy based on umudugudu??
    private List<String> addressHierarchyListtoStringList(List<AddressHierarchyEntry> ahList, String search){
            List<String> stList = new ArrayList<String>();
            for (AddressHierarchyEntry ah : ahList){
                if (ah != null && ah.getLocationName() != null && (search.equals("") || ah.getLocationName().toLowerCase().contains(search.toLowerCase()))){
                    //we need to parse out address hierarchy ID before display, and set as javascript var on page.
                    stList.add(ah.getAddressHierarchyEntryId()+ "|" + ah.getLocationName());
                }    
            }
            if (stList.size() == 0)
                return dao.getPatientAddress1List(search);
            

            return stList;
    }

    /**
     * Save offline transaction for later synchronization
     */
    public void saveOfflineTransaction(Object offlineTransaction) {
        // TODO: Implement offline transaction persistence
        // This would typically save to database for later sync
        log.warn("saveOfflineTransaction called but not yet implemented - transaction will not be saved");
    }

    /**
     * Find patients by identifier value
     */
    public List<Patient> findPatientByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return new ArrayList<Patient>();
        }
        return Context.getPatientService().getPatients(null, identifier, null, true);
    }

    /**
     * Create last Tracnet ID for a patient
     * TODO: Implement proper Tracnet ID generation logic
     */
    public String createLastTracnetId(String fosaID, int patientId, String locationId, String prefix) {
        log.info("Creating Tracnet ID for patient: " + patientId + " with fosaID: " + fosaID);

        // Generate a basic Tracnet ID format: PREFIX-FOSAID-PATIENTID
        // This is a placeholder implementation - replace with actual business logic
        if (prefix == null || prefix.isEmpty()) {
            prefix = "TN";
        }
        if (fosaID == null || fosaID.isEmpty()) {
            fosaID = "000";
        }

        String tracnetId = prefix + "-" + fosaID + "-" + String.format("%06d", patientId);
        log.info("Generated Tracnet ID: " + tracnetId);

        return tracnetId;
    }
}

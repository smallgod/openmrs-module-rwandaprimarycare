# Test Report: Rwanda Primary Care Patient Search & CRUD Endpoints

**Test Date:** [YYYY-MM-DD]
**Tester:** [Your Name]
**Module Version:** [2.1.0-SNAPSHOT]
**OpenMRS Version:** [e.g., 2.3.0]
**Environment:** [Development/Staging/Production]

---

## Executive Summary

| Metric | Result |
|--------|--------|
| Total Tests | [X] |
| Passed | [X] |
| Failed | [X] |
| Success Rate | [X%] |
| Critical Issues Found | [X] |
| Environment Issues | [X] |

**Overall Status:** [PASS / FAIL / PARTIAL]

**Recommendation:** [Ready for deployment / Needs fixes / Blocked]

---

## Environment Configuration

### System Details
- **OpenMRS URL:** [http://localhost:8080/openmrs]
- **Module Version:** [2.1.0-SNAPSHOT]
- **OpenMRS Core Version:** [e.g., 2.3.0]
- **Java Version:** [e.g., 1.8.0_312]
- **Database:** [MySQL 5.7.x / PostgreSQL]

### Module Dependencies Status
- [ ] Rwanda Primary Care Module: [Loaded / Not Loaded]
- [ ] MOH Billing Module: [Loaded / Not Loaded / Not Required]
- [ ] OpenHIM Integration: [Configured / Not Configured / Skipped]
- [ ] Client Registry Access: [Available / Not Available]

### Global Properties Configured
- [ ] `posttoopenhim.openhim.client.user`
- [ ] `posttoopenhim.openhim.client.pwd`
- [ ] `primaryCare.openhim.clientregistry.base.url`
- [ ] `primaryCare.openhim.nida.api`
- [ ] `facility.fosa.id`
- [ ] `dhis2.organizationunitcode`

---

## Test Results

### Phase 1: Environment Setup & Authentication

| Test Case | Status | Notes |
|-----------|--------|-------|
| OpenMRS server accessible | [ ] PASS / [ ] FAIL | HTTP Code: ___ |
| Authentication successful | [ ] PASS / [ ] FAIL | User: ___ |
| Module loaded | [ ] PASS / [ ] FAIL | Version: ___ |
| Session cookie created | [ ] PASS / [ ] FAIL | Cookie file: ___ |

**Issues:**
- [None / List any issues]

---

### Phase 2: Search Endpoints

#### Test 2.1: Search by Document

| Test Case | Document Type | Status | Response Time | Records Found | Origin/Rank |
|-----------|--------------|--------|---------------|---------------|-------------|
| Valid NID | NID | [ ] PASS / [ ] FAIL | ___ms | ___ | ___ |
| Valid Passport | PASSPORT | [ ] PASS / [ ] FAIL | ___ms | ___ | ___ |
| Valid PC ID | PRIMARY_CARE | [ ] PASS / [ ] FAIL | ___ms | ___ | ___ |
| Insurance Number | INSURANCE_POLICY_NUMBER | [ ] PASS / [ ] FAIL | ___ms | ___ | ___ |
| Non-existent document | NID | [ ] PASS / [ ] FAIL | ___ms | 0 | N/A |
| Invalid document type | INVALID | [ ] PASS / [ ] FAIL | ___ms | ___ | ___ |

**Sample Response (NID Search):**
```json
[Paste actual response here]
```

**Issues:**
- [None / List any issues]

#### Test 2.2: Search by Names

| Test Case | Status | Response Time | Records Found | Notes |
|-----------|--------|---------------|---------------|-------|
| Full name + year | [ ] PASS / [ ] FAIL | ___ms | ___ | |
| Partial name | [ ] PASS / [ ] FAIL | ___ms | ___ | |
| Non-existent name | [ ] PASS / [ ] FAIL | ___ms | 0 | |
| Special characters | [ ] PASS / [ ] FAIL | ___ms | ___ | |

**Sample Response:**
```json
[Paste actual response here]
```

**Issues:**
- [None / List any issues]

#### Test 2.3: Get Locations

| Test Case | Status | Response Time | Locations Found | Notes |
|-----------|--------|---------------|----------------|-------|
| Get all locations | [ ] PASS / [ ] FAIL | ___ms | ___ | |
| Get specific location | [ ] PASS / [ ] FAIL | ___ms | 1 | facility_code: ___ |

**Sample Response:**
```json
[Paste actual response here]
```

**Issues:**
- [None / List any issues]

---

### Phase 3: Create Patient

| Test Case | Status | Response Time | Patient ID | PC ID Used | Notes |
|-----------|--------|---------------|-----------|------------|-------|
| Create with full data | [ ] PASS / [ ] FAIL | ___ms | ___ | ___ | |
| Create with minimal data | [ ] PASS / [ ] FAIL | ___ms | ___ | ___ | |
| Create with invalid PC ID | [ ] PASS / [ ] FAIL | ___ms | N/A | ___ | Expected to fail |
| Create with duplicate ID | [ ] PASS / [ ] FAIL | ___ms | N/A | ___ | Expected to fail |
| Verify in search | [ ] PASS / [ ] FAIL | ___ms | ___ | ___ | |

**Generated PC IDs:**
1. _______________
2. _______________
3. _______________

**Sample Create Response:**
```json
[Paste actual response here]
```

**Issues:**
- [None / List any issues]

---

### Phase 4: Update Patient

| Test Case | Status | Response Time | Patient Updated | Notes |
|-----------|--------|---------------|----------------|-------|
| Update with PC ID | [ ] PASS / [ ] FAIL | ___ms | [ ] Yes / [ ] No | |
| Update with UPI | [ ] PASS / [ ] FAIL | ___ms | [ ] Yes / [ ] No | |
| Update with CR sync | [ ] PASS / [ ] FAIL | ___ms | [ ] Yes / [ ] No | updateCrFlag: true |
| Update non-existent | [ ] PASS / [ ] FAIL | ___ms | N/A | Expected to fail |
| Verify changes | [ ] PASS / [ ] FAIL | ___ms | [ ] Yes / [ ] No | |

**Sample Update Response:**
```json
[Paste actual response here]
```

**Issues:**
- [None / List any issues]

---

### Phase 5: Integration Tests

#### Test 5.1: Multi-Source Search

| Test Case | Status | Notes |
|-----------|--------|-------|
| Local-only patient | [ ] PASS / [ ] FAIL | Origin: LOCAL, Rank: LOCAL_ONLY |
| CR-only patient | [ ] PASS / [ ] FAIL | Origin: CR, Rank: CR_ONLY |
| CR+Local patient | [ ] PASS / [ ] FAIL | Origin: CR, Rank: CR_LOCAL |
| NPR patient | [ ] PASS / [ ] FAIL | Origin: NPR, Rank: NPR_ONLY |
| Search priority verified | [ ] PASS / [ ] FAIL | LOCAL → CR → NPR |

**Issues:**
- [None / List any issues]

#### Test 5.2: FHIR Transformation

| Test Case | Status | Notes |
|-----------|--------|-------|
| FHIR Bundle parsed | [ ] PASS / [ ] FAIL | |
| Patient names mapped | [ ] PASS / [ ] FAIL | surName, postNames |
| Identifiers mapped | [ ] PASS / [ ] FAIL | All identifier types |
| Address mapped | [ ] PASS / [ ] FAIL | Rwandan address structure |
| Contacts mapped | [ ] PASS / [ ] FAIL | Father, mother, phone |
| Extensions mapped | [ ] PASS / [ ] FAIL | Education, profession, religion |

**Issues:**
- [None / List any issues]

#### Test 5.3: Response Structure Validation

| Field | Present | Correct Type | Notes |
|-------|---------|--------------|-------|
| status | [ ] Yes / [ ] No | String | |
| recordsCount | [ ] Yes / [ ] No | Integer | |
| results | [ ] Yes / [ ] No | Array | |
| results[].surName | [ ] Yes / [ ] No | String | |
| results[].gender | [ ] Yes / [ ] No | String | |
| results[].origin | [ ] Yes / [ ] No | String | |
| results[].originRank | [ ] Yes / [ ] No | String | |
| results[].identifiers | [ ] Yes / [ ] No | Array | |
| results[].addressList | [ ] Yes / [ ] No | Array | |

**Issues:**
- [None / List any issues]

---

## Performance Metrics

| Endpoint | Min (ms) | Max (ms) | Avg (ms) | Target | Status |
|----------|---------|---------|---------|--------|--------|
| Search Local | ___ | ___ | ___ | < 500ms | [ ] PASS / [ ] FAIL |
| Search CR | ___ | ___ | ___ | < 3s | [ ] PASS / [ ] FAIL |
| Search NPR | ___ | ___ | ___ | < 5s | [ ] PASS / [ ] FAIL |
| Create Patient | ___ | ___ | ___ | < 2s | [ ] PASS / [ ] FAIL |
| Update Patient | ___ | ___ | ___ | < 2s | [ ] PASS / [ ] FAIL |
| Get Locations | ___ | ___ | ___ | < 200ms | [ ] PASS / [ ] FAIL |

---

## Issues Found

### Critical Issues (Blockers)

1. **[Issue Title]**
   - **Severity:** Critical
   - **Test Case:** [Which test]
   - **Description:** [Detailed description]
   - **Steps to Reproduce:**
     1. [Step 1]
     2. [Step 2]
   - **Expected Result:** [What should happen]
   - **Actual Result:** [What actually happened]
   - **Error Message/Stack Trace:**
     ```
     [Paste error here]
     ```
   - **Status:** [Open / Fixed / Workaround]

### Major Issues (Important but not blockers)

1. **[Issue Title]**
   - [Same format as critical]

### Minor Issues (Nice to fix)

1. **[Issue Title]**
   - [Same format as critical]

---

## Test Artifacts

### Files Generated

- Test log: `[path/to/test_results_YYYYMMDD_HHMMSS.log]`
- Test data directory: `[path/to/test_data/]`
- Request/Response files:
  - `search_nid_response.json`
  - `search_name_response.json`
  - `create_response.json`
  - `update_response.json`
  - `locations_response.json`

### Screenshots (if applicable)

1. [Description of screenshot 1]
2. [Description of screenshot 2]

---

## Observations & Notes

### Positive Findings
- [List anything that worked particularly well]
- [Performance highlights]
- [Good error handling examples]

### Areas for Improvement
- [Suggestions for enhancement]
- [Performance optimization opportunities]
- [Documentation gaps]

### Questions for Development Team
1. [Question 1]
2. [Question 2]

---

## Recommendations

### Immediate Actions Required
- [ ] [Action item 1]
- [ ] [Action item 2]

### Before Deployment
- [ ] [Checklist item 1]
- [ ] [Checklist item 2]
- [ ] Verify all critical tests pass
- [ ] Performance within acceptable limits
- [ ] No blockers remaining
- [ ] Documentation reviewed and accurate

### Future Enhancements
- [Enhancement 1]
- [Enhancement 2]

---

## Sign-Off

**Tested By:** [Name]
**Date:** [YYYY-MM-DD]
**Signature:** [___________]

**Reviewed By:** [Name]
**Date:** [YYYY-MM-DD]
**Signature:** [___________]

**Approval Status:** [ ] Approved / [ ] Rejected / [ ] Conditional

**Comments:**
[Any additional comments from reviewer]

---

## Appendix

### Test Environment Details

```bash
# OpenMRS Version
[Output of version check]

# Module List
[List of installed modules]

# System Resources
CPU: [___]
Memory: [___]
Disk: [___]
```

### Test Data Used

**Patient Records Created:**
1. PC ID: ___, Name: ___, Purpose: ___
2. PC ID: ___, Name: ___, Purpose: ___

**Search Test Data:**
- NID: ___
- Passport: ___
- Names: ___

### References
- Implementation Documentation: `docs/PATIENT_SEARCH_IMPLEMENTATION.md`
- Testing Guide: `docs/TESTING_GUIDE.md`
- Test Scripts: `docs/test_patient_endpoints.sh`
- Sample Payloads: `docs/sample_payloads/`

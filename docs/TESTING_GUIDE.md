# Testing Guide: Rwanda Primary Care Patient Search & CRUD Endpoints

## Overview

This guide provides comprehensive instructions for testing the patient search, create, and update functionality added to the Rwanda Primary Care module.

## Prerequisites

### 1. System Requirements
- OpenMRS server running (typically `http://localhost:8080/openmrs`)
- Rwanda Primary Care module deployed (version 2.1.0-SNAPSHOT or later)
- OpenHIM Client Registry configured (optional but recommended for full functionality)
- Required tools:
  - `curl`
  - `jq` (JSON processor)
  - `python3`
  - `bash`

### 2. Module Configuration

Ensure the following global properties are configured in OpenMRS:

```properties
# OpenHIM Connection
posttoopenhim.openhim.client.user = your-openhim-user
posttoopenhim.openhim.client.pwd = your-openhim-password
primaryCare.openhim.clientregistry.base.url = http://openhim:5001/clientregistry
primaryCare.openhim.nida.api = http://openhim:5001/nida

# Facility Configuration
facility.fosa.id = your-facility-id
dhis2.organizationunitcode = your-org-unit-code

# Scheduler (for background sync)
posttoopenhim.scheduler.username = scheduler
posttoopenhim.scheduler.password = scheduler-password
```

## Available Test Scripts

### 1. Comprehensive Test Suite

**File:** `docs/test_patient_endpoints.sh`

Runs all tests in sequence, covering:
- Environment setup & authentication
- All search endpoints
- Create patient
- Update patient
- Integration tests
- Response validation

**Usage:**
```bash
cd /Users/smallgod/srv/applications/mets/openmrs-module-rwandaprimarycare
./docs/test_patient_endpoints.sh
```

**Environment Variables:**
```bash
# Optional: Override defaults
export OPENMRS_URL="http://localhost:8080/openmrs"
export OPENMRS_USERNAME="your-username"
export OPENMRS_PASSWORD="your-password"

./docs/test_patient_endpoints.sh
```

**Output:**
- Console output with colored status indicators
- Test log: `docs/test_results_YYYYMMDD_HHMMSS.log`
- Test data: `docs/test_data/` directory with all request/response files

### 2. Quick Test Scripts

#### a) Search Patient
**File:** `docs/quick_test_search.sh`

```bash
# Search by NID
./docs/quick_test_search.sh 1199080003158620 NID

# Search by Passport
./docs/quick_test_search.sh PC1234567 PASSPORT

# Search by Primary Care ID
./docs/quick_test_search.sh 123456-X PRIMARY_CARE
```

#### b) Create Patient
**File:** `docs/quick_test_create.sh`

```bash
# Creates a new patient with auto-generated valid Primary Care ID
./docs/quick_test_create.sh

# Saves the PC ID to /tmp/last_created_patient_pcid.txt for later use
```

#### c) Update Patient
**File:** `docs/quick_test_update.sh`

```bash
# Update using specific PC ID
./docs/quick_test_update.sh 123456-X

# Update last created patient (from quick_test_create.sh)
./docs/quick_test_update.sh
```

### 3. Utility Scripts

Located in `/Users/smallgod/openmrs/rwanda-emr/docs/scripts/`:

#### a) Authentication
**File:** `test_emr_auth.sh`

```bash
# Authenticate with default credentials
./test_emr_auth.sh

# Authenticate with custom credentials
./test_emr_auth.sh myusername mypassword
```

Creates session cookie at `/tmp/openmrs_cookies.txt`

#### b) Generate Valid Primary Care ID
**File:** `generate_pc_id.py`

```bash
# Generate 5 random valid PC IDs
python3 generate_pc_id.py
```

Uses Luhn Mod-24 algorithm to ensure checksum validity.

#### c) Create Test Patient (Full Example)
**File:** `create_test_patient.sh`

```bash
# Creates a complete test patient with valid PC ID
./create_test_patient.sh
```

## REST API Endpoints Reference

### Base URL
```
/ws/rwandaprimarycare/findPatient
```

### 1. Search by Document

**Endpoint:** `GET /ws/rwandaprimarycare/findPatient/byDocument`

**Query Parameters:**
- `documentNumber` (required) - The document/identifier number
- `documentType` (required) - Type of document (see supported types below)
- `fosaid` (optional) - Facility/FOSA ID

**Supported Document Types:**
- `NID` - National ID
- `NIN` - National Identification Number
- `NID_APPLICATION_NUMBER` - NID Application Number
- `PASSPORT` - Passport Number
- `FOREIGNER_ID` - Foreigner ID
- `PRIMARY_CARE` - Primary Care ID
- `INSURANCE_POLICY_NUMBER` - Insurance Policy Number

**Response:**
```json
{
  "status": "SUCCESS",
  "recordsCount": 1,
  "results": [
    {
      "openMrsId": "12345",
      "surName": "Doe",
      "postNames": "John",
      "dateOfBirth": "1990-01-01",
      "gender": "M",
      "identifiers": [...],
      "addressList": [...],
      "origin": "CR",
      "originRank": "CR_LOCAL"
    }
  ]
}
```

**Origin Values:**
- `LOCAL` - Found in local OpenMRS database
- `CR` - Found in Client Registry via OpenHIM
- `NPR` - Found in National Population Registry

**Origin Rank Values:**
- `CR_LOCAL` - Found in both CR and Local (highest confidence)
- `CR_ONLY` - Found only in CR
- `NPR_ONLY` - Found only in NPR
- `LOCAL_ONLY` - Found only in Local DB

**cURL Example:**
```bash
curl -b /tmp/openmrs_cookies.txt \
  "http://localhost:8080/openmrs/ws/rwandaprimarycare/findPatient/byDocument?documentNumber=1199080003158620&documentType=NID&fosaid=facility-001"
```

### 2. Search by Names

**Endpoint:** `GET /ws/rwandaprimarycare/findPatient/byNames`

**Query Parameters:**
- `surName` (required) - Patient's surname/family name
- `postName` (optional) - Patient's given/first name(s)
- `yearOfBirth` (optional) - Year of birth (YYYY format)
- `origin` (optional) - Search origin filter: `LOCAL`, `CR`, or `NPR`

**Response:** Same structure as search by document

**cURL Example:**
```bash
curl -b /tmp/openmrs_cookies.txt \
  "http://localhost:8080/openmrs/ws/rwandaprimarycare/findPatient/byNames?surName=NIYONSHUTI&postName=Janvier&yearOfBirth=1990&origin=LOCAL"
```

### 3. Create Patient

**Endpoint:** `POST /ws/rwandaprimarycare/findPatient/create`

**Request:**
```json
{
  "surName": "Doe",
  "postNames": "John",
  "gender": "M",
  "dateOfBirth": "1990-01-01",
  "fatherName": "Doe Senior",
  "motherName": "Doe Jane",
  "phoneNumber": "+250788123456",
  "identifiers": [
    {
      "system": "NID",
      "value": "1234567890123456"
    },
    {
      "system": "PRIMARY_CARE_ID",
      "value": "123456-X"
    }
  ],
  "addressList": [
    {
      "type": "RESIDENTIAL",
      "country": "Rwanda",
      "state": "Kigali",
      "district": "Gasabo",
      "sector": "Remera",
      "cell": "Rukiri",
      "city": "Kigali"
    }
  ],
  "maritalStatus": "Married",
  "nationality": "Rwandan",
  "educationalLevel": "University",
  "profession": "Engineer",
  "religion": "Christian"
}
```

**Response:**
```json
{
  "status": "SUCCESS",
  "message": "Patient created successfully",
  "patientId": "12345"
}
```

**cURL Example:**
```bash
curl -b /tmp/openmrs_cookies.txt \
  -H "Content-Type: application/json" \
  -X POST \
  -d @create_patient_payload.json \
  "http://localhost:8080/openmrs/ws/rwandaprimarycare/findPatient/create"
```

### 4. Update Patient

**Endpoint:** `PUT /ws/rwandaprimarycare/findPatient/update`

**Request:**
```json
{
  "surName": "Doe",
  "postNames": "John Updated",
  "gender": "M",
  "dateOfBirth": "1990-01-01",
  "phoneNumber": "+250788999999",
  "identifiers": [
    {
      "system": "PRIMARY_CARE_ID",
      "value": "123456-X"
    }
  ],
  "addressList": [...],
  "updateCrFlag": true
}
```

**Response:**
```json
{
  "status": "COMPLETED",
  "message": "Patient updated successfully"
}
```

**cURL Example:**
```bash
curl -b /tmp/openmrs_cookies.txt \
  -H "Content-Type: application/json" \
  -X PUT \
  -d @update_patient_payload.json \
  "http://localhost:8080/openmrs/ws/rwandaprimarycare/findPatient/update"
```

### 5. Get Locations

**Endpoint:** `GET /ws/rwandaprimarycare/findPatient/location`

**Response:**
```json
{
  "status": "SUCCESS",
  "results": [
    {
      "locationId": 1,
      "name": "Kigali Health Center",
      "description": "Main facility in Kigali",
      "uuid": "abc-123-def-456"
    }
  ]
}
```

**cURL Example:**
```bash
curl -b /tmp/openmrs_cookies.txt \
  "http://localhost:8080/openmrs/ws/rwandaprimarycare/findPatient/location"
```

### 6. Generate Tracnet ID

**Endpoint:** `POST /ws/rwandaprimarycare/findPatient/generateTracnet`

**Request:**
```json
{
  "patientId": "12345"
}
```

**Response:**
```json
{
  "status": "SUCCESS",
  "tracnetId": "TRAC-2024-001234"
}
```

**cURL Example:**
```bash
curl -b /tmp/openmrs_cookies.txt \
  -H "Content-Type: application/json" \
  -X POST \
  -d '{"patientId": "12345"}' \
  "http://localhost:8080/openmrs/ws/rwandaprimarycare/findPatient/generateTracnet"
```

## Test Scenarios

### Scenario 1: Basic Search Flow

1. Authenticate:
   ```bash
   ./test_emr_auth.sh
   ```

2. Search for existing patient by NID:
   ```bash
   ./quick_test_search.sh 1199080003158620 NID
   ```

3. Verify response contains:
   - `status: "SUCCESS"`
   - `recordsCount > 0`
   - Patient details with correct fields

### Scenario 2: Create and Verify Patient

1. Authenticate:
   ```bash
   ./test_emr_auth.sh
   ```

2. Create patient:
   ```bash
   ./quick_test_create.sh
   ```

3. Note the generated Primary Care ID

4. Search for created patient:
   ```bash
   ./quick_test_search.sh [PC_ID] PRIMARY_CARE
   ```

5. Verify patient appears in search results

### Scenario 3: Update Patient

1. Create a patient (or use existing PC ID)
2. Update patient:
   ```bash
   ./quick_test_update.sh [PC_ID]
   ```

3. Verify update succeeded:
   ```bash
   ./quick_test_search.sh [PC_ID] PRIMARY_CARE
   ```

4. Check updated fields in response

### Scenario 4: Multi-Source Search

1. Search for patient that exists locally:
   - Expect: `origin: "LOCAL"`, `originRank: "LOCAL_ONLY"`

2. Search for patient in Client Registry:
   - Expect: `origin: "CR"`, `originRank: "CR_ONLY"` or `"CR_LOCAL"`

3. Search for non-existent patient:
   - Expect: `status: "FAILURE"` or `recordsCount: 0`

### Scenario 5: Offline Mode

1. Disable OpenHIM connection (set global property to invalid URL)

2. Search locally:
   - Should still work for local patients

3. Create patient:
   - Should create locally (may not sync to CR)

4. Re-enable OpenHIM and verify sync

## Troubleshooting

### Issue: "OpenMRS server is not accessible"

**Cause:** OpenMRS server is not running

**Solution:**
```bash
# Check if server is running
ps aux | grep openmrs

# Start OpenMRS server (method depends on your setup)
# Example for SDK:
cd ~/openmrs/rwanda-emr
mvn openmrs-sdk:run
```

### Issue: "Authentication failed"

**Cause:** Invalid credentials or session expired

**Solution:**
```bash
# Re-authenticate with correct credentials
./test_emr_auth.sh your-username your-password
```

### Issue: "Module not found"

**Cause:** Rwanda Primary Care module not deployed

**Solution:**
```bash
# Build and deploy module
cd /Users/smallgod/srv/applications/mets/openmrs-module-rwandaprimarycare
mvn clean install

# Copy OMOD to OpenMRS modules directory
# (Location depends on your setup)
```

### Issue: "OpenHIM Client Registry not configured"

**Cause:** Missing global properties

**Solution:**
1. Log in to OpenMRS Admin
2. Go to: Administration → Maintenance → Settings
3. Set required properties (see Prerequisites section)

### Issue: "Patient not found in search after creation"

**Cause:** Database commit delay or indexing lag

**Solution:**
- Wait a few seconds and try again
- Check OpenMRS logs for errors:
  ```bash
  tail -f ~/openmrs/rwanda-emr/openmrs.log
  ```

### Issue: "Invalid Primary Care ID"

**Cause:** Incorrect checksum

**Solution:**
- Always use the `generate_pc_id.py` script to generate valid IDs
- Don't manually create PC IDs without computing the correct check digit

## Test Results Interpretation

### Success Indicators

✅ **Search Working:**
- `status: "SUCCESS"` or `"FAILURE"` (not error)
- Response has proper structure
- `recordsCount` matches actual results length

✅ **Create Working:**
- Response contains patient ID
- Patient appears in subsequent searches
- No error messages in logs

✅ **Update Working:**
- `status: "COMPLETED"` or `"SUCCESS"`
- Changes reflected in search results
- If `updateCrFlag: true`, changes synced to CR

### Failure Indicators

❌ **Endpoint Errors:**
- HTTP 404: Module not loaded or endpoint path incorrect
- HTTP 500: Server error (check logs)
- HTTP 401/403: Authentication issue

❌ **Response Errors:**
- Missing required fields
- Incorrect data types
- `status: "ERROR"` with error message

## Performance Benchmarks

Expected response times (approximate):

| Endpoint | Expected Time | Notes |
|----------|--------------|-------|
| Search Local | < 500ms | Direct database query |
| Search CR | 1-3s | Network call to OpenHIM |
| Search NPR | 2-5s | External service call |
| Create Patient | 500ms - 2s | Depends on sync settings |
| Update Patient | 500ms - 2s | Depends on sync settings |
| Get Locations | < 200ms | Cached data |

## Continuous Integration

To run tests in CI/CD pipeline:

```bash
#!/bin/bash
# ci_test.sh

# Start OpenMRS (or wait for it to be ready)
while ! curl -s http://localhost:8080/openmrs/ > /dev/null; do
    echo "Waiting for OpenMRS..."
    sleep 5
done

# Run tests
cd /Users/smallgod/srv/applications/mets/openmrs-module-rwandaprimarycare
./docs/test_patient_endpoints.sh

# Check exit code
if [ $? -eq 0 ]; then
    echo "✅ All tests passed"
    exit 0
else
    echo "❌ Tests failed"
    exit 1
fi
```

## Additional Resources

- **Implementation Documentation:** `docs/PATIENT_SEARCH_IMPLEMENTATION.md`
- **OpenHIM Proxy Documentation:** `docs/OPENHIM_CLIENT_REGISTRY_PROXY.md`
- **Module Source:** `omod/src/main/java/org/openmrs/module/rwandaprimarycare/FindPatient.java`
- **Service Logic:** `omod/src/main/java/org/openmrs/module/rwandaprimarycare/service/FindPatientService.java`

## Support

For issues or questions:
1. Check this guide and the troubleshooting section
2. Review OpenMRS logs for error details
3. Verify all prerequisites are met
4. Contact the development team with:
   - Test script output
   - OpenMRS log excerpts
   - Environment details (OpenMRS version, module version, etc.)

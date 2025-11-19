# Testing Resources Summary

## Overview

Comprehensive testing framework created for Rwanda Primary Care Patient Search & CRUD functionality.

## What Was Created

### 1. Test Scripts (Executable)

#### Comprehensive Test Suite
**Location:** `docs/test_patient_endpoints.sh`
- **Lines:** ~550+
- **Features:**
  - Automated authentication
  - Tests all 6 endpoints
  - Response validation
  - Integration testing
  - Performance tracking
  - Detailed logging
  - Colored console output
- **Usage:** `./docs/test_patient_endpoints.sh`
- **Output:**
  - Console with real-time results
  - Log file: `docs/test_results_YYYYMMDD_HHMMSS.log`
  - Test data: `docs/test_data/` directory

#### Quick Test Scripts
**Location:** `docs/quick_test_*.sh`

1. **quick_test_search.sh** - Fast search testing
   ```bash
   ./docs/quick_test_search.sh [document_number] [document_type]
   ```

2. **quick_test_create.sh** - Create patient with auto-generated PC ID
   ```bash
   ./docs/quick_test_create.sh
   ```

3. **quick_test_update.sh** - Update existing patient
   ```bash
   ./docs/quick_test_update.sh [pc_id]
   ```

### 2. Sample Payloads

**Location:** `docs/sample_payloads/`

| File | Purpose | Endpoint |
|------|---------|----------|
| `search_by_nid.json` | Search by National ID | POST byDocument |
| `search_by_passport.json` | Search by Passport | POST byDocument |
| `search_by_primary_care_id.json` | Search by PC ID | POST byDocument |
| `search_by_insurance.json` | Search by Insurance | POST byDocument |
| `search_by_name.json` | Search by name | POST byNames |
| `create_patient_full.json` | Create with all fields | POST create |
| `create_patient_minimal.json` | Create minimal | POST create |
| `update_patient.json` | Update patient | PUT update |
| `generate_tracnet.json` | Generate Tracnet ID | POST generateTracnet |

All payloads are ready-to-use with curl or Postman.

### 3. Documentation

#### Testing Guide (Comprehensive)
**Location:** `docs/TESTING_GUIDE.md`
- **Pages:** ~15+
- **Sections:**
  - Prerequisites & setup
  - Test scripts usage
  - REST API reference
  - Test scenarios
  - Troubleshooting
  - Performance benchmarks
  - CI/CD integration

#### Test Report Template
**Location:** `docs/TEST_REPORT_TEMPLATE.md`
- **Pages:** ~8+
- **Sections:**
  - Executive summary
  - Environment configuration
  - Detailed test results per phase
  - Performance metrics
  - Issues tracking
  - Sign-off section

#### Sample Payloads README
**Location:** `docs/sample_payloads/README.md`
- Usage instructions
- Field descriptions
- Valid value examples
- Testing workflow

### 4. Utility Scripts (From Rwanda EMR)

Referenced external scripts:
- `/Users/smallgod/openmrs/rwanda-emr/docs/scripts/test_emr_auth.sh`
- `/Users/smallgod/openmrs/rwanda-emr/docs/scripts/generate_pc_id.py`
- `/Users/smallgod/openmrs/rwanda-emr/docs/scripts/create_test_patient.sh`

## Quick Start Guide

### Option 1: Run Full Test Suite (Recommended)

```bash
cd /Users/smallgod/srv/applications/mets/openmrs-module-rwandaprimarycare

# Make sure OpenMRS is running, then:
./docs/test_patient_endpoints.sh

# Review results
cat docs/test_results_*.log
ls docs/test_data/
```

### Option 2: Quick Individual Tests

```bash
# 1. Authenticate
bash /Users/smallgod/openmrs/rwanda-emr/docs/scripts/test_emr_auth.sh

# 2. Test search
cd /Users/smallgod/srv/applications/mets/openmrs-module-rwandaprimarycare
./docs/quick_test_search.sh 1199080003158620 NID

# 3. Create patient
./docs/quick_test_create.sh

# 4. Update patient (uses last created)
./docs/quick_test_update.sh
```

### Option 3: Manual Testing with curl

```bash
# 1. Authenticate
bash /Users/smallgod/openmrs/rwanda-emr/docs/scripts/test_emr_auth.sh

# 2. Use sample payload
curl -b /tmp/openmrs_cookies.txt \
  -H "Content-Type: application/json" \
  -X POST \
  -d @docs/sample_payloads/search_by_nid.json \
  "http://localhost:8080/openmrs/module/rwandaprimarycare/findPatient/byDocument"
```

## Current Status

### OpenMRS Server Status
⚠️ **Server Not Running** (as of last check)

To start OpenMRS:
```bash
# Check your OpenMRS installation method and use appropriate command
# Example for SDK:
cd ~/openmrs/rwanda-emr
mvn openmrs-sdk:run

# Or if using standalone:
# Start Tomcat with OpenMRS
```

### What Can Be Done Now (Without Server)

1. ✅ Review all documentation
2. ✅ Examine test scripts
3. ✅ Study sample payloads
4. ✅ Prepare test data
5. ✅ Read implementation details

### What Requires Running Server

1. ⏸️ Execute test scripts
2. ⏸️ Call REST endpoints
3. ⏸️ Verify responses
4. ⏸️ Performance testing
5. ⏸️ Integration testing

## File Structure

```
openmrs-module-rwandaprimarycare/
├── docs/
│   ├── TESTING_GUIDE.md                 (Main testing documentation)
│   ├── TESTING_SUMMARY.md               (This file)
│   ├── TEST_REPORT_TEMPLATE.md          (Template for test reports)
│   ├── PATIENT_SEARCH_IMPLEMENTATION.md (Implementation docs)
│   │
│   ├── test_patient_endpoints.sh        (Comprehensive test suite)
│   ├── quick_test_search.sh             (Quick search test)
│   ├── quick_test_create.sh             (Quick create test)
│   ├── quick_test_update.sh             (Quick update test)
│   │
│   └── sample_payloads/
│       ├── README.md
│       ├── search_by_nid.json
│       ├── search_by_passport.json
│       ├── search_by_primary_care_id.json
│       ├── search_by_insurance.json
│       ├── search_by_name.json
│       ├── create_patient_full.json
│       ├── create_patient_minimal.json
│       ├── update_patient.json
│       └── generate_tracnet.json
│
└── [Test outputs created at runtime]
    ├── test_results_YYYYMMDD_HHMMSS.log
    └── test_data/
        ├── *.json (request/response files)
        └── *.txt (metadata files)
```

## Endpoints Covered

✅ All 6 endpoints implemented:

1. **Search by Document** - `POST /module/rwandaprimarycare/findPatient/byDocument`
2. **Search by Names** - `POST /module/rwandaprimarycare/findPatient/byNames`
3. **Create Patient** - `POST /module/rwandaprimarycare/findPatient/create`
4. **Update Patient** - `PUT /module/rwandaprimarycare/findPatient/update`
5. **Get Locations** - `GET /module/rwandaprimarycare/findPatient/location`
6. **Generate Tracnet** - `POST /module/rwandaprimarycare/findPatient/generateTracnet`

## Testing Capabilities

### Test Coverage

- ✅ Basic functionality (CRUD operations)
- ✅ Multi-source search (LOCAL → CR → NPR)
- ✅ FHIR transformation validation
- ✅ Response structure validation
- ✅ Error handling
- ✅ Performance measurement
- ✅ Integration testing
- ✅ Edge cases (invalid data, missing fields, etc.)

### Test Types

- **Unit-level:** Individual endpoint testing
- **Integration:** Multi-source search flow
- **End-to-end:** Create → Search → Update → Verify
- **Performance:** Response time tracking
- **Validation:** Response structure and data types

## Key Features

### 1. Automated Testing
- Single command runs all tests
- Self-contained (handles authentication)
- Detailed logging
- Color-coded output
- Pass/fail tracking

### 2. Flexibility
- Full suite or individual tests
- Configurable via environment variables
- Works with any OpenMRS instance
- Supports custom credentials

### 3. Data Generation
- Auto-generates valid Primary Care IDs
- Creates realistic test data
- Prevents ID collisions

### 4. Debugging Support
- Saves all requests/responses
- Detailed error messages
- Step-by-step logging
- Easy to reproduce issues

## Next Steps

### Immediate (Once Server is Running)

1. **Start OpenMRS server**
2. **Run comprehensive test suite:**
   ```bash
   ./docs/test_patient_endpoints.sh
   ```
3. **Review results** in log file and console
4. **Fix any failing tests**
5. **Document results** using TEST_REPORT_TEMPLATE.md

### Short Term

1. **Execute all test scenarios** from TESTING_GUIDE.md
2. **Test with real data** (if available)
3. **Performance testing** with larger datasets
4. **Verify OpenHIM integration** (if configured)
5. **Test offline mode** functionality

### Long Term

1. **Integrate into CI/CD** pipeline
2. **Add automated regression tests**
3. **Create Postman collection** (import sample payloads)
4. **Document known limitations**
5. **Create training materials** for testers

## Troubleshooting

### Common Issues

1. **"OpenMRS server not running"**
   - Start OpenMRS server first
   - Verify port 8080 is available

2. **"Authentication failed"**
   - Check username/password in scripts
   - Re-run test_emr_auth.sh

3. **"Module not found"**
   - Build and deploy module: `mvn clean install`
   - Verify module is loaded in OpenMRS

4. **"Invalid PC ID"**
   - Always use generate_pc_id.py script
   - Don't manually create PC IDs

### Getting Help

1. Check TESTING_GUIDE.md troubleshooting section
2. Review OpenMRS logs: `tail -f ~/openmrs/rwanda-emr/openmrs.log`
3. Verify global properties are set correctly
4. Check implementation docs: PATIENT_SEARCH_IMPLEMENTATION.md

## Performance Expectations

| Endpoint | Target | Acceptable | Slow |
|----------|--------|------------|------|
| Search Local | < 500ms | < 1s | > 1s |
| Search CR | < 3s | < 5s | > 5s |
| Search NPR | < 5s | < 8s | > 8s |
| Create | < 2s | < 3s | > 3s |
| Update | < 2s | < 3s | > 3s |
| Locations | < 200ms | < 500ms | > 500ms |

## Success Criteria

### For Endpoints
- ✅ Returns proper status codes
- ✅ Response structure matches spec
- ✅ All required fields present
- ✅ Data types correct
- ✅ Performance within targets

### For Search
- ✅ Finds patients in all sources
- ✅ Correct origin/rank returned
- ✅ Multi-source priority respected
- ✅ FHIR transformation accurate

### For Create/Update
- ✅ Patient saved successfully
- ✅ Findable in subsequent searches
- ✅ All data persisted correctly
- ✅ Client Registry sync works (if configured)

## Statistics

### Testing Resources Created

| Type | Count | Total Lines |
|------|-------|-------------|
| Test Scripts | 4 | ~800 |
| Documentation | 4 | ~1200 |
| Sample Payloads | 9 | ~300 |
| **Total** | **17** | **~2300** |

### Time Estimates

- **Initial test run:** 5-10 minutes
- **Full test suite:** 15-20 minutes
- **Individual quick test:** < 1 minute
- **Complete test cycle (with reporting):** 30-45 minutes

## Resources Referenced

### Internal
- `docs/PATIENT_SEARCH_IMPLEMENTATION.md` - Implementation details
- `docs/OPENHIM_CLIENT_REGISTRY_PROXY.md` - OpenHIM proxy docs
- `omod/src/main/java/org/openmrs/module/rwandaprimarycare/FindPatient.java` - Controller
- `omod/src/main/java/org/openmrs/module/rwandaprimarycare/service/FindPatientService.java` - Service

### External
- `/Users/smallgod/openmrs/rwanda-emr/docs/scripts/` - Utility scripts
- OpenMRS REST API documentation
- FHIR specification (for transformation validation)

## Conclusion

**Status:** ✅ Complete and ready for testing

All testing resources have been created and are ready to use. Once the OpenMRS server is running, the comprehensive test suite can be executed immediately.

The testing framework provides:
- Automated testing capability
- Manual testing resources
- Comprehensive documentation
- Troubleshooting guidance
- Performance benchmarks
- Reporting templates

**Next Action:** Start OpenMRS server and run `./docs/test_patient_endpoints.sh`

---

**Created:** November 19, 2025
**Author:** Testing Framework Generator
**Version:** 1.0

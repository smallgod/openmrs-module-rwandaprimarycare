#!/bin/bash
# Comprehensive Test Script for Rwanda Primary Care Patient Search & CRUD Endpoints
#
# Prerequisites:
# 1. OpenMRS server running on localhost:8080
# 2. Rwanda Primary Care module deployed
# 3. OpenHIM Client Registry configured (for full functionality)
#
# Usage: ./test_patient_endpoints.sh

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
OPENMRS_URL="${OPENMRS_URL:-http://localhost:8080/openmrs}"
USERNAME="${OPENMRS_USERNAME:-amugume}"
PASSWORD="${OPENMRS_PASSWORD:-Amugume@123!}"
COOKIE_FILE="/tmp/openmrs_cookies.txt"
TEST_LOG="./docs/test_results_$(date +%Y%m%d_%H%M%S).log"
TEST_DATA_DIR="./docs/test_data"

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

# Create test data directory
mkdir -p "$TEST_DATA_DIR"

# Logging function
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$TEST_LOG"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}" | tee -a "$TEST_LOG"
    ((TESTS_PASSED++))
}

log_error() {
    echo -e "${RED}❌ $1${NC}" | tee -a "$TEST_LOG"
    ((TESTS_FAILED++))
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}" | tee -a "$TEST_LOG"
}

# Test result tracking
test_result() {
    ((TESTS_TOTAL++))
    local test_name="$1"
    local expected="$2"
    local actual="$3"

    if [ "$actual" == "$expected" ]; then
        log_success "Test Passed: $test_name"
        return 0
    else
        log_error "Test Failed: $test_name (Expected: $expected, Got: $actual)"
        return 1
    fi
}

# ============================================================================
# Phase 1: Environment Setup & Authentication
# ============================================================================

echo ""
log "=========================================="
log "Phase 1: Environment Setup & Authentication"
log "=========================================="

# Check if OpenMRS is running
log "Checking if OpenMRS server is running..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$OPENMRS_URL/" || echo "000")

if [ "$HTTP_CODE" == "000" ] || [ "$HTTP_CODE" == "502" ] || [ "$HTTP_CODE" == "503" ]; then
    log_error "OpenMRS server is not accessible at $OPENMRS_URL"
    echo ""
    log "Please start the OpenMRS server and try again."
    log "Expected URL: $OPENMRS_URL"
    exit 1
fi

log_success "OpenMRS server is running (HTTP $HTTP_CODE)"

# Authenticate
log "Authenticating as $USERNAME..."
AUTH_HEADER=$(echo -n "${USERNAME}:${PASSWORD}" | base64)

AUTH_RESPONSE=$(curl -s -H "Authorization: Basic ${AUTH_HEADER}" \
    "$OPENMRS_URL/ws/rest/v1/session" \
    -c "$COOKIE_FILE")

AUTHENTICATED=$(echo "$AUTH_RESPONSE" | jq -r '.authenticated // false' 2>/dev/null || echo "false")

if [ "$AUTHENTICATED" == "true" ]; then
    USER_DISPLAY=$(echo "$AUTH_RESPONSE" | jq -r '.user.display // "Unknown"')
    log_success "Authentication successful (User: $USER_DISPLAY)"
else
    log_error "Authentication failed"
    echo "Response: $AUTH_RESPONSE"
    exit 1
fi

# Check module is loaded
log "Verifying Rwanda Primary Care module is loaded..."
MODULE_CHECK=$(curl -s -b "$COOKIE_FILE" \
    "$OPENMRS_URL/ws/rest/v1/module/rwandaprimarycare" 2>/dev/null || echo "")

if [ -n "$MODULE_CHECK" ]; then
    MODULE_VERSION=$(echo "$MODULE_CHECK" | jq -r '.version // "Unknown"' 2>/dev/null || echo "Unknown")
    log_success "Rwanda Primary Care module loaded (Version: $MODULE_VERSION)"
else
    log_warning "Could not verify module status (may not be available via REST API)"
fi

# ============================================================================
# Phase 2: Search Endpoints Testing
# ============================================================================

echo ""
log "=========================================="
log "Phase 2: Search Endpoints Testing"
log "=========================================="

# ----------------------------------------------------------------------------
# Test 2.1: Search by Document (NID)
# ----------------------------------------------------------------------------

log ""
log "Test 2.1: Search by Document"

# Test Case 1: Search with valid NID (now GET with query parameters)
log "  Test Case 2.1.1: Search by valid NID (GET)"

SEARCH_NID_RESPONSE=$(curl -s -b "$COOKIE_FILE" \
    "$OPENMRS_URL/ws/rwandaprimarycare/findPatient/byDocument?documentNumber=1199080003158620&documentType=NID&fosaid=test-facility")

SEARCH_STATUS=$(echo "$SEARCH_NID_RESPONSE" | jq -r '.status // "UNKNOWN"')
log "    Response Status: $SEARCH_STATUS"
echo "$SEARCH_NID_RESPONSE" | jq '.' > "$TEST_DATA_DIR/search_nid_response.json"

if [ "$SEARCH_STATUS" == "SUCCESS" ] || [ "$SEARCH_STATUS" == "FAILURE" ]; then
    log_success "  Search by NID endpoint responding correctly"

    if [ "$SEARCH_STATUS" == "SUCCESS" ]; then
        RECORD_COUNT=$(echo "$SEARCH_NID_RESPONSE" | jq -r '.recordsCount // 0')
        log "    Found $RECORD_COUNT record(s)"

        if [ "$RECORD_COUNT" -gt 0 ]; then
            ORIGIN=$(echo "$SEARCH_NID_RESPONSE" | jq -r '.results[0].origin // "UNKNOWN"')
            ORIGIN_RANK=$(echo "$SEARCH_NID_RESPONSE" | jq -r '.results[0].originRank // "UNKNOWN"')
            SURNAME=$(echo "$SEARCH_NID_RESPONSE" | jq -r '.results[0].surName // "UNKNOWN"')
            log "    Patient: $SURNAME (Origin: $ORIGIN, Rank: $ORIGIN_RANK)"
        fi
    fi
else
    log_error "  Search by NID endpoint returned unexpected status: $SEARCH_STATUS"
fi

# Test Case 2: Search with Passport (now GET with query parameters)
log "  Test Case 2.1.2: Search by Passport (GET)"

SEARCH_PASSPORT_RESPONSE=$(curl -s -b "$COOKIE_FILE" \
    "$OPENMRS_URL/ws/rwandaprimarycare/findPatient/byDocument?documentNumber=PC1234567&documentType=PASSPORT&fosaid=test-facility")

PASSPORT_STATUS=$(echo "$SEARCH_PASSPORT_RESPONSE" | jq -r '.status // "UNKNOWN"')
log "    Response Status: $PASSPORT_STATUS"

if [ "$PASSPORT_STATUS" == "SUCCESS" ] || [ "$PASSPORT_STATUS" == "FAILURE" ]; then
    log_success "  Search by Passport endpoint responding correctly"
else
    log_error "  Search by Passport endpoint returned unexpected status"
fi

# Test Case 3: Search with Primary Care ID (now GET with query parameters)
log "  Test Case 2.1.3: Search by Primary Care ID (GET)"

# Generate a valid PC ID
GENERATED_PC_ID=$(python3 /Users/smallgod/openmrs/rwanda-emr/docs/scripts/generate_pc_id.py | tail -1 | awk '{print $2}')
log "    Generated PC ID: $GENERATED_PC_ID"

SEARCH_PCID_RESPONSE=$(curl -s -b "$COOKIE_FILE" \
    "$OPENMRS_URL/ws/rwandaprimarycare/findPatient/byDocument?documentNumber=${GENERATED_PC_ID}&documentType=PRIMARY_CARE&fosaid=test-facility")

PCID_STATUS=$(echo "$SEARCH_PCID_RESPONSE" | jq -r '.status // "UNKNOWN"')
log "    Response Status: $PCID_STATUS"

if [ "$PCID_STATUS" == "SUCCESS" ] || [ "$PCID_STATUS" == "FAILURE" ]; then
    log_success "  Search by Primary Care ID endpoint responding correctly"
else
    log_error "  Search by Primary Care ID endpoint returned unexpected status"
fi

# ----------------------------------------------------------------------------
# Test 2.2: Search by Names
# ----------------------------------------------------------------------------

log ""
log "Test 2.2: Search by Names"

# Test Case 1: Search by full name (now GET with query parameters)
log "  Test Case 2.2.1: Search by full name and year (GET)"

SEARCH_NAME_RESPONSE=$(curl -s -b "$COOKIE_FILE" \
    "$OPENMRS_URL/ws/rwandaprimarycare/findPatient/byNames?surName=NIYONSHUTI&postName=Janvier&yearOfBirth=1990&origin=LOCAL")

NAME_STATUS=$(echo "$SEARCH_NAME_RESPONSE" | jq -r '.status // "UNKNOWN"')
log "    Response Status: $NAME_STATUS"
echo "$SEARCH_NAME_RESPONSE" | jq '.' > "$TEST_DATA_DIR/search_name_response.json"

if [ "$NAME_STATUS" == "SUCCESS" ] || [ "$NAME_STATUS" == "FAILURE" ]; then
    log_success "  Search by name endpoint responding correctly"

    if [ "$NAME_STATUS" == "SUCCESS" ]; then
        NAME_RECORD_COUNT=$(echo "$SEARCH_NAME_RESPONSE" | jq -r '.recordsCount // 0')
        log "    Found $NAME_RECORD_COUNT record(s)"
    fi
else
    log_error "  Search by name endpoint returned unexpected status"
fi

# ----------------------------------------------------------------------------
# Test 2.3: Get Locations
# ----------------------------------------------------------------------------

log ""
log "Test 2.3: Get Locations"

LOCATIONS_RESPONSE=$(curl -s -b "$COOKIE_FILE" \
    "$OPENMRS_URL/ws/rwandaprimarycare/findPatient/location")

LOCATIONS_STATUS=$(echo "$LOCATIONS_RESPONSE" | jq -r '.status // "UNKNOWN"')
log "  Response Status: $LOCATIONS_STATUS"
echo "$LOCATIONS_RESPONSE" | jq '.' > "$TEST_DATA_DIR/locations_response.json"

if [ "$LOCATIONS_STATUS" == "SUCCESS" ]; then
    LOCATION_COUNT=$(echo "$LOCATIONS_RESPONSE" | jq '.results | length')
    log_success "Get Locations endpoint working (Found $LOCATION_COUNT locations)"

    # Save first location UUID for later use
    if [ "$LOCATION_COUNT" -gt 0 ]; then
        LOCATION_UUID=$(echo "$LOCATIONS_RESPONSE" | jq -r '.results[0].uuid // ""')
        echo "$LOCATION_UUID" > "$TEST_DATA_DIR/location_uuid.txt"
        log "  Saved location UUID: $LOCATION_UUID"
    fi
else
    log_error "Get Locations endpoint failed"
fi

# ============================================================================
# Phase 3: Create Patient Testing
# ============================================================================

echo ""
log "=========================================="
log "Phase 3: Create Patient Testing"
log "=========================================="

# Generate a new valid Primary Care ID
log "Generating valid Primary Care ID..."
NEW_PC_ID=$(python3 - <<'PYTHON'
import random
BASE_CHARS = "0123456789ACEFHJKMNPUWXY"
BASE = 24

def compute_check_digit(identifier):
    sum_val = 0
    factor = 2
    for char in reversed(identifier):
        value = BASE_CHARS.index(char)
        addend = factor * value
        factor = 3 - factor
        sum_val += addend
    remainder = sum_val % BASE
    check_digit_value = (BASE - remainder) % BASE
    return BASE_CHARS[check_digit_value]

base_id = ''.join(random.choice(BASE_CHARS[:10]) for _ in range(6))
check_digit = compute_check_digit(base_id)
print(f"{base_id}-{check_digit}")
PYTHON
)

log "Generated PC ID: $NEW_PC_ID"

# Get location UUID (or use a default one)
if [ -f "$TEST_DATA_DIR/location_uuid.txt" ]; then
    LOCATION_UUID=$(cat "$TEST_DATA_DIR/location_uuid.txt")
else
    LOCATION_UUID="ba6c3005-fe8e-44e0-bd1d-8253306038eb"  # Default from script
    log_warning "Using default location UUID"
fi

# Get identifier type UUID for PC ID
log "Getting Primary Care identifier type UUID..."
IDENTIFIER_TYPES=$(curl -s -b "$COOKIE_FILE" \
    "$OPENMRS_URL/ws/rest/v1/patientidentifiertype?q=Primary")

PC_ID_TYPE_UUID=$(echo "$IDENTIFIER_TYPES" | jq -r '.results[0].uuid // "c37ab937-0fb6-4d98-8c6f-c394c6fa2e14"')
log "Identifier Type UUID: $PC_ID_TYPE_UUID"

# Create patient payload
log "Creating patient payload..."
cat > "$TEST_DATA_DIR/create_patient.json" << EOF
{
  "surName": "TestPatient",
  "postNames": "AutoGenerated",
  "gender": "M",
  "dateOfBirth": "1995-06-15",
  "fatherName": "TestFather",
  "motherName": "TestMother",
  "phoneNumber": "+250788123456",
  "identifiers": [
    {
      "system": "PRIMARY_CARE_ID",
      "value": "$NEW_PC_ID"
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
  "maritalStatus": "Single",
  "nationality": "Rwandan",
  "educationalLevel": "Secondary",
  "profession": "Student",
  "religion": "Christian"
}
EOF

log "Sending create patient request..."
CREATE_RESPONSE=$(curl -s -b "$COOKIE_FILE" \
    -H "Content-Type: application/json" \
    -X POST \
    -d @"$TEST_DATA_DIR/create_patient.json" \
    "$OPENMRS_URL/ws/rwandaprimarycare/findPatient/create")

echo "$CREATE_RESPONSE" | jq '.' > "$TEST_DATA_DIR/create_response.json" 2>/dev/null || echo "$CREATE_RESPONSE" > "$TEST_DATA_DIR/create_response.txt"

# Parse response
if echo "$CREATE_RESPONSE" | jq -e . >/dev/null 2>&1; then
    CREATE_STATUS=$(echo "$CREATE_RESPONSE" | jq -r '.status // "UNKNOWN"')
    PATIENT_ID=$(echo "$CREATE_RESPONSE" | jq -r '.patientId // ""')

    if [ "$CREATE_STATUS" == "SUCCESS" ] || [ -n "$PATIENT_ID" ]; then
        log_success "Create Patient endpoint working (Patient ID: $PATIENT_ID)"
        echo "$PATIENT_ID" > "$TEST_DATA_DIR/created_patient_id.txt"
        echo "$NEW_PC_ID" > "$TEST_DATA_DIR/created_patient_pcid.txt"
    else
        log_error "Create Patient failed (Status: $CREATE_STATUS)"
    fi
else
    # Response might be a simple string
    if [ -n "$CREATE_RESPONSE" ]; then
        log_success "Create Patient endpoint responded (Response: $CREATE_RESPONSE)"
        echo "$CREATE_RESPONSE" > "$TEST_DATA_DIR/created_patient_id.txt"
    else
        log_error "Create Patient failed (No response)"
    fi
fi

# Verify patient was created by searching for it (now GET with query parameters)
log "Verifying patient was created..."
sleep 2  # Wait for DB commit

VERIFY_SEARCH=$(curl -s -b "$COOKIE_FILE" \
    "$OPENMRS_URL/ws/rwandaprimarycare/findPatient/byDocument?documentNumber=${NEW_PC_ID}&documentType=PRIMARY_CARE&fosaid=test-facility")

VERIFY_STATUS=$(echo "$VERIFY_SEARCH" | jq -r '.status // "UNKNOWN"')

if [ "$VERIFY_STATUS" == "SUCCESS" ]; then
    VERIFY_COUNT=$(echo "$VERIFY_SEARCH" | jq -r '.recordsCount // 0')
    if [ "$VERIFY_COUNT" -gt 0 ]; then
        log_success "Patient creation verified - patient found in search"
    else
        log_warning "Patient created but not found in search (may need sync time)"
    fi
else
    log_warning "Could not verify patient creation via search"
fi

# ============================================================================
# Phase 4: Update Patient Testing
# ============================================================================

echo ""
log "=========================================="
log "Phase 4: Update Patient Testing"
log "=========================================="

# Only test update if we successfully created a patient
if [ -f "$TEST_DATA_DIR/created_patient_pcid.txt" ]; then
    CREATED_PC_ID=$(cat "$TEST_DATA_DIR/created_patient_pcid.txt")

    log "Updating patient with PC ID: $CREATED_PC_ID"

    # Create update payload
    cat > "$TEST_DATA_DIR/update_patient.json" << EOF
{
  "surName": "TestPatient",
  "postNames": "UpdatedName",
  "gender": "M",
  "dateOfBirth": "1995-06-15",
  "phoneNumber": "+250788999999",
  "identifiers": [
    {
      "system": "PRIMARY_CARE_ID",
      "value": "$CREATED_PC_ID"
    }
  ],
  "addressList": [
    {
      "type": "RESIDENTIAL",
      "country": "Rwanda",
      "state": "Kigali",
      "district": "Kicukiro",
      "sector": "Gahanga",
      "cell": "Karembure",
      "city": "Kigali"
    }
  ],
  "maritalStatus": "Married",
  "nationality": "Rwandan",
  "updateCrFlag": true
}
EOF

    log "Sending update patient request..."
    UPDATE_RESPONSE=$(curl -s -b "$COOKIE_FILE" \
        -H "Content-Type: application/json" \
        -X PUT \
        -d @"$TEST_DATA_DIR/update_patient.json" \
        "$OPENMRS_URL/ws/rwandaprimarycare/findPatient/update")

    echo "$UPDATE_RESPONSE" | jq '.' > "$TEST_DATA_DIR/update_response.json" 2>/dev/null || echo "$UPDATE_RESPONSE" > "$TEST_DATA_DIR/update_response.txt"

    if echo "$UPDATE_RESPONSE" | jq -e . >/dev/null 2>&1; then
        UPDATE_STATUS=$(echo "$UPDATE_RESPONSE" | jq -r '.status // "UNKNOWN"')

        if [ "$UPDATE_STATUS" == "COMPLETED" ] || [ "$UPDATE_STATUS" == "SUCCESS" ]; then
            log_success "Update Patient endpoint working (Status: $UPDATE_STATUS)"
        else
            log_error "Update Patient failed (Status: $UPDATE_STATUS)"
        fi
    else
        if [ -n "$UPDATE_RESPONSE" ]; then
            log_success "Update Patient endpoint responded (Response: $UPDATE_RESPONSE)"
        else
            log_error "Update Patient failed (No response)"
        fi
    fi
else
    log_warning "Skipping update test - no patient was created in Phase 3"
fi

# ============================================================================
# Phase 5: Integration Testing
# ============================================================================

echo ""
log "=========================================="
log "Phase 5: Integration Testing"
log "=========================================="

log "Test 5.1: Multi-source search verification"
# Test that search checks multiple sources (LOCAL → CR → NPR)
log "  Testing search priority order..."

# Search for a non-existent patient (now GET with query parameters)
NONEXISTENT_RESPONSE=$(curl -s -b "$COOKIE_FILE" \
    "$OPENMRS_URL/ws/rwandaprimarycare/findPatient/byDocument?documentNumber=9999999999999999&documentType=NID&fosaid=test-facility")

NONEXISTENT_STATUS=$(echo "$NONEXISTENT_RESPONSE" | jq -r '.status // "UNKNOWN"')

if [ "$NONEXISTENT_STATUS" == "FAILURE" ]; then
    log_success "  Multi-source search correctly returns FAILURE for non-existent patient"
elif [ "$NONEXISTENT_STATUS" == "SUCCESS" ]; then
    NONEXISTENT_COUNT=$(echo "$NONEXISTENT_RESPONSE" | jq -r '.recordsCount // 0')
    if [ "$NONEXISTENT_COUNT" -eq 0 ]; then
        log_success "  Multi-source search correctly returns SUCCESS with 0 records"
    else
        log_warning "  Unexpected: Found $NONEXISTENT_COUNT records for non-existent patient"
    fi
else
    log_warning "  Multi-source search status unclear: $NONEXISTENT_STATUS"
fi

log ""
log "Test 5.2: Response structure validation"
# Validate that all required fields are present in responses

SAMPLE_RESPONSE_FILE="$TEST_DATA_DIR/search_nid_response.json"
if [ -f "$SAMPLE_RESPONSE_FILE" ]; then
    log "  Validating response structure..."

    HAS_STATUS=$(jq -e '.status' "$SAMPLE_RESPONSE_FILE" >/dev/null 2>&1 && echo "true" || echo "false")
    HAS_RECORDS_COUNT=$(jq -e '.recordsCount' "$SAMPLE_RESPONSE_FILE" >/dev/null 2>&1 && echo "true" || echo "false")
    HAS_RESULTS=$(jq -e '.results' "$SAMPLE_RESPONSE_FILE" >/dev/null 2>&1 && echo "true" || echo "false")

    if [ "$HAS_STATUS" == "true" ] && [ "$HAS_RESULTS" == "true" ]; then
        log_success "  Response structure is valid (has status and results)"

        # Check if results have required patient fields
        RESULT_COUNT=$(jq '.results | length' "$SAMPLE_RESPONSE_FILE")
        if [ "$RESULT_COUNT" -gt 0 ]; then
            HAS_SURNAME=$(jq -e '.results[0].surName' "$SAMPLE_RESPONSE_FILE" >/dev/null 2>&1 && echo "true" || echo "false")
            HAS_GENDER=$(jq -e '.results[0].gender' "$SAMPLE_RESPONSE_FILE" >/dev/null 2>&1 && echo "true" || echo "false")
            HAS_ORIGIN=$(jq -e '.results[0].origin' "$SAMPLE_RESPONSE_FILE" >/dev/null 2>&1 && echo "true" || echo "false")

            if [ "$HAS_SURNAME" == "true" ] && [ "$HAS_GENDER" == "true" ] && [ "$HAS_ORIGIN" == "true" ]; then
                log_success "  Patient object structure is valid"
            else
                log_warning "  Some patient fields may be missing"
            fi
        fi
    else
        log_error "  Response structure is incomplete"
    fi
else
    log_warning "  No sample response file to validate"
fi

# ============================================================================
# Phase 6: Test Summary & Report
# ============================================================================

echo ""
log "=========================================="
log "Test Summary & Report"
log "=========================================="

log ""
log "Tests Passed: $TESTS_PASSED"
log "Tests Failed: $TESTS_FAILED"
log "Tests Total: $TESTS_TOTAL"

if [ $TESTS_FAILED -eq 0 ]; then
    SUCCESS_RATE=100
else
    SUCCESS_RATE=$(awk "BEGIN {printf \"%.1f\", ($TESTS_PASSED / $TESTS_TOTAL) * 100}")
fi

log "Success Rate: $SUCCESS_RATE%"

echo ""
log "Test artifacts saved in: $TEST_DATA_DIR"
log "Full test log saved to: $TEST_LOG"

echo ""
if [ $TESTS_FAILED -eq 0 ]; then
    log_success "All critical endpoints are working correctly! ✅"
else
    log_warning "Some tests failed. Review the log for details."
fi

echo ""
log "=========================================="
log "Key Findings:"
log "=========================================="

# List all JSON response files
echo ""
log "Response Files Created:"
for file in "$TEST_DATA_DIR"/*.json; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        filesize=$(wc -c < "$file" | tr -d ' ')
        log "  - $filename ($filesize bytes)"
    fi
done

echo ""
log "=========================================="
log "Test Complete!"
log "=========================================="

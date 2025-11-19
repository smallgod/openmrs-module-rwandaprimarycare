#!/bin/bash
# Quick Test: Create Patient Endpoint
#
# Usage: ./quick_test_create.sh

OPENMRS_URL="${OPENMRS_URL:-http://localhost:8080/openmrs}"
COOKIE_FILE="/tmp/openmrs_cookies.txt"

echo "=== Quick Test: Create Patient ==="
echo ""

# Check authentication
if [ ! -f "$COOKIE_FILE" ]; then
    echo "❌ Not authenticated. Run test_emr_auth.sh first."
    exit 1
fi

# Generate valid Primary Care ID
echo "Generating valid Primary Care ID..."
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

echo "Generated PC ID: $NEW_PC_ID"

# Create patient payload
PAYLOAD=$(cat <<EOF
{
  "surName": "QuickTest",
  "postNames": "Patient$(date +%H%M%S)",
  "gender": "M",
  "dateOfBirth": "1995-01-15",
  "fatherName": "TestFather",
  "motherName": "TestMother",
  "phoneNumber": "+250788$(printf '%06d' $RANDOM)",
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
)

echo ""
echo "Creating patient..."
RESPONSE=$(curl -s -b "$COOKIE_FILE" \
    -H "Content-Type: application/json" \
    -X POST \
    -d "$PAYLOAD" \
    "$OPENMRS_URL/ws/rwandaprimarycare/findPatient/create")

echo "Response:"
echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"

# Save patient info for later testing
if [ -n "$RESPONSE" ]; then
    echo ""
    echo "✅ Patient created with PC ID: $NEW_PC_ID"
    echo "$NEW_PC_ID" > /tmp/last_created_patient_pcid.txt
    echo ""
    echo "Saved PC ID to: /tmp/last_created_patient_pcid.txt"
fi

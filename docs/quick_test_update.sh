#!/bin/bash
# Quick Test: Update Patient Endpoint
#
# Usage: ./quick_test_update.sh [pc_id]
# Example: ./quick_test_update.sh 123456-X

OPENMRS_URL="${OPENMRS_URL:-http://localhost:8080/openmrs}"
COOKIE_FILE="/tmp/openmrs_cookies.txt"

# Check authentication
if [ ! -f "$COOKIE_FILE" ]; then
    echo "❌ Not authenticated. Run test_emr_auth.sh first."
    exit 1
fi

# Get PC ID from argument or from last created patient
if [ -n "$1" ]; then
    PC_ID="$1"
elif [ -f "/tmp/last_created_patient_pcid.txt" ]; then
    PC_ID=$(cat /tmp/last_created_patient_pcid.txt)
    echo "Using last created patient PC ID: $PC_ID"
else
    echo "❌ No PC ID provided and no last created patient found."
    echo "Usage: $0 [pc_id]"
    echo "   or: ./quick_test_create.sh (to create a patient first)"
    exit 1
fi

echo "=== Quick Test: Update Patient ==="
echo "PC ID: $PC_ID"
echo ""

# Create update payload
PAYLOAD=$(cat <<EOF
{
  "surName": "UpdatedPatient",
  "postNames": "Updated$(date +%H%M%S)",
  "gender": "M",
  "dateOfBirth": "1995-01-15",
  "phoneNumber": "+250788999999",
  "identifiers": [
    {
      "system": "PRIMARY_CARE_ID",
      "value": "$PC_ID"
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
)

echo "Updating patient..."
RESPONSE=$(curl -s -b "$COOKIE_FILE" \
    -H "Content-Type: application/json" \
    -X PUT \
    -d "$PAYLOAD" \
    "$OPENMRS_URL/ws/rwandaprimarycare/findPatient/update")

echo "Response:"
echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"

# Parse status
if echo "$RESPONSE" | jq -e . >/dev/null 2>&1; then
    STATUS=$(echo "$RESPONSE" | jq -r '.status // "UNKNOWN"')
    echo ""
    echo "Update Status: $STATUS"

    if [ "$STATUS" == "COMPLETED" ] || [ "$STATUS" == "SUCCESS" ]; then
        echo "✅ Patient updated successfully"
    fi
fi

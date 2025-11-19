#!/bin/bash
# Quick Test: Patient Search Endpoints
#
# Usage: ./quick_test_search.sh [document_number] [document_type] [fosaid]
# Example: ./quick_test_search.sh 1199080003158620 NID test-facility

OPENMRS_URL="${OPENMRS_URL:-http://localhost:8080/openmrs}"
COOKIE_FILE="/tmp/openmrs_cookies.txt"

DOCUMENT_NUMBER="${1:-1199080003158620}"
DOCUMENT_TYPE="${2:-NID}"
FOSAID="${3:-test-facility}"

echo "=== Quick Test: Search by Document (GET) ==="
echo "Document: $DOCUMENT_NUMBER ($DOCUMENT_TYPE)"
echo ""

# Check authentication
if [ ! -f "$COOKIE_FILE" ]; then
    echo "❌ Not authenticated. Run test_emr_auth.sh first."
    exit 1
fi

# Build query string
QUERY_STRING="documentNumber=${DOCUMENT_NUMBER}&documentType=${DOCUMENT_TYPE}"
if [ -n "$FOSAID" ]; then
    QUERY_STRING="${QUERY_STRING}&fosaid=${FOSAID}"
fi

# Test search by document (GET with query parameters, uses /ws/ prefix for REST endpoints)
echo "Searching with GET request..."
RESPONSE=$(curl -s -b "$COOKIE_FILE" \
    "$OPENMRS_URL/ws/rwandaprimarycare/findPatient/byDocument?${QUERY_STRING}")

echo "Response:"
echo "$RESPONSE" | jq '.' || echo "$RESPONSE"

# Parse key info
STATUS=$(echo "$RESPONSE" | jq -r '.status // "UNKNOWN"')
RECORD_COUNT=$(echo "$RESPONSE" | jq -r '.recordsCount // 0')

echo ""
echo "Status: $STATUS"
echo "Records Found: $RECORD_COUNT"

if [ "$RECORD_COUNT" -gt 0 ]; then
    echo ""
    echo "First Patient:"
    echo "$RESPONSE" | jq '.results[0] | {
        name: (.surName + " " + .postNames),
        gender: .gender,
        dob: .dateOfBirth,
        origin: .origin,
        rank: .originRank
    }'
fi

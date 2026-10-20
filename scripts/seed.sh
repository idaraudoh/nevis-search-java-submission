#!/usr/bin/env bash
# Load demo data into a running API.  Usage: scripts/seed.sh [base_url]
set -euo pipefail
BASE="${1:-http://localhost:8000}"
J='content-type: application/json'

id_of() { sed -E 's/.*"id":"([^"]+)".*/\1/'; }

JOHN=$(curl -sf -X POST "$BASE/clients" -H "$J" -d '{
  "first_name":"John","last_name":"Doe","email":"john.doe@neviswealth.com",
  "description":"Senior partner. Interested in ESG funds.",
  "social_links":["https://linkedin.com/in/johndoe"]}' | id_of)
curl -sf -X POST "$BASE/clients/$JOHN/documents" -H "$J" -d '{
  "title":"March utility statement",
  "content":"Utility bill from British Gas for March. Service address: 12 High Street, London. Amount due: 84.20 GBP. Direct debit set up."}' >/dev/null
curl -sf -X POST "$BASE/clients/$JOHN/documents" -H "$J" -d '{
  "title":"Passport copy","content":"UK passport, valid until 2031. Holder: John Doe."}' >/dev/null

MARIA=$(curl -sf -X POST "$BASE/clients" -H "$J" -d '{
  "first_name":"Maria","last_name":"Garcia","email":"maria@acme.io",
  "description":"Runs a family office in Madrid."}' | id_of)
curl -sf -X POST "$BASE/clients/$MARIA/documents" -H "$J" -d '{
  "title":"Proof of address letter",
  "content":"This letter is proof of address for Maria Garcia, resident at Calle Mayor 4, Madrid."}' >/dev/null
curl -sf -X POST "$BASE/clients/$MARIA/documents" -H "$J" -d '{
  "title":"Q3 portfolio review",
  "content":"Equity allocation is 60 percent. Bond allocation is 40 percent. Rebalancing is planned for November."}' >/dev/null

echo "Seeded 2 clients and 4 documents."

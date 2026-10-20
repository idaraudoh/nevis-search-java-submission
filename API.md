# Nevis Search API Documentation

## Overview

The Nevis Search API enables wealth management advisors to search across clients and their documents using natural language queries. The API supports exact matching, prefix matching, and semantic search capabilities.

**Base URL:** `http://localhost:8000`

**API Specification:** [openapi.yaml](./openapi.yaml)

**Interactive Documentation:** http://localhost:8000/docs (Swagger UI)

## Authentication

Currently, no authentication is required. In production, this would be protected by OAuth 2.0 or similar.

## Response Format

All responses are in JSON format. Errors follow a consistent structure:

```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Detailed error message",
  "fields": {
    "field_name": "field-specific error"
  }
}
```

The `fields` object is only present for validation errors.

## Endpoints

### Health Check

**GET** `/health`

Check if the service is running.

**Response 200:**
```json
{
  "status": "UP"
}
```

---

### Create Client

**POST** `/clients`

Create a new client record.

**Request Body:**
```json
{
  "first_name": "John",
  "last_name": "Doe",
  "email": "john.doe@neviswealth.com",
  "description": "Senior partner. Interested in ESG funds.",
  "social_links": [
    "https://linkedin.com/in/johndoe"
  ]
}
```

**Required fields:** `first_name`, `last_name`, `email`

**Response 201:**
```json
{
  "id": "955671d6-a2b3-4c5d-8e9f-0a1b2c3d4e5f",
  "first_name": "John",
  "last_name": "Doe",
  "email": "john.doe@neviswealth.com",
  "description": "Senior partner. Interested in ESG funds.",
  "social_links": [
    "https://linkedin.com/in/johndoe"
  ],
  "created_at": "2026-10-18T22:46:09Z"
}
```

**Errors:**
- `400` - Malformed JSON
- `409` - Email already exists
- `422` - Validation failed

---

### Get Client

**GET** `/clients/{id}`

Retrieve a specific client by ID.

**Path Parameters:**
- `id` (string, required) - Client UUID

**Response 200:**
```json
{
  "id": "955671d6-a2b3-4c5d-8e9f-0a1b2c3d4e5f",
  "first_name": "John",
  "last_name": "Doe",
  "email": "john.doe@neviswealth.com",
  "description": "Senior partner. Interested in ESG funds.",
  "social_links": [
    "https://linkedin.com/in/johndoe"
  ],
  "created_at": "2026-10-18T22:46:09Z"
}
```

**Errors:**
- `404` - Client not found

---

### Create Document

**POST** `/clients/{id}/documents`

Add a document to a client.

**Path Parameters:**
- `id` (string, required) - Client UUID

**Request Body:**
```json
{
  "title": "UK passport",
  "content": "UK passport, valid until 2031. Holder: John Doe."
}
```

**Required fields:** `title`, `content`

**Response 201:**
```json
{
  "id": "a9257743-4b2e-4f8c-9d1a-6e7f8a9b0c1d",
  "client_id": "955671d6-a2b3-4c5d-8e9f-0a1b2c3d4e5f",
  "title": "UK passport",
  "content": "UK passport, valid until 2031. Holder: John Doe.",
  "created_at": "2026-10-18T22:46:30Z"
}
```

**Errors:**
- `404` - Client not found
- `422` - Validation failed

---

### Get Document

**GET** `/documents/{id}`

Retrieve a specific document by ID.

**Path Parameters:**
- `id` (string, required) - Document UUID

**Response 200:**
```json
{
  "id": "a9257743-4b2e-4f8c-9d1a-6e7f8a9b0c1d",
  "client_id": "955671d6-a2b3-4c5d-8e9f-0a1b2c3d4e5f",
  "title": "UK passport",
  "content": "UK passport, valid until 2031. Holder: John Doe.",
  "created_at": "2026-10-18T22:46:30Z"
}
```

**Errors:**
- `404` - Document not found

---

### Get Document Summary

**GET** `/documents/{id}/summary`

Get a quick summary of document content.

**Path Parameters:**
- `id` (string, required) - Document UUID

**Response 200:**
```json
{
  "document_id": "a9257743-4b2e-4f8c-9d1a-6e7f8a9b0c1d",
  "summary": "UK passport, valid until 2031. Holder: John Doe.",
  "method": "extractive"
}
```

**Fields:**
- `summary` - The generated summary text
- `method` - How it was generated: `extractive` (default) or `llm` (when Anthropic API key is configured)

**Errors:**
- `404` - Document not found

---

### Search

**GET** `/search`

Search across clients and documents.

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `q` | string | Yes | - | Search query (max 500 chars) |
| `type` | string | No | `all` | Filter by type: `all`, `client`, or `document` |
| `limit` | integer | No | `20` | Max results (1-100) |
| `client_id` | string | No | - | Search only within this client |

**Example Requests:**

```bash
# Search everything
curl "localhost:8000/search?q=address%20proof"

# Search only clients
curl "localhost:8000/search?q=john&type=client&limit=5"

# Search within one client
curl "localhost:8000/search?q=passport&client_id=955671d6-a2b3-4c5d-8e9f-0a1b2c3d4e5f"
```

**Response 200:**

Returns an array of search results sorted by relevance score (0-1, higher is better).

**Client Hit Example:**
```json
{
  "type": "client",
  "score": 1.0,
  "client": {
    "id": "955671d6-a2b3-4c5d-8e9f-0a1b2c3d4e5f",
    "first_name": "John",
    "last_name": "Doe",
    "email": "john.doe@neviswealth.com",
    "description": "Senior partner. Interested in ESG funds.",
    "social_links": ["https://linkedin.com/in/johndoe"],
    "created_at": "2026-10-18T22:46:09Z"
  },
  "matched_on": ["email"]
}
```

**Document Hit Example:**
```json
{
  "type": "document",
  "score": 0.6206,
  "document": {
    "id": "8e9a52f9-1b2c-3d4e-5f6a-7b8c9d0e1f2a",
    "client_id": "4c1b7a20-3b4c-5d6e-7f8a-9b0c1d2e3f4a",
    "title": "Proof of address letter",
    "content": "This letter is proof of address for Maria Garcia...",
    "created_at": "2026-10-18T22:47:15Z"
  },
  "owner": {
    "id": "4c1b7a20-3b4c-5d6e-7f8a-9b0c1d2e3f4a",
    "first_name": "Maria",
    "last_name": "Garcia",
    "email": "maria@acme.io"
  },
  "snippet": "This letter is proof of address for Maria Garcia, resident at Calle Mayor 4, Madrid.",
  "summary": "This letter is proof of address for Maria Garcia, resident at Calle Mayor 4, Madrid.",
  "matched_terms": ["address", "proof"],
  "via_expansion": []
}
```

**Result Fields:**

*Client hits include:*
- `type` - Always "client"
- `score` - Relevance score (0-1)
- `client` - Full client object
- `matched_on` - Array of fields that matched (e.g., ["email", "first_name"])

*Document hits include:*
- `type` - Always "document"
- `score` - Relevance score (0-1)
- `document` - Full document object
- `owner` - Summary of the client who owns the document
- `snippet` - Relevant excerpt from the document
- `summary` - Quick summary of the content
- `matched_terms` - Query terms that appeared in the document
- `via_expansion` - Related phrases that caused the match (e.g., ["utility bill"] for query "address proof")

**Errors:**
- `404` - Unknown `client_id`
- `422` - Invalid parameters:
  - Missing or blank `q`
  - Invalid `type` (must be all, client, or document)
  - Invalid `limit` (must be 1-100)
  - Blank `client_id`

---

## Search Capabilities

### Client Search

Searches across:
- First name
- Last name
- Email
- Description (0.6 weight)
- Social links (0.5 weight)

**Matching types:**
- **Exact token match** (score: 1.0) - "john" matches "john"
- **Prefix match** (score: 0.8) - "nevis" matches "neviswealth"
- **Substring match** (score: 0.7) - "wealth" matches "neviswealth.com"
- **Fuzzy match** (score: 0.7 × similarity) - Handles typos for words 5+ chars

Results below 0.55 score are filtered out.

### Document Search

Uses BM25 ranking with semantic expansion:

1. **Exact match** - Words from the query
2. **Semantic expansion** - Related terms (0.6 weight)
   - Default: Built-in concept map (e.g., "address proof" → "utility bill", "council tax")
   - Optional: LLM-generated expansions when API key is configured

**Features:**
- Title words count double
- Stemming (plurals normalized)
- Stop words ignored
- Results include explanation (`via_expansion`) of why non-obvious matches appeared

### Scoped Search

Using `client_id` parameter:
- Filters results to one client and their documents
- Scoring uses full corpus statistics (scores remain consistent)
- Returns 404 if client doesn't exist
- Returns empty array if no matches found

---

## Example Use Cases

### 1. Find client by company email

```bash
curl "localhost:8000/search?q=NevisWealth"
```

Returns John Doe whose email is `john.doe@neviswealth.com`

### 2. Find similar documents

```bash
curl "localhost:8000/search?q=address%20proof"
```

Returns:
- Documents literally containing "address proof"
- Documents containing "utility bill" (expanded term)
- Documents containing "bank statement" (expanded term)

The `via_expansion` field shows why each document matched.

### 3. Search one client's documents

```bash
# First get the client ID
curl "localhost:8000/search?q=john&type=client"

# Then search their documents
curl "localhost:8000/search?q=passport&client_id=<client_id>"
```

---

## Configuration

### Optional: Enable LLM Features

Set environment variable:
```bash
ANTHROPIC_API_KEY=sk-ant-...
```

This enables:
1. **LLM query expansion** - Additional semantic terms from Claude
2. **LLM summarization** - Better document summaries

**Fallback behavior:** If LLM call fails or times out, the API automatically falls back to built-in algorithms. Search never breaks due to LLM issues.

### Other Configuration

Via environment variables:
- `SPRING_DATASOURCE_URL` - Postgres connection string
- `SPRING_DATASOURCE_USERNAME` - Database user
- `SPRING_DATASOURCE_PASSWORD` - Database password
- `LLM_MODEL` - Model name (default: claude-sonnet-5-5)

---

## Rate Limits

Currently, no rate limits are enforced. In production, consider:
- Per-user rate limits
- Query complexity limits
- Result size limits

## Testing

Load demo data:
```bash
scripts/seed.sh
```

This creates sample clients (John Doe, Maria Garcia) and documents (passport, utility bills, proof of address) for testing search capabilities.

---

## Next Steps / Known Limitations

- No authentication/authorization
- No pagination (results capped at 100)
- No document updates or deletes
- No multi-tenancy (advisors can't be scoped to their own clients)
- In-memory scoring (works for thousands of rows, not millions)
- For scale, consider migrating to Postgres `pgvector` + `tsvector`

## Support

For questions about this API, contact:
- Grigory Sobko (grigory.sobko@neviswealth.com)
- Ksenia Filimonova (ksenia.filimonova@neviswealth.com)

# Nevis Search API (Java)

Search across wealth-management clients and their documents.

* Find clients by name, email, description. `NevisWealth` finds `john.doe@neviswealth.com`.
* Find documents by meaning. `address proof` finds a document that says `utility bill`.
* Every document hit comes with a snippet and a quick summary.

Stack: Java 21, Spring Boot 3.3, Spring Data JPA, Postgres, springdoc (Swagger UI), JUnit 5.

##  Live Deployment

**API Base URL:** https://nevis-search-api-ytiv.onrender.com

**Swagger UI (Interactive Docs):** https://nevis-search-api-ytiv.onrender.com/docs

**Important:** The app runs on Render's free tier and may take 30-50 seconds to wake up on the first request after 15 minutes of inactivity. Subsequent requests are instant.

### Quick Test

```bash
# Health check
curl https://nevis-search-api-ytiv.onrender.com/health

# Search (semantic matching works!)
curl "https://nevis-search-api-ytiv.onrender.com/search?q=address%20proof"
```

## Run it

```bash
docker compose up --build
```

API: http://localhost:8000
Swagger UI: http://localhost:8000/docs
OpenAPI spec: [openapi.yaml](./openapi.yaml) or http://localhost:8000/v3/api-docs

Load demo data:

```bash
scripts/seed.sh
```

Run from your IDE or shell instead (needs Java 21 and Maven):

```bash
docker compose up -d db
mvn spring-boot:run          # listens on :8080
```

Run the tests (no Docker, no network, no API key; integration tests use in-memory H2):

```bash
mvn test
```

## Optional: turn on the LLM

Set `ANTHROPIC_API_KEY` (see `.env.example`) and restart. Two things change:

1. Query expansion gets extra phrases from Claude, on top of the built-in map.
2. `GET /documents/{id}/summary` is written by Claude instead of extracted.

If the LLM call fails or times out, the API silently falls back to the offline path. Search never breaks because of it.

## Endpoints

**📄 Full API Documentation:** See [openapi.yaml](./openapi.yaml) for the complete OpenAPI 3.1 specification.

| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/clients` | 201 | 409 duplicate email, 422 invalid body, 400 malformed JSON |
| GET | `/clients/{id}` | 200 | 404 |
| POST | `/clients/{id}/documents` | 201 | 404 unknown client, 422 invalid body |
| GET | `/documents/{id}` | 200 | 404 |
| GET | `/documents/{id}/summary` | 200 | 404 |
| GET | `/search?q=&type=&limit=&client_id=` | 200 | 404 unknown `client_id`, 422 missing or blank `q`, bad `type`, bad `limit`, blank `client_id` |
| GET | `/health` | 200 | |

Extras beyond the spec: `created_at` on Client, the GET endpoints, the summary endpoint, `type` / `limit` / `client_id` on search, and `owner` on document hits.

All errors share one shape: `{"status": 422, "error": "Unprocessable Entity", "message": "...", "fields": {"email": "..."}}`. `fields` appears on validation errors only.

### Search response

`GET /search` returns a list sorted by `score` (0 to 1, higher is better). Each item is a client hit or a document hit. Fields that do not apply are omitted.

Client hit: `type`, `score`, `client`, `matched_on`.
Document hit: `type`, `score`, `document`, `owner`, `snippet`, `summary`, `matched_terms`, `via_expansion`.

`owner` is the client the document belongs to (`id`, `first_name`, `last_name`, `email`), so an advisor can see whose utility bill it is without a second call. Client hits have no `owner`.

`via_expansion` is the useful one. It tells the advisor why a document showed up when the words never appeared in the query.

## Example queries

Seeded with `scripts/seed.sh`. IDs and timestamps shortened.

### 1. Client by company name

```bash
curl "localhost:8000/search?q=NevisWealth"
```

```json
[
  {
    "type": "client",
    "score": 1.0,
    "client": {
      "id": "955671d6-...",
      "first_name": "John",
      "last_name": "Doe",
      "email": "john.doe@neviswealth.com",
      "description": "Senior partner. Interested in ESG funds.",
      "social_links": ["https://linkedin.com/in/johndoe"],
      "created_at": "2026-10-18T22:46:09Z"
    },
    "matched_on": ["email"]
  }
]
```

`nevis wealth` (with a space) and `nevis` (prefix) find the same client.

### 2. Documents by related term

```bash
curl "localhost:8000/search?q=address%20proof"
```

```json
[
  {
    "type": "document",
    "score": 0.6206,
    "document": { "id": "8e9a52f9-...", "title": "Proof of address letter", "...": "..." },
    "owner": { "id": "4c1b7a20-...", "first_name": "Maria", "last_name": "Garcia", "email": "maria@acme.io" },
    "snippet": "This letter is proof of address for Maria Garcia, resident at Calle Mayor 4, Madrid.",
    "summary": "This letter is proof of address for Maria Garcia, resident at Calle Mayor 4, Madrid.",
    "matched_terms": ["address", "proof"],
    "via_expansion": []
  },
  {
    "type": "document",
    "score": 0.3634,
    "document": { "id": "6ea03f15-...", "title": "March utility statement", "...": "..." },
    "owner": { "id": "955671d6-...", "first_name": "John", "last_name": "Doe", "email": "john.doe@neviswealth.com" },
    "snippet": "Utility bill from British Gas for March.",
    "summary": "Utility bill from British Gas for March. Amount due: 84.20 GBP. Direct debit set up.",
    "matched_terms": ["address"],
    "via_expansion": ["utility bill"]
  }
]
```

The literal match ranks first. The utility bill still shows up, and `via_expansion` says why.

### 3. Filter by type

```bash
curl "localhost:8000/search?q=john&type=client&limit=5"
```

```json
[
  {
    "type": "client",
    "score": 1.0,
    "client": {
      "id": "955671d6-...",
      "first_name": "John",
      "last_name": "Doe",
      "email": "john.doe@neviswealth.com",
      "description": "Senior partner. Interested in ESG funds.",
      "social_links": ["https://linkedin.com/in/johndoe"],
      "created_at": "2026-10-18T22:46:09Z"
    },
    "matched_on": ["first_name"]
  }
]
```

### 4. Search inside one client's file

```bash
curl "localhost:8000/search?q=address%20proof&client_id=<john's id>"
```

Only that client and that client's documents can match. With the seed data this returns John's utility statement and not Maria's proof of address letter. A document scores the same with or without `client_id`.

```json
[
  {
    "type": "document",
    "score": 0.3634,
    "document": {
      "id": "6ea03f15-...",
      "client_id": "955671d6-...",
      "title": "March utility statement",
      "content": "Utility bill from British Gas for March...",
      "created_at": "2026-10-18T22:48:32Z"
    },
    "owner": {
      "id": "955671d6-...",
      "first_name": "John",
      "last_name": "Doe",
      "email": "john.doe@neviswealth.com"
    },
    "snippet": "Utility bill from British Gas for March.",
    "summary": "Utility bill from British Gas for March. Amount due: 84.20 GBP. Direct debit set up.",
    "matched_terms": ["address"],
    "via_expansion": ["utility bill"]
  }
]
```

* Unknown `client_id`: `404`.
* Known client, nothing matches: `200` with `[]`.
* Blank `client_id`: `422`.

### 5. Summary

```bash
curl "localhost:8000/documents/<id>/summary"
```

```json
{
  "document_id": "a9257743-...",
  "summary": "UK passport, valid until 2031. Holder: John Doe.",
  "method": "extractive"
}
```

`method` is `llm` when the LLM wrote it.

### 6. Errors

```bash
curl -i "localhost:8000/search"                    # 422, q is required
curl -i -X POST localhost:8000/clients/nope/documents \
  -H 'content-type: application/json' -d '{"title":"a","content":"b"}'   # 404
```

## How search works

### Clients

Per query token, take the best score across fields, then average.

| Match | Score |
|---|---|
| exact token | 1.0 |
| prefix (`nevis` in `neviswealth`) | 0.8 |
| substring after stripping punctuation | 0.7 |
| fuzzy (typo or swap, similarity 80%+, token 5+ chars) | 0.7 x similarity |

Field weights: email, first name, last name = 1.0. Description = 0.6. Social links = 0.5.

Tokens can match in different fields, so `john neviswealth` works. The glued query is also checked, so `nevis wealth` hits `neviswealth`. Results under 0.55 are dropped.

### Documents

Let's say the query is `address proof` and a document says `utility bill`.

1. BM25 on the query words finds nothing in that document.
2. The expander adds related phrases: `utility bill`, `council tax`, `bank statement`, and so on.
3. Those phrases are scored with BM25 too, at 0.6 weight.
4. A document that literally says `proof of address` still wins.

Title words count double. Plurals are stemmed (`bills` = `bill`). Stop words are ignored, so `proof of address` and `address proof` give the same results.

The final score is `total / (total + 2)`, which keeps it in (0, 1).

## Design decisions

**Core has no framework.** Everything under `com.nevis.search.core` is plain Java. It never touches Spring or JPA, so it is fast to test and easy to move. The service layer maps entities to small records (`ClientData`, `DocData`) and back.

**Solution A: concept map (default).** A curated list of synonym groups for KYC paperwork: proof of address, ID, income, tax, source of wealth, and more. Deterministic, free, instant, easy to test. Weakness: it only knows what is in the map.

**Solution B: LLM expansion (optional).** Claude suggests phrases for any query. Covers the long tail. Weakness: latency, cost, non-determinism. Successful results are cached per query. Failures are not cached and fall back to Solution A.

**Why not embeddings?** They are the right call at scale. I skipped them to keep the stack small, offline and testable, and to avoid a model download in the image. See below.

**Summaries.** Solution A is extractive (top 3 sentences by term frequency, title boost, opener boost). Solution B is the LLM. Search results always use the cheap extractive one so a search never triggers LLM calls per hit.

**Client scope ranks first, then filters.** `client_id` removes other clients' hits after scoring, rather than ranking only that client's documents. The cost is none (search already loads every row). The benefit is that a document's score and order do not change when you add the filter, because the BM25 statistics always come from the whole corpus. Ranking a single client's handful of documents on their own would make scores jump around and become incomparable.

**Status codes.** 422 for valid JSON with bad values, 400 for broken JSON, 409 for duplicate email, 404 for unknown ids.

## Known limits and next steps

* Search loads all rows and scores in memory. Fine for a demo and thousands of rows. Not for millions.
* At scale, swap in Postgres `pgvector` for semantic recall plus `tsvector` for lexical, and merge with reciprocal rank fusion. `QueryExpander` and `DocumentRanker` are the seams.
* Concept map is English and UK/US finance flavoured.
* No auth, no pagination cursor, no document update or delete.
* `client_id` scopes search to one client, but there is still no multi-tenancy. A real advisor platform needs search limited to the advisor's own clients.
* Schema is created by Hibernate (`ddl-auto: update`). Use Flyway for production.

## Layout

```
src/main/java/com/nevis/search/
  NevisSearchApplication.java
  api/        controllers, request/response records, error handler
  service/    ClientService, DocumentService, SearchService
  domain/     JPA entities and repositories
  config/     LLM properties, bean wiring (A vs A+B)
  core/       framework-free search logic
    TextUtil, ClientMatcher, DocumentRanker
    ConceptExpander, LlmExpander, HybridExpander
    ExtractiveSummarizer, LlmSummarizer, AnthropicClient
src/test/java/
  core/       51 unit tests
  api/        30 MockMvc integration tests (H2)
scripts/seed.sh
```

## Deploying

The image is a plain Spring Boot jar. Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` and optionally `ANTHROPIC_API_KEY`. Any container host works (Fly.io, Render, Cloud Run) plus a managed Postgres.

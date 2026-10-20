package com.nevis.search.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nevis.search.domain.ClientRepository;
import com.nevis.search.domain.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ClientRepository clients;
    @Autowired DocumentRepository documents;

    @BeforeEach
    void clean() {
        documents.deleteAll();
        clients.deleteAll();
    }

    // ---- helpers ----------------------------------------------------------

    private static final String ANA = """
            {"first_name":"Ana","last_name":"Lopez","email":"ana@example.com"}""";

    private String createClient(String body) throws Exception {
        String res = mvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("id").asText();
    }

    private String john() throws Exception {
        return createClient("""
                {"first_name":"John","last_name":"Doe","email":"john.doe@neviswealth.com",
                 "description":"Senior partner, interested in ESG funds",
                 "social_links":["https://linkedin.com/in/johndoe"]}""");
    }

    private String addDoc(String clientId, String title, String content) throws Exception {
        String body = json.writeValueAsString(java.util.Map.of("title", title, "content", content));
        String res = mvc.perform(post("/clients/" + clientId + "/documents")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("id").asText();
    }

    // ---- POST /clients ----------------------------------------------------

    @Test
    void createClientReturns201WithShape() throws Exception {
        mvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON).content("""
                        {"first_name":"Ana","last_name":"Lopez","email":"ana@example.com",
                         "description":"Hi","social_links":["x.com/ana"]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email", is("ana@example.com")))
                .andExpect(jsonPath("$.social_links", contains("x.com/ana")))
                .andExpect(jsonPath("$.created_at", notNullValue()));
    }

    @Test
    void createClientDefaultsOptionalFields() throws Exception {
        mvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON).content(ANA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.social_links", empty()));
    }

    @Test
    void invalidEmailIs422() throws Exception {
        mvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON).content(
                        """
                        {"first_name":"Ana","last_name":"Lopez","email":"nope"}"""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fields.email", notNullValue()));
    }

    @Test
    void missingRequiredFieldsAre422() throws Exception {
        for (String body : new String[] {
                """
                {"last_name":"Lopez","email":"ana@example.com"}""",
                """
                {"first_name":"Ana","email":"ana@example.com"}""",
                """
                {"first_name":"Ana","last_name":"Lopez"}""",
                """
                {"first_name":"   ","last_name":"Lopez","email":"ana@example.com"}"""}) {
            mvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateEmailIs409CaseInsensitive() throws Exception {
        createClient(ANA);
        mvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON).content("""
                        {"first_name":"Ana","last_name":"Lopez","email":"ANA@example.com"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void getClientAnd404() throws Exception {
        String id = john();
        mvc.perform(get("/clients/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.id", is(id)));
        mvc.perform(get("/clients/missing")).andExpect(status().isNotFound());
    }

    // ---- documents --------------------------------------------------------

    @Test
    void createDocumentReturns201() throws Exception {
        String clientId = john();
        mvc.perform(post("/clients/" + clientId + "/documents")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"title":"T","content":"C"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.client_id", is(clientId)))
                .andExpect(jsonPath("$.created_at", notNullValue()));
    }

    @Test
    void createDocumentForUnknownClientIs404() throws Exception {
        mvc.perform(post("/clients/nope/documents")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"title":"T","content":"C"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void createDocumentValidation() throws Exception {
        String url = "/clients/" + john() + "/documents";
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"T"}""")).andExpect(status().isUnprocessableEntity());
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"","content":"C"}""")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unknownDocumentIs404() throws Exception {
        mvc.perform(get("/documents/missing")).andExpect(status().isNotFound());
        mvc.perform(get("/documents/missing/summary")).andExpect(status().isNotFound());
    }

    @Test
    void summaryEndpoint() throws Exception {
        String docId = addDoc(john(), "Bill", "Utility bill. Service address is 12 High Street.");
        mvc.perform(get("/documents/" + docId + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method", is("extractive")))
                .andExpect(jsonPath("$.summary", is("Utility bill. Service address is 12 High Street.")));
    }

    // ---- search -----------------------------------------------------------

    @Test
    void searchRequiresQ() throws Exception {
        mvc.perform(get("/search")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void blankQIs422() throws Exception {
        mvc.perform(get("/search").param("q", "   ")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void badTypeAndLimitAre422() throws Exception {
        mvc.perform(get("/search").param("q", "x").param("limit", "0")).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/search").param("q", "x").param("limit", "abc")).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/search").param("q", "x").param("type", "nope")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void emptyDatabaseReturnsEmptyList() throws Exception {
        mvc.perform(get("/search").param("q", "anything"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void findsClientByCompanyName() throws Exception {
        john();
        mvc.perform(get("/search").param("q", "NevisWealth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type", is("client")))
                .andExpect(jsonPath("$[0].client.email", is("john.doe@neviswealth.com")))
                .andExpect(jsonPath("$[0].matched_on", contains("email")))
                .andExpect(jsonPath("$[0].document").doesNotExist()); // null fields are omitted
    }

    @Test
    void findsDocumentByRelatedTerm() throws Exception {
        String clientId = john();
        addDoc(clientId, "March statement", "Utility bill for 12 High Street, London.");
        addDoc(clientId, "Notes", "Discussed ESG allocation.");
        mvc.perform(get("/search").param("q", "address proof"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("document")))
                .andExpect(jsonPath("$[0].document.title", is("March statement")))
                .andExpect(jsonPath("$[0].via_expansion", contains("utility bill")))
                .andExpect(jsonPath("$[0].snippet", is("Utility bill for 12 High Street, London.")))
                .andExpect(jsonPath("$[0].summary", notNullValue()));
    }

    @Test
    void typeFilter() throws Exception {
        String clientId = john();
        addDoc(clientId, "Doc", "john doe notes");
        mvc.perform(get("/search").param("q", "john")).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/search").param("q", "john").param("type", "client"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("client")));
        mvc.perform(get("/search").param("q", "john").param("type", "document"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("document")));
    }

    @Test
    void limitIsApplied() throws Exception {
        for (int i = 0; i < 5; i++) {
            createClient("{\"first_name\":\"Ana\",\"last_name\":\"Lopez\",\"email\":\"ana" + i + "@example.com\"}");
        }
        mvc.perform(get("/search").param("q", "ana").param("limit", "3")).andExpect(jsonPath("$", hasSize(3)));
    }

    // ---- document owner ---------------------------------------------------

    @Test
    void documentHitCarriesOwner() throws Exception {
        String clientId = john();
        addDoc(clientId, "March statement", "Utility bill for 12 High Street, London.");
        mvc.perform(get("/search").param("q", "address proof"))
                .andExpect(jsonPath("$[0].owner.id", is(clientId)))
                .andExpect(jsonPath("$[0].owner.first_name", is("John")))
                .andExpect(jsonPath("$[0].owner.last_name", is("Doe")))
                .andExpect(jsonPath("$[0].owner.email", is("john.doe@neviswealth.com")))
                .andExpect(jsonPath("$[0].owner.description").doesNotExist()); // summary only
    }

    @Test
    void clientHitHasNoOwner() throws Exception {
        john();
        mvc.perform(get("/search").param("q", "NevisWealth"))
                .andExpect(jsonPath("$[0].type", is("client")))
                .andExpect(jsonPath("$[0].owner").doesNotExist());
    }

    @Test
    void eachDocumentHitNamesItsOwnClient() throws Exception {
        String johnId = john();
        String anaId = createClient(ANA);
        addDoc(johnId, "John bill", "Utility bill for John.");
        addDoc(anaId, "Ana bill", "Utility bill for Ana.");
        String body = mvc.perform(get("/search").param("q", "utility bill").param("type", "document"))
                .andReturn().getResponse().getContentAsString();
        for (var hit : json.readTree(body)) {
            String owner = hit.get("owner").get("id").asText();
            org.junit.jupiter.api.Assertions.assertEquals(hit.get("document").get("client_id").asText(), owner);
        }
    }

    // ---- client_id scope --------------------------------------------------

    @Test
    void clientIdLimitsDocumentsToThatClient() throws Exception {
        String johnId = john();
        String anaId = createClient(ANA);
        addDoc(johnId, "John bill", "Utility bill for John.");
        addDoc(anaId, "Ana bill", "Utility bill for Ana.");
        mvc.perform(get("/search").param("q", "address proof")).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/search").param("q", "address proof").param("client_id", anaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].document.title", is("Ana bill")))
                .andExpect(jsonPath("$[0].owner.id", is(anaId)));
    }

    @Test
    void clientIdLimitsClientHitsToThatClient() throws Exception {
        String johnId = john();
        String anaId = createClient(ANA);
        mvc.perform(get("/search").param("q", "john").param("client_id", anaId))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/search").param("q", "john").param("client_id", johnId))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].client.id", is(johnId)));
    }

    @Test
    void clientIdDoesNotChangeScores() throws Exception {
        String johnId = john();
        String anaId = createClient(ANA);
        addDoc(johnId, "John bill", "Utility bill for John.");
        addDoc(anaId, "Ana passport", "UK passport valid until 2031.");
        String all = mvc.perform(get("/search").param("q", "address proof").param("type", "document"))
                .andReturn().getResponse().getContentAsString();
        String scoped = mvc.perform(get("/search").param("q", "address proof").param("type", "document")
                        .param("client_id", johnId))
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(
                json.readTree(all).get(0).get("score"), json.readTree(scoped).get(0).get("score"));
    }

    @Test
    void clientIdWithNoMatchesIsEmptyNot404() throws Exception {
        String anaId = createClient(ANA);
        addDoc(john(), "Bill", "Utility bill.");
        mvc.perform(get("/search").param("q", "address proof").param("client_id", anaId))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void unknownClientIdIs404() throws Exception {
        mvc.perform(get("/search").param("q", "x").param("client_id", "nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void blankClientIdIs422() throws Exception {
        mvc.perform(get("/search").param("q", "x").param("client_id", "  "))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void healthAndOpenApi() throws Exception {
        mvc.perform(get("/health")).andExpect(jsonPath("$.status", is("ok")));
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/search']", notNullValue()))
                .andExpect(jsonPath("$.paths['/clients/{id}/documents']", notNullValue()));
    }
}

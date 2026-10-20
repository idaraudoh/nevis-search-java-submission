package com.nevis.search.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ClientMatcherTest {

    private static ClientData c(String id, String first, String last, String email, String desc, String... links) {
        return new ClientData(id, first, last, email, desc, List.of(links));
    }

    private static final List<ClientData> CLIENTS = List.of(
            c("1", "John", "Doe", "john.doe@neviswealth.com", "Senior partner, loves ESG funds"),
            c("2", "Maria", "Garcia", "maria@acme.io", "Runs a family office in Madrid"),
            c("3", "Priya", "Shah", "priya.shah@gmail.com", null, "https://linkedin.com/in/priyashah"));

    private final ClientMatcher matcher = new ClientMatcher();

    private List<String> firstNames(String query) {
        return matcher.search(query, CLIENTS).stream().map(h -> h.client().firstName()).toList();
    }

    @Test
    void companyNameMatchesEmailDomain() {
        List<ClientHit> hits = matcher.search("NevisWealth", CLIENTS);
        assertEquals(1, hits.size());
        assertEquals("John", hits.get(0).client().firstName());
        assertEquals(List.of("email"), hits.get(0).matchedOn());
    }

    @Test
    void splitCompanyNameStillMatches() {
        assertEquals(List.of("John"), firstNames("nevis wealth"));
    }

    @Test
    void prefixMatches() {
        assertEquals(List.of("John"), firstNames("nevis"));
    }

    @Test
    void fullNameMatchRanksFirst() {
        List<ClientHit> hits = matcher.search("maria garcia", CLIENTS);
        assertEquals("Maria", hits.get(0).client().firstName());
        assertTrue(hits.get(0).score() > 0.9);
    }

    @Test
    void tokensCanMatchInDifferentFields() {
        assertEquals(List.of("John"), firstNames("john neviswealth"));
    }

    @Test
    void descriptionMatchScoresLowerThanNameMatch() {
        List<ClientHit> byDesc = matcher.search("madrid", CLIENTS);
        List<ClientHit> byName = matcher.search("maria", CLIENTS);
        assertEquals(List.of("Maria"), firstNames("madrid"));
        assertTrue(byDesc.get(0).score() < byName.get(0).score());
    }

    @Test
    void typoTolerance() {
        assertEquals(List.of(), firstNames("jhon")); // too short to fuzz safely
        assertEquals(List.of("Maria"), firstNames("garcai"));
    }

    @Test
    void socialLinksAreSearchable() {
        assertEquals(List.of("Priya"), firstNames("priyashah"));
    }

    @Test
    void partialMultiTokenMissIsRejected() {
        assertEquals(List.of(), firstNames("john smith"));
    }

    @Test
    void noMatch() {
        assertEquals(List.of(), firstNames("zzzzqqq"));
    }

    @Test
    void caseAndAccentInsensitive() {
        assertEquals(List.of("Maria"), firstNames("MARÍA"));
    }

    @Test
    void nullDescriptionIsSafe() {
        assertEquals("Priya", matcher.search("shah", CLIENTS).get(0).client().firstName());
    }

    @Test
    void emptyQueryReturnsNothing() {
        assertEquals(List.of(), firstNames("   "));
    }
}

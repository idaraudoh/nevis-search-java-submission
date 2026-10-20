package com.nevis.search.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DocumentRankerTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private final DocumentRanker ranker = new DocumentRanker();
    private final QueryExpander exp = new ConceptExpander();

    private static DocData d(int i, String title, String content) {
        return new DocData(String.valueOf(i), title, content, T0.plusSeconds(60L * i));
    }

    private static final List<DocData> DOCS = List.of(
            d(1, "Electricity bill March", "Utility bill from British Gas. Service address: 12 High Street, London."),
            d(2, "Passport copy", "UK passport, valid until 2031. Holder: John Doe."),
            d(3, "Proof of address letter", "This letter is proof of address for John Doe at 12 High Street."),
            d(4, "Quarterly portfolio review", "Equity allocation is 60 percent. Bond allocation is 40 percent."));

    private List<String> ids(String query) {
        return ranker.search(query, DOCS, exp).stream().map(h -> h.document().id()).toList();
    }

    @Test
    void addressProofFindsUtilityBill() {
        List<DocumentHit> hits = ranker.search("address proof", DOCS, exp);
        DocumentHit bill = hits.stream().filter(h -> h.document().id().equals("1")).findFirst().orElseThrow();
        assertTrue(bill.viaExpansion().contains("utility bill"));
    }

    @Test
    void literalMatchOutranksExpansionOnlyMatch() {
        assertEquals("3", ids("address proof").get(0));
    }

    @Test
    void wordOrderAndFillerWordsDoNotMatter() {
        assertEquals(ids("address proof"), ids("proof of address"));
    }

    @Test
    void unrelatedDocumentsAreExcluded() {
        List<String> ids = ids("address proof");
        assertFalse(ids.contains("4"));
        assertFalse(ids.contains("2"));
    }

    @Test
    void reverseDirectionUtilityBillFindsProofOfAddress() {
        assertTrue(ids("utility bill").contains("3"));
    }

    @Test
    void identityQueryFindsPassport() {
        assertTrue(ids("id proof").contains("2"));
    }

    @Test
    void pluralAndCaseInsensitive() {
        assertTrue(ids("UTILITY BILLS").contains("1"));
    }

    @Test
    void titleHitsCount() {
        List<DocData> docs = List.of(
                d(1, "Mortgage offer", "Terms attached."),
                d(2, "Notes", "Mortgage mentioned once."));
        assertEquals("1", ranker.search("mortgage", docs, exp).get(0).document().id());
    }

    @Test
    void scoresAreInOpenUnitIntervalAndSorted() {
        List<DocumentHit> hits = ranker.search("address proof", DOCS, exp);
        double prev = Double.MAX_VALUE;
        for (DocumentHit h : hits) {
            assertTrue(h.score() > 0 && h.score() < 1);
            assertTrue(h.score() <= prev);
            prev = h.score();
        }
    }

    @Test
    void snippetPicksTheRelevantSentence() {
        DocData doc = d(9, "Misc", "Intro text here. Utility bill from Thames Water attached. Closing remarks.");
        DocumentHit hit = ranker.search("address proof", List.of(doc), exp).get(0);
        assertEquals("Utility bill from Thames Water attached.", hit.snippet());
    }

    @Test
    void longSnippetsAreTruncated() {
        DocData doc = d(9, "Misc", "Utility bill " + "x".repeat(500) + ".");
        String snippet = ranker.search("utility bill", List.of(doc), exp).get(0).snippet();
        assertTrue(snippet.length() <= DocumentRanker.SNIPPET_MAX);
        assertTrue(snippet.endsWith("..."));
    }

    @Test
    void noDocuments() {
        assertEquals(List.of(), ranker.search("anything", List.of(), exp));
    }

    @Test
    void queryWithNoMatchAndNoExpansion() {
        assertEquals(List.of(), ids("zzzz"));
    }

    @Test
    void expanderThatEchoesTheQueryIsIgnored() {
        QueryExpander echo = q -> List.of(q);
        List<DocumentHit> hits = ranker.search("passport", DOCS, echo);
        assertEquals(List.of(), hits.get(0).viaExpansion());
    }

    @Test
    void tiesBreakByCreationTime() {
        List<DocData> docs = List.of(d(2, "Same", "mortgage"), d(1, "Same", "mortgage"));
        assertEquals(List.of("1", "2"),
                ranker.search("mortgage", docs, exp).stream().map(h -> h.document().id()).toList());
    }
}

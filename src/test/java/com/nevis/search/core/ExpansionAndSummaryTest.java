package com.nevis.search.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ExpansionAndSummaryTest {

    private static final LlmClient FAILING = (p, n) -> {
        throw new IOException("HTTP 500");
    };

    // ---- expansion --------------------------------------------------------

    @Test
    void conceptExpanderCoversAddressProof() {
        List<String> out = new ConceptExpander().expand("address proof");
        assertTrue(out.contains("utility bill"));
        assertTrue(out.contains("council tax"));
        assertFalse(out.contains("address proof"));
    }

    @Test
    void conceptExpanderNoMatchReturnsEmpty() {
        assertEquals(List.of(), new ConceptExpander().expand("quarterly review"));
    }

    @Test
    void llmExpanderParsesJsonEvenWithChatter() {
        LlmExpander e = new LlmExpander((p, n) -> "Sure! [\"gas bill\", \"rent receipt\"]");
        assertEquals(List.of("gas bill", "rent receipt"), e.expand("address proof"));
    }

    @Test
    void llmExpanderReturnsEmptyOnError() {
        assertEquals(List.of(), new LlmExpander(FAILING).expand("x"));
    }

    @Test
    void llmExpanderReturnsEmptyOnGarbage() {
        assertEquals(List.of(), new LlmExpander((p, n) -> "no json here").expand("x"));
    }

    @Test
    void llmExpanderCapsAtTenPhrases() {
        String reply = "[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\",\"h\",\"i\",\"j\",\"k\",\"l\"]";
        assertEquals(10, new LlmExpander((p, n) -> reply).expand("x").size());
    }

    @Test
    void llmExpanderCachesSuccessesButNotFailures() {
        AtomicInteger calls = new AtomicInteger();
        LlmExpander ok = new LlmExpander((p, n) -> {
            calls.incrementAndGet();
            return "[\"a b\"]";
        });
        ok.expand("Same Query");
        ok.expand("same query ");
        assertEquals(1, calls.get());

        AtomicInteger failCalls = new AtomicInteger();
        LlmExpander bad = new LlmExpander((p, n) -> {
            failCalls.incrementAndGet();
            throw new IOException("down");
        });
        bad.expand("q");
        bad.expand("q");
        assertEquals(2, failCalls.get());
    }

    @Test
    void hybridDedupesAndSurvivesLlmFailure() {
        QueryExpander hybrid = new HybridExpander(List.of(new ConceptExpander(), new LlmExpander(FAILING)));
        List<String> out = hybrid.expand("address proof");
        assertTrue(out.contains("utility bill"));
        assertEquals(out.size(), out.stream().distinct().count());
    }

    @Test
    void hybridMergesLlmPhrases() {
        QueryExpander hybrid = new HybridExpander(List.of(
                new ConceptExpander(),
                new LlmExpander((p, n) -> "[\"rent receipt\", \"utility bill\"]")));
        List<String> out = hybrid.expand("address proof");
        assertTrue(out.contains("rent receipt"));
        assertEquals(1, out.stream().filter("utility bill"::equals).count());
    }

    // ---- summaries --------------------------------------------------------

    @Test
    void extractiveShortDocReturnedWhole() {
        Summarizer.Result r = new ExtractiveSummarizer().summarize("T", "One sentence. Two sentences.");
        assertEquals("One sentence. Two sentences.", r.text());
        assertEquals("extractive", r.method());
    }

    @Test
    void extractiveSummaryIsBoundedAndOrdered() {
        String content = "The mortgage statement shows a balance of 200000 pounds. "
                + "Weather was nice. Lunch happened. "
                + "Mortgage payments are monthly and the mortgage rate is fixed. "
                + "Parking was hard. Another filler line. Yet another filler line.";
        String text = new ExtractiveSummarizer().text("Mortgage statement", content);
        assertTrue(text.length() <= 400);
        assertTrue(text.toLowerCase().contains("mortgage"));
        assertFalse(text.contains("Lunch happened"));
        assertTrue(text.indexOf("balance") < text.indexOf("monthly"));
    }

    @Test
    void extractiveSummaryClipsVeryLongSentences() {
        String text = new ExtractiveSummarizer().text("T", "x".repeat(1000) + ".");
        assertEquals(400, text.length());
        assertTrue(text.endsWith("..."));
    }

    @Test
    void extractiveSummaryEmptyContent() {
        assertEquals("", new ExtractiveSummarizer().text("T", "   "));
    }

    @Test
    void llmSummaryUsedWhenAvailable() {
        Summarizer s = new LlmSummarizer((p, n) -> "A utility bill for 12 High Street.\n", new ExtractiveSummarizer());
        Summarizer.Result r = s.summarize("T", "body");
        assertEquals("A utility bill for 12 High Street.", r.text());
        assertEquals("llm", r.method());
    }

    @Test
    void llmSummaryFallsBackOnFailure() {
        Summarizer s = new LlmSummarizer(FAILING, new ExtractiveSummarizer());
        Summarizer.Result r = s.summarize("T", "Short body.");
        assertEquals("Short body.", r.text());
        assertEquals("extractive", r.method());
    }

    @Test
    void llmSummaryFallsBackOnBlankReply() {
        Summarizer s = new LlmSummarizer((p, n) -> "   ", new ExtractiveSummarizer());
        assertEquals("extractive", s.summarize("T", "Short body.").method());
    }
}

package com.nevis.search.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextUtilTest {

    @Test
    void stemsPlurals() {
        assertEquals("bill", TextUtil.stem("bills"));
        assertEquals("utility", TextUtil.stem("utilities"));
        assertEquals("address", TextUtil.stem("address")); // double s is not a plural
        assertEquals("address", TextUtil.stem("addresses"));
        assertEquals("is", TextUtil.stem("is"));
    }

    @Test
    void tokenizeDropsStopwordsAndStems() {
        assertEquals(List.of("proof", "address"), TextUtil.tokenize("Proof of the Addresses"));
    }

    @Test
    void tokenizeKeepsStopwordOnlyQueries() {
        assertEquals(List.of("the", "of"), TextUtil.tokenize("the of"));
    }

    @Test
    void tokenizeStripsAccents() {
        assertEquals(List.of("cafe", "zoe"), TextUtil.tokenize("Café Zoë", true, false));
    }

    @Test
    void compactRemovesPunctuationAndSpaces() {
        assertEquals("neviswealth", TextUtil.compact("Nevis Wealth!"));
    }

    @Test
    void splitsSentences() {
        assertEquals(List.of("One.", "Two!", "Three"), TextUtil.splitSentences("One. Two!\nThree"));
    }

    @Test
    void similarityHandlesTranspositions() {
        assertTrue(TextUtil.similarity("garcai", "garcia") > 0.83);
        assertEquals(1.0, TextUtil.similarity("same", "same"));
        assertEquals(1.0, TextUtil.similarity("", ""));
    }
}

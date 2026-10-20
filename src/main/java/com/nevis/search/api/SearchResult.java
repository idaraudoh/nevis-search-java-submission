package com.nevis.search.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** One hit. Exactly one of {@code client} or {@code document} is set. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchResult(
        String type,
        double score,
        ClientResponse client,
        DocumentResponse document,
        /* document hits: whose document it is */ ClientSummary owner,
        /* client hits: which fields matched */ List<String> matchedOn,
        /* document hits: best sentence and short summary */ String snippet,
        String summary,
        /* document hits: query terms that hit, and phrases that hit only via expansion */
        List<String> matchedTerms,
        List<String> viaExpansion) {

    public static SearchResult ofClient(double score, ClientResponse client, List<String> matchedOn) {
        return new SearchResult("client", score, client, null, null, matchedOn, null, null, null, null);
    }

    public static SearchResult ofDocument(double score, DocumentResponse doc, ClientSummary owner, String snippet,
                                 String summary, List<String> matchedTerms, List<String> viaExpansion) {
        return new SearchResult("document", score, null, doc, owner, null, snippet, summary, matchedTerms, viaExpansion);
    }
}

package com.nevis.search.core;

import java.util.List;

public record DocumentHit(
        DocData document, double score, String snippet,
        List<String> matchedTerms, List<String> viaExpansion) {}

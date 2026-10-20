package com.nevis.search.core;

import java.util.ArrayList;
import java.util.List;

/** Runs every expander in order and merges the phrases without duplicates. */
public final class HybridExpander implements QueryExpander {

    private final List<QueryExpander> expanders;

    public HybridExpander(List<QueryExpander> expanders) {
        this.expanders = List.copyOf(expanders);
    }

    @Override
    public List<String> expand(String query) {
        List<String> out = new ArrayList<>();
        for (QueryExpander e : expanders) {
            for (String phrase : e.expand(query)) {
                if (!out.contains(phrase)) out.add(phrase);
            }
        }
        return out;
    }
}

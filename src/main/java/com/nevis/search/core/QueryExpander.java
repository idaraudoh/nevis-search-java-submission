package com.nevis.search.core;

import java.util.List;

/** Turns a query into related phrases, e.g. "address proof" -> "utility bill". */
public interface QueryExpander {
    List<String> expand(String query);
}

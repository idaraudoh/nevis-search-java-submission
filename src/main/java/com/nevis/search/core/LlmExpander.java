package com.nevis.search.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Solution B (optional): ask an LLM for related document phrases.
 * Any failure returns an empty list, so search never breaks because of it.
 */
public final class LlmExpander implements QueryExpander {

    private static final Logger log = LoggerFactory.getLogger(LlmExpander.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int CACHE_SIZE = 512;

    private final LlmClient llm;
    private final Map<String, List<String>> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<String>> eldest) {
                    return size() > CACHE_SIZE;
                }
            });

    public LlmExpander(LlmClient llm) {
        this.llm = llm;
    }

    @Override
    public List<String> expand(String query) {
        String key = query.strip().toLowerCase();
        List<String> cached = cache.get(key);
        if (cached != null) return cached;
        try {
            List<String> phrases = parse(llm.complete(prompt(key), 300));
            cache.put(key, phrases); // failures are not cached
            return phrases;
        } catch (Exception e) {
            log.warn("LLM expansion failed, falling back: {}", e.toString());
            return List.of();
        }
    }

    private static String prompt(String query) {
        return "A wealth advisor searches client documents with this query: '" + query + "'.\n"
                + "List up to 10 short phrases (1-3 words) that a relevant document might contain "
                + "instead of the query words. Example: query 'address proof' -> "
                + "['utility bill', 'council tax', 'bank statement'].\n"
                + "Reply with a JSON array of strings only.";
    }

    static List<String> parse(String text) throws Exception {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start < 0 || end < start) throw new IllegalArgumentException("no JSON array in LLM reply");
        List<Object> raw = JSON.readValue(text.substring(start, end + 1), new TypeReference<>() {});
        return raw.stream().map(o -> String.valueOf(o).strip()).filter(s -> !s.isEmpty()).limit(10).toList();
    }
}

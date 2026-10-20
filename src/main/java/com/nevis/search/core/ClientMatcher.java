package com.nevis.search.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Forgiving lexical matching over name, email, description and social links.
 *
 * <p>Per query token we take the best score across fields:
 * <pre>
 *   exact token   1.0
 *   prefix        0.8   ("nevis" -> "neviswealth")
 *   substring     0.7   inside the field with punctuation removed
 *   fuzzy         0.7 * similarity, only when similarity >= 0.8 (typos, swaps)
 * </pre>
 * then multiply by the field weight and average over query tokens.
 * Tokens can match in different fields, so "john neviswealth" works.
 */
public final class ClientMatcher {

    static final Map<String, Double> FIELD_WEIGHTS = new LinkedHashMap<>();
    static {
        FIELD_WEIGHTS.put("email", 1.0);
        FIELD_WEIGHTS.put("first_name", 1.0);
        FIELD_WEIGHTS.put("last_name", 1.0);
        FIELD_WEIGHTS.put("description", 0.6);
        FIELD_WEIGHTS.put("social_links", 0.5);
    }

    static final double MIN_SCORE = 0.55;
    static final int MIN_SUBSTRING_LEN = 4;
    static final int MIN_PREFIX_LEN = 3;
    static final int FUZZY_MIN_LEN = 5;
    static final double FUZZY_FLOOR = 0.80;

    public List<ClientHit> search(String query, List<ClientData> clients) {
        List<ClientHit> hits = new ArrayList<>();
        for (ClientData c : clients) {
            ClientHit hit = score(query, c);
            if (hit != null) hits.add(hit);
        }
        hits.sort(Comparator
                .comparingDouble(ClientHit::score).reversed()
                .thenComparing(h -> h.client().lastName().toLowerCase(Locale.ROOT))
                .thenComparing(h -> h.client().firstName().toLowerCase(Locale.ROOT)));
        return hits;
    }

    ClientHit score(String query, ClientData client) {
        List<String> qTokens = TextUtil.tokenize(query, true, false);
        String qCompact = TextUtil.compact(query);
        if (qTokens.isEmpty()) return null;

        Map<String, String> fields = fields(client);
        Map<String, List<String>> fieldTokens = new LinkedHashMap<>();
        Map<String, String> fieldCompact = new LinkedHashMap<>();
        fields.forEach((name, value) -> {
            fieldTokens.put(name, TextUtil.tokenize(value, false, false));
            fieldCompact.put(name, TextUtil.compact(value));
        });

        double sum = 0;
        Set<String> matched = new LinkedHashSet<>();
        for (String token : qTokens) {
            double best = 0;
            String bestField = null;
            for (Map.Entry<String, Double> e : FIELD_WEIGHTS.entrySet()) {
                String name = e.getKey();
                double s = tokenScore(token, fieldTokens.get(name), fieldCompact.get(name)) * e.getValue();
                if (s > best) {
                    best = s;
                    bestField = name;
                }
            }
            sum += best;
            if (bestField != null && best >= MIN_SCORE * 0.9) matched.add(bestField);
        }
        double score = sum / qTokens.size();

        // Whole query glued together: "nevis wealth" -> "neviswealth" inside the email.
        if (qCompact.length() >= MIN_SUBSTRING_LEN) {
            for (Map.Entry<String, Double> e : FIELD_WEIGHTS.entrySet()) {
                if (fieldCompact.get(e.getKey()).contains(qCompact)) {
                    score = Math.max(score, 0.9 * e.getValue());
                    matched.add(e.getKey());
                }
            }
        }

        if (score < MIN_SCORE) return null;
        List<String> ordered = FIELD_WEIGHTS.keySet().stream().filter(matched::contains).toList();
        return new ClientHit(client, Math.min(score, 1.0), ordered);
    }

    static double tokenScore(String token, List<String> fieldTokens, String fieldCompact) {
        if (fieldTokens.isEmpty()) return 0.0;
        if (fieldTokens.contains(token)) return 1.0;
        if (token.length() >= MIN_PREFIX_LEN && fieldTokens.stream().anyMatch(ft -> ft.startsWith(token))) {
            return 0.8;
        }
        if (token.length() >= MIN_SUBSTRING_LEN && fieldCompact.contains(token)) return 0.7;
        if (token.length() >= FUZZY_MIN_LEN) {
            double best = 0;
            for (String ft : fieldTokens) best = Math.max(best, TextUtil.similarity(token, ft));
            if (best >= FUZZY_FLOOR) return 0.7 * best;
        }
        return 0.0;
    }

    private static Map<String, String> fields(ClientData c) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("email", nz(c.email()));
        m.put("first_name", nz(c.firstName()));
        m.put("last_name", nz(c.lastName()));
        m.put("description", nz(c.description()));
        m.put("social_links", c.socialLinks() == null ? "" : String.join(" ", c.socialLinks()));
        return m;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}

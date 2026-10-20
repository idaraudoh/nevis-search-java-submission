package com.nevis.search.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BM25 on the query, plus a discounted BM25 on expanded phrases.
 *
 * <p>Let's say the query is "address proof" and a document says "utility bill".
 * BM25 alone scores 0. The expander adds "utility bill", the phrase matches, and the
 * document lands in the results at a discount (EXPANSION_WEIGHT). A document that
 * literally says "proof of address" still ranks higher.
 */
public final class DocumentRanker {

    static final double K1 = 1.5;
    static final double B = 0.75;
    static final int TITLE_BOOST = 2; // title tokens are counted this many times
    static final double EXPANSION_WEIGHT = 0.6;
    static final int SNIPPET_MAX = 240;

    private record Indexed(DocData doc, List<String> tokens, Map<String, Integer> tf) {}

    private record Expansion(String phrase, List<String> tokens) {}

    public List<DocumentHit> search(String query, List<DocData> documents, QueryExpander expander) {
        List<String> qTokens = new ArrayList<>(new LinkedHashSet<>(TextUtil.tokenize(query)));
        if (qTokens.isEmpty() || documents.isEmpty()) return List.of();

        Set<String> qSet = new LinkedHashSet<>(qTokens);
        List<Expansion> expansions = new ArrayList<>();
        for (String phrase : expander.expand(query)) {
            List<String> toks = TextUtil.tokenize(phrase);
            if (!toks.isEmpty() && !new LinkedHashSet<>(toks).equals(qSet)) {
                expansions.add(new Expansion(phrase, toks));
            }
        }

        List<Indexed> index = new ArrayList<>();
        for (DocData d : documents) {
            List<String> title = TextUtil.tokenize(d.title());
            List<String> body = TextUtil.tokenize(d.content());
            List<String> ordered = new ArrayList<>(title);
            ordered.addAll(body);
            Map<String, Integer> tf = new HashMap<>();
            for (int i = 0; i < TITLE_BOOST; i++) title.forEach(t -> tf.merge(t, 1, Integer::sum));
            body.forEach(t -> tf.merge(t, 1, Integer::sum));
            index.add(new Indexed(d, ordered, tf));
        }

        int nDocs = index.size();
        double avgdl = index.stream().mapToInt(i -> i.tokens().size()).average().orElse(1.0);

        Map<String, Double> qIdf = new HashMap<>();
        for (String t : qTokens) {
            long df = index.stream().filter(i -> i.tf().containsKey(t)).count();
            qIdf.put(t, idf(nDocs, df));
        }
        Map<String, Double> expIdf = new HashMap<>();
        for (Expansion e : expansions) {
            long df = index.stream().filter(i -> countPhrase(i.tokens(), e.tokens()) > 0).count();
            expIdf.put(e.phrase(), idf(nDocs, df));
        }

        List<DocumentHit> hits = new ArrayList<>();
        for (Indexed item : index) {
            int dl = item.tokens().size();
            double lexical = 0;
            List<String> matched = new ArrayList<>();
            for (String t : qTokens) {
                int tf = item.tf().getOrDefault(t, 0);
                if (tf > 0) {
                    lexical += bm25(tf, qIdf.get(t), dl, avgdl);
                    matched.add(t);
                }
            }

            double semantic = 0;
            List<String> via = new ArrayList<>();
            List<List<String>> viaTokens = new ArrayList<>();
            for (Expansion e : expansions) {
                int tf = countPhrase(item.tokens(), e.tokens());
                if (tf > 0) {
                    semantic += bm25(tf, expIdf.get(e.phrase()), dl, avgdl);
                    via.add(e.phrase());
                    viaTokens.add(e.tokens());
                }
            }

            double total = lexical + EXPANSION_WEIGHT * semantic;
            if (total <= 0) continue;

            List<List<String>> needles = new ArrayList<>();
            matched.forEach(t -> needles.add(List.of(t)));
            needles.addAll(viaTokens);
            hits.add(new DocumentHit(
                    item.doc(),
                    total / (total + 2.0), // squash into (0, 1)
                    snippet(item.doc().content(), needles),
                    matched,
                    via));
        }

        hits.sort(Comparator.comparingDouble(DocumentHit::score).reversed()
                .thenComparing(h -> h.document().createdAt()));
        return hits;
    }

    static int countPhrase(List<String> tokens, List<String> phrase) {
        int n = phrase.size();
        if (n == 0 || tokens.size() < n) return 0;
        if (n == 1) return Collections.frequency(tokens, phrase.get(0));
        int count = 0;
        for (int i = 0; i <= tokens.size() - n; i++) {
            if (tokens.subList(i, i + n).equals(phrase)) count++;
        }
        return count;
    }

    private static double idf(int nDocs, long df) {
        return Math.log(1 + (nDocs - df + 0.5) / (df + 0.5));
    }

    private static double bm25(int tf, double idf, int dl, double avgdl) {
        if (tf == 0) return 0.0;
        return idf * (tf * (K1 + 1)) / (tf + K1 * (1 - B + B * dl / Math.max(avgdl, 1.0)));
    }

    static String snippet(String content, List<List<String>> needles) {
        List<String> sentences = TextUtil.splitSentences(content);
        if (sentences.isEmpty()) sentences = List.of(content);
        String best = sentences.get(0);
        int bestHits = -1;
        for (String sentence : sentences) {
            List<String> toks = TextUtil.tokenize(sentence, false, true);
            int hits = 0;
            for (List<String> n : needles) if (countPhrase(toks, n) > 0) hits++;
            if (hits > bestHits) {
                best = sentence;
                bestHits = hits;
            }
        }
        return best.length() <= SNIPPET_MAX ? best : best.substring(0, SNIPPET_MAX - 3).stripTrailing() + "...";
    }
}

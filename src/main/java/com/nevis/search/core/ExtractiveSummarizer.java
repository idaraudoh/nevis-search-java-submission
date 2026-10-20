package com.nevis.search.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Solution A: pick the most informative sentences. No network, no cost. */
public final class ExtractiveSummarizer implements Summarizer {

    static final int MAX_SENTENCES = 3;
    static final int MAX_CHARS = 400;

    @Override
    public Result summarize(String title, String content) {
        return new Result(text(title, content), "extractive");
    }

    public String text(String title, String content) {
        List<String> sentences = TextUtil.splitSentences(content);
        if (sentences.isEmpty()) return "";
        if (sentences.size() <= MAX_SENTENCES) return clip(String.join(" ", sentences));

        Map<String, Integer> freq = new HashMap<>();
        for (String s : sentences) TextUtil.tokenize(s).forEach(t -> freq.merge(t, 1, Integer::sum));
        Set<String> titleTerms = new HashSet<>(TextUtil.tokenize(title));

        record Scored(double score, int pos, String sentence) {}
        List<Scored> scored = new ArrayList<>();
        for (int pos = 0; pos < sentences.size(); pos++) {
            List<String> toks = TextUtil.tokenize(sentences.get(pos));
            if (toks.isEmpty()) continue;
            double sum = 0;
            for (String t : toks) sum += freq.get(t) + (titleTerms.contains(t) ? 2 : 0);
            double score = sum / toks.size();
            if (pos == 0) score *= 1.15; // openers usually frame the document
            scored.add(new Scored(score, pos, sentences.get(pos)));
        }
        List<Scored> top = new ArrayList<>(scored.stream()
                .sorted(Comparator.comparingDouble(Scored::score).reversed().thenComparingInt(Scored::pos))
                .limit(MAX_SENTENCES).toList());
        top.sort(Comparator.comparingInt(Scored::pos)); // restore reading order
        return clip(String.join(" ", top.stream().map(Scored::sentence).toList()));
    }

    private static String clip(String text) {
        return text.length() <= MAX_CHARS ? text : text.substring(0, MAX_CHARS - 3).stripTrailing() + "...";
    }
}

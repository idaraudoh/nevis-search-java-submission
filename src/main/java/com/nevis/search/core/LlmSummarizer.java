package com.nevis.search.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Solution B (optional): the LLM writes the summary. Falls back to Solution A on any failure. */
public final class LlmSummarizer implements Summarizer {

    private static final Logger log = LoggerFactory.getLogger(LlmSummarizer.class);

    private final LlmClient llm;
    private final Summarizer fallback;

    public LlmSummarizer(LlmClient llm, Summarizer fallback) {
        this.llm = llm;
        this.fallback = fallback;
    }

    @Override
    public Result summarize(String title, String content) {
        try {
            String body = content.length() > 8000 ? content.substring(0, 8000) : content;
            String text = llm.complete(
                    "Summarise this client document in 1-2 plain sentences for a wealth advisor. "
                            + "Mention document type and key facts. Do not invent details.\n\n"
                            + "Title: " + title + "\n\n" + body,
                    200).strip();
            if (text.isEmpty()) throw new IllegalStateException("empty summary");
            return new Result(text, "llm");
        } catch (Exception e) {
            log.warn("LLM summary failed, falling back: {}", e.toString());
            return fallback.summarize(title, content);
        }
    }
}

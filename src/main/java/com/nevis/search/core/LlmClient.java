package com.nevis.search.core;

/** One-shot text completion. Lets tests swap the real API for a lambda. */
@FunctionalInterface
public interface LlmClient {
    String complete(String prompt, int maxTokens) throws Exception;
}

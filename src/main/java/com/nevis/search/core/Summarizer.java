package com.nevis.search.core;

public interface Summarizer {

    record Result(String text, String method) {}

    Result summarize(String title, String content);
}

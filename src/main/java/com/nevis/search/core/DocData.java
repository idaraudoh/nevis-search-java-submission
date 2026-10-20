package com.nevis.search.core;

import java.time.Instant;

public record DocData(String id, String title, String content, Instant createdAt) {}

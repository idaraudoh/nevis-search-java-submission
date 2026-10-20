package com.nevis.search.core;

import java.util.List;

public record ClientHit(ClientData client, double score, List<String> matchedOn) {}

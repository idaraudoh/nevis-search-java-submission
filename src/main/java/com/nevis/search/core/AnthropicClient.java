package com.nevis.search.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Minimal Anthropic Messages API client built on the JDK HTTP client. */
public final class AnthropicClient implements LlmClient {

    private static final URI API_URL = URI.create("https://api.anthropic.com/v1/messages");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String apiKey;
    private final String model;
    private final Duration timeout;
    private final HttpClient http;

    public AnthropicClient(String apiKey, String model, Duration timeout) {
        this.apiKey = apiKey;
        this.model = model;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public String complete(String prompt, int maxTokens) throws IOException, InterruptedException {
        ObjectNode body = JSON.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "user").put("content", prompt);

        HttpRequest request = HttpRequest.newBuilder(API_URL)
                .timeout(timeout)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Anthropic API returned HTTP " + response.statusCode());
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode block : JSON.readTree(response.body()).path("content")) {
            text.append(block.path("text").asText(""));
        }
        return text.toString();
    }
}

package com.nevis.search.config;

import com.nevis.search.core.AnthropicClient;
import com.nevis.search.core.ConceptExpander;
import com.nevis.search.core.ExtractiveSummarizer;
import com.nevis.search.core.HybridExpander;
import com.nevis.search.core.LlmExpander;
import com.nevis.search.core.LlmSummarizer;
import com.nevis.search.core.QueryExpander;
import com.nevis.search.core.Summarizer;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires Solution A (offline) by default and adds Solution B (LLM) when an API key is set.
 */
@Configuration
public class SearchConfig {

    @Bean
    AnthropicClient anthropicClient(LlmProperties props) {
        // Only used when enabled; cheap to build either way.
        return new AnthropicClient(
                props.apiKey() == null ? "" : props.apiKey(),
                props.model() == null ? "claude-sonnet-5-5" : props.model(),
                props.timeout() == null ? Duration.ofSeconds(8) : props.timeout());
    }

    @Bean
    QueryExpander queryExpander(LlmProperties props, AnthropicClient llm) {
        ConceptExpander concepts = new ConceptExpander();
        return props.enabled() ? new HybridExpander(List.of(concepts, new LlmExpander(llm))) : concepts;
    }

    @Bean
    Summarizer summarizer(LlmProperties props, AnthropicClient llm) {
        ExtractiveSummarizer extractive = new ExtractiveSummarizer();
        return props.enabled() ? new LlmSummarizer(llm, extractive) : extractive;
    }
}

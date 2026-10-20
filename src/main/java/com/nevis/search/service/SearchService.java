package com.nevis.search.service;

import com.nevis.search.api.ClientResponse;
import com.nevis.search.api.ClientSummary;
import com.nevis.search.api.DocumentResponse;
import com.nevis.search.api.SearchResult;
import com.nevis.search.core.ClientData;
import com.nevis.search.core.ClientMatcher;
import com.nevis.search.core.DocData;
import com.nevis.search.core.DocumentHit;
import com.nevis.search.core.DocumentRanker;
import com.nevis.search.core.ExtractiveSummarizer;
import com.nevis.search.core.QueryExpander;
import com.nevis.search.domain.ClientEntity;
import com.nevis.search.domain.ClientRepository;
import com.nevis.search.domain.DocumentEntity;
import com.nevis.search.domain.DocumentRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SearchService {

    public enum Kind { ALL, CLIENT, DOCUMENT }

    private final ClientRepository clients;
    private final DocumentRepository documents;
    private final QueryExpander expander;
    private final ClientMatcher clientMatcher = new ClientMatcher();
    private final DocumentRanker documentRanker = new DocumentRanker();
    // Search results always use the cheap extractive summary, never one LLM call per hit.
    private final ExtractiveSummarizer quickSummary = new ExtractiveSummarizer();

    public SearchService(ClientRepository clients, DocumentRepository documents, QueryExpander expander) {
        this.clients = clients;
        this.documents = documents;
        this.expander = expander;
    }

    /**
     * @param clientId when set, only that client and that client's documents are returned
     *                 (404 if the client does not exist)
     */
    @Transactional(readOnly = true)
    public List<SearchResult> search(String query, Kind kind, int limit, String clientId) {
        ClientEntity scope = clientId == null ? null : clients.findById(clientId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
        List<SearchResult> results = new ArrayList<>();

        if (kind == Kind.ALL || kind == Kind.CLIENT) {
            List<ClientEntity> pool = scope == null ? clients.findAll() : List.of(scope);
            Map<String, ClientEntity> byId = pool.stream()
                    .collect(Collectors.toMap(ClientEntity::getId, Function.identity()));
            List<ClientData> data = byId.values().stream()
                    .map(c -> new ClientData(c.getId(), c.getFirstName(), c.getLastName(), c.getEmail(),
                            c.getDescription(), c.getSocialLinks()))
                    .toList();
            clientMatcher.search(query, data).forEach(hit -> results.add(SearchResult.ofClient(
                    round(hit.score()), ClientResponse.from(byId.get(hit.client().id())), hit.matchedOn())));
        }

        if (kind == Kind.ALL || kind == Kind.DOCUMENT) {
            Map<String, DocumentEntity> byId = documents.findAll().stream()
                    .collect(Collectors.toMap(DocumentEntity::getId, Function.identity()));
            List<DocData> data = byId.values().stream()
                    .map(d -> new DocData(d.getId(), d.getTitle(), d.getContent(), d.getCreatedAt()))
                    .toList();
            // Rank everything, then filter. A document scores the same with or without client_id,
            // because the BM25 statistics always come from the whole corpus.
            List<DocumentHit> hits = documentRanker.search(query, data, expander).stream()
                    .filter(h -> scope == null || scope.getId().equals(byId.get(h.document().id()).getClientId()))
                    .toList();
            Map<String, ClientEntity> owners = scope != null
                    ? Map.of(scope.getId(), scope)
                    : clients.findAllById(hits.stream().map(h -> byId.get(h.document().id()).getClientId())
                                    .distinct().toList()).stream()
                            .collect(Collectors.toMap(ClientEntity::getId, Function.identity()));
            for (DocumentHit hit : hits) {
                DocumentEntity d = byId.get(hit.document().id());
                results.add(SearchResult.ofDocument(
                        round(hit.score()), DocumentResponse.from(d), ClientSummary.from(owners.get(d.getClientId())),
                        hit.snippet(), quickSummary.text(d.getTitle(), d.getContent()),
                        hit.matchedTerms(), hit.viaExpansion()));
            }
        }

        results.sort(Comparator.comparingDouble(SearchResult::score).reversed()
                .thenComparing(SearchResult::type));
        return results.size() > limit ? results.subList(0, limit) : results;
    }

    private static double round(double v) {
        return Math.round(v * 10_000d) / 10_000d;
    }
}

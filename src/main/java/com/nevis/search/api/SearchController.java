package com.nevis.search.api;

import com.nevis.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Tag(name = "search")
public class SearchController {

    private static final int MAX_QUERY_LENGTH = 500;

    private final SearchService service;

    public SearchController(SearchService service) {
        this.service = service;
    }

    @Operation(summary = "Search clients and documents",
            description = "Results are sorted by score (0 to 1). Document hits include the owning client. "
                    + "Pass client_id to search one client's file only. "
                    + "422 if q is missing or blank, or type/limit/client_id is invalid. 404 if client_id is unknown.")
    @GetMapping("/search")
    public List<SearchResult> search(
            @Parameter(description = "Search text", required = true) @RequestParam("q") String q,
            @Parameter(description = "all, client or document") @RequestParam(name = "type", defaultValue = "all") String type,
            @Parameter(description = "1 to 100") @RequestParam(name = "limit", defaultValue = "20") int limit,
            @Parameter(description = "Only search this client and their documents. 404 if the client does not exist")
            @RequestParam(name = "client_id", required = false) String clientId) {

        String query = q.strip();
        if (query.isEmpty()) throw unprocessable("q must not be blank");
        if (query.length() > MAX_QUERY_LENGTH) throw unprocessable("q must be at most " + MAX_QUERY_LENGTH + " characters");
        if (limit < 1 || limit > 100) throw unprocessable("limit must be between 1 and 100");

        if (clientId != null && clientId.isBlank()) throw unprocessable("client_id must not be blank");

        SearchService.Kind kind;
        try {
            kind = SearchService.Kind.valueOf(type.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw unprocessable("type must be one of: all, client, document");
        }
        return service.search(query, kind, limit, clientId == null ? null : clientId.strip());
    }

    private static ResponseStatusException unprocessable(String message) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}

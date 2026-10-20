package com.nevis.search.api;

import com.nevis.search.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @Operation(summary = "Add a document to a client", description = "201 on success, 404 if the client does not exist, 422 if the body is invalid.")
    @PostMapping("/clients/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse create(@PathVariable String id, @Valid @RequestBody CreateDocumentRequest request) {
        return DocumentResponse.from(service.create(id, request));
    }

    @Operation(summary = "Get a document")
    @GetMapping("/documents/{id}")
    public DocumentResponse get(@PathVariable String id) {
        return DocumentResponse.from(service.get(id));
    }

    @Operation(summary = "Quick summary of a document",
            description = "`method` is `llm` when the LLM wrote it, `extractive` otherwise (also the fallback).")
    @GetMapping("/documents/{id}/summary")
    public DocumentSummaryResponse summary(@PathVariable String id) {
        return service.summary(id);
    }
}

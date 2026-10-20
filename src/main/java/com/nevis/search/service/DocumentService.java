package com.nevis.search.service;

import com.nevis.search.api.CreateDocumentRequest;
import com.nevis.search.api.DocumentSummaryResponse;
import com.nevis.search.core.Summarizer;
import com.nevis.search.domain.ClientRepository;
import com.nevis.search.domain.DocumentEntity;
import com.nevis.search.domain.DocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentService {

    private final ClientRepository clients;
    private final DocumentRepository documents;
    private final Summarizer summarizer;

    public DocumentService(ClientRepository clients, DocumentRepository documents, Summarizer summarizer) {
        this.clients = clients;
        this.documents = documents;
        this.summarizer = summarizer;
    }

    public DocumentEntity create(String clientId, CreateDocumentRequest req) {
        if (!clients.existsById(clientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }
        return documents.save(new DocumentEntity(clientId, req.title().strip(), req.content().strip()));
    }

    public DocumentEntity get(String id) {
        return documents.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    public DocumentSummaryResponse summary(String id) {
        DocumentEntity doc = get(id);
        Summarizer.Result r = summarizer.summarize(doc.getTitle(), doc.getContent());
        return new DocumentSummaryResponse(doc.getId(), r.text(), r.method());
    }
}

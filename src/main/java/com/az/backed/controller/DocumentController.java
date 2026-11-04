
package com.az.backed.controller;

import com.az.backed.entity.Document;
import com.az.backed.repository.DocumentRepository;
import com.az.backed.service.PythonAiService;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final PythonAiService pythonAiService;
    private final DocumentRepository documentRepository;

    public DocumentController(
            PythonAiService pythonAiService,
            DocumentRepository documentRepository) {

        this.pythonAiService = pythonAiService;
        this.documentRepository = documentRepository;
    }

    @PostMapping("/upload")
    public Map<String, Object> upload(
            @RequestParam("file") MultipartFile file) {

        //  Validate PDF
        if (file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File cannot be empty"
            );
        }

        if (file.getOriginalFilename() == null ||
                !file.getOriginalFilename()
                        .toLowerCase()
                        .endsWith(".pdf")) {

            throw new IllegalArgumentException(
                    "Only PDF files are supported"
            );
        }

        //  Creating and saving document in MySQL first
        Document document = new Document();

        document.setFilename(file.getOriginalFilename());

        Document savedDocument =
                documentRepository.save(document);

        // MySQL-generated UUID
        String documentId = savedDocument.getId();

        try {

            //  Sendpng PDF and the same UUID to Python
            Map<?, ?> aiResponse =
                    pythonAiService.uploadDocument(file, documentId);

            //  Update metadata using Python response
            savedDocument.setFilename(
                    String.valueOf(aiResponse.get("filename"))
            );

            savedDocument.setPagesProcessed(
                    ((Number) aiResponse.get("pages_processed"))
                            .intValue()
            );

            savedDocument.setChunksStored(
                    ((Number) aiResponse.get("chunks_stored"))
                            .intValue()
            );

            savedDocument.setVectorDatabase(
                    String.valueOf(aiResponse.get("vector_database"))
            );

            documentRepository.save(savedDocument);

            //  Return upload response
            Map<String, Object> response = new LinkedHashMap<>();

            aiResponse.forEach((key, value) ->
                    response.put(String.valueOf(key), value)
            );

            response.put("document_id", documentId);
            response.put("database_status", "SAVED");

            return response;

        } catch (RuntimeException e) {

            // Remove incomplete MySQL record if Python upload fails
            documentRepository.delete(savedDocument);

            throw new RuntimeException(
                    "Document processing failed: " + e.getMessage(),
                    e
            );
        }
    }

    // Retrieve all documents from MySQL
    @GetMapping
    public List<Document> getAllDocuments() {

        return documentRepository.findAll();
    }

    // Deleting document from FAISS and MySQL
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteDocument(@PathVariable String id) {

        // 1. Check whether document exists in MySQL
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found"
                ));

        // 2. Delete chunks and vectors from Python FAISS first
        Map<?, ?> pythonResponse =
                pythonAiService.deleteDocument(id);

        // 3. Delete metadata from MySQL only after Python succeeds
        documentRepository.delete(document);

        // 4. Return response
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("message", "Document deleted successfully");
        response.put("document_id", id);
        response.put("filename", document.getFilename());
        response.put("python_status", pythonResponse);

        return response;
    }

}

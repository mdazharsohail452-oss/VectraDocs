
package com.az.backed.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class PythonAiService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PythonAiService() {
        this.objectMapper = new ObjectMapper();

        this.restClient = RestClient.builder()
                .baseUrl("http://127.0.0.1:8000")
                .build();
    }

    // Check Python service health
    public Map<?, ?> checkPythonHealth() {

        return restClient.get()
                .uri("/health")
                .retrieve()
                .body(Map.class);
    }

    // Send text to Python summarization API
    public Map<?, ?> summarizeText(String text) {

        HttpURLConnection connection = null;

        try {
            String json = objectMapper.writeValueAsString(
                    Map.of("text", text)
            );

            byte[] requestBody =
                    json.getBytes(StandardCharsets.UTF_8);

            System.out.println("JSON SENT FROM JAVA: " + json);
            System.out.println("JSON LENGTH: " + requestBody.length);

            connection = (HttpURLConnection)
                    URI.create("http://127.0.0.1:8000/summarize")
                            .toURL()
                            .openConnection();

            connection.setRequestMethod("POST");

            connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(requestBody.length);

            connection.setConnectTimeout(10000);
            connection.setReadTimeout(120000);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(requestBody);
                output.flush();
            }

            int status = connection.getResponseCode();

            InputStream responseStream =
                    status >= 200 && status < 300
                            ? connection.getInputStream()
                            : connection.getErrorStream();

            if (responseStream == null) {
                throw new RuntimeException(
                        "Python returned an empty response."
                );
            }

            String responseBody;

            try (InputStream input = responseStream) {
                responseBody = new String(
                        input.readAllBytes(),
                        StandardCharsets.UTF_8
                );
            }

            System.out.println("PYTHON STATUS: " + status);
            System.out.println("PYTHON RESPONSE: " + responseBody);

            if (status < 200 || status >= 300) {
                throw new RuntimeException(
                        "Python API error: " + responseBody
                );
            }

            return objectMapper.readValue(
                    responseBody,
                    Map.class
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Python API request failed",
                    e
            );
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // Upload PDF to Python with its MySQL document ID
    public Map<?, ?> uploadDocument(
            MultipartFile file,
            String documentId) {

        String boundary = "----JavaBoundary" + System.nanoTime();
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection)
                    URI.create("http://127.0.0.1:8000/rag/upload")
                            .toURL()
                            .openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            connection.setConnectTimeout(10000);
            connection.setReadTimeout(120000);

            connection.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data; boundary=" + boundary
            );

            ByteArrayOutputStream body = new ByteArrayOutputStream();

            // Part 1: document_id
            body.write(("--" + boundary + "\r\n")
                    .getBytes(StandardCharsets.UTF_8));

            body.write((
                    "Content-Disposition: form-data; name=\"document_id\"\r\n\r\n"
            ).getBytes(StandardCharsets.UTF_8));

            body.write(documentId.getBytes(StandardCharsets.UTF_8));

            body.write("\r\n".getBytes(StandardCharsets.UTF_8));

            // Part 2: PDF file
            body.write(("--" + boundary + "\r\n")
                    .getBytes(StandardCharsets.UTF_8));

            String filename = file.getOriginalFilename() != null
                    ? file.getOriginalFilename().replace("\"", "")
                    : "document.pdf";

            body.write((
                    "Content-Disposition: form-data; name=\"file\"; filename=\""
                            + filename + "\"\r\n"
            ).getBytes(StandardCharsets.UTF_8));

            body.write((
                    "Content-Type: application/pdf\r\n\r\n"
            ).getBytes(StandardCharsets.UTF_8));

            body.write(file.getBytes());

            body.write(("\r\n--" + boundary + "--\r\n")
                    .getBytes(StandardCharsets.UTF_8));

            byte[] requestBody = body.toByteArray();

            connection.setFixedLengthStreamingMode(requestBody.length);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(requestBody);
                output.flush();
            }

            return readResponse(connection);

        } catch (Exception e) {
            throw new RuntimeException("Document upload failed", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // Ask a question about a specific document
    public Map<?, ?> askQuestion(
            String question,
            String documentId,
            int topK) {

        HttpURLConnection connection = null;

        try {
            String json = objectMapper.writeValueAsString(
                    Map.of(
                            "question", question,
                            "document_id", documentId,
                            "top_k", topK
                    )
            );

            connection = (HttpURLConnection)
                    URI.create("http://127.0.0.1:8000/rag/ask")
                            .toURL()
                            .openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            connection.setConnectTimeout(10000);
            connection.setReadTimeout(120000);

            connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
            );

            byte[] requestBody =
                    json.getBytes(StandardCharsets.UTF_8);

            connection.setFixedLengthStreamingMode(requestBody.length);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(requestBody);
                output.flush();
            }

            return readResponse(connection);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Question answering failed",
                    e
            );
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // Get RAG system status
    public Map<?, ?> getRagStatus() {

        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection)
                    URI.create("http://127.0.0.1:8000/rag/status")
                            .toURL()
                            .openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            return readResponse(connection);

        } catch (Exception e) {
            throw new RuntimeException(
                    "RAG status request failed",
                    e
            );
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // Delete document and its vectors from Python FAISS
    public Map<?, ?> deleteDocument(String documentId) {

        HttpURLConnection connection = null;

        try {
            String encodedId = java.net.URLEncoder.encode(
                    documentId,
                    StandardCharsets.UTF_8
            );

            connection = (HttpURLConnection)
                    URI.create("http://127.0.0.1:8000/rag/documents/" + encodedId)
                            .toURL()
                            .openConnection();

            connection.setRequestMethod("DELETE");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(120000);

            return readResponse(connection);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to delete document from Python FAISS",
                    e
            );
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // Common Python response handler
    private Map<?, ?> readResponse(HttpURLConnection connection)
            throws Exception {

        int status = connection.getResponseCode();

        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();

        if (stream == null) {
            throw new RuntimeException(
                    "Python returned an empty response. HTTP status: " + status
            );
        }

        String response;

        try (InputStream input = stream) {
            response = new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }

        if (status < 200 || status >= 300) {
            throw new RuntimeException(
                    "Python API returned " + status + ": " + response
            );
        }

        return objectMapper.readValue(response, Map.class);
    }
}

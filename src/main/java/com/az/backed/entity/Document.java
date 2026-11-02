package com.az.backed.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    private String id;

    @Column(nullable = false)
    private String filename;

    private int pagesProcessed;

    private int chunksStored;

    private String vectorDatabase;

    private LocalDateTime uploadedAt;

    @PrePersist
    public void generateId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }

        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }

    public String getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getPagesProcessed() {
        return pagesProcessed;
    }

    public void setPagesProcessed(int pagesProcessed) {
        this.pagesProcessed = pagesProcessed;
    }

    public int getChunksStored() {
        return chunksStored;
    }

    public void setChunksStored(int chunksStored) {
        this.chunksStored = chunksStored;
    }

    public String getVectorDatabase() {
        return vectorDatabase;
    }

    public void setVectorDatabase(String vectorDatabase) {
        this.vectorDatabase = vectorDatabase;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
}
package com.az.backed.repository;

import com.az.backed.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository
        extends JpaRepository<Document, String> {
}

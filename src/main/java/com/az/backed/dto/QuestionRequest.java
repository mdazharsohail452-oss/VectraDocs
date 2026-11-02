
package com.az.backed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public class QuestionRequest {

    @NotBlank(message = "Question cannot be empty")
    private String question;

    @NotBlank(message = "Document ID is required")
    private String document_id;

    @Min(1)
    @Max(10)
    private int top_k = 3;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getDocument_id() {
        return document_id;
    }

    public void setDocument_id(String document_id) {
        this.document_id = document_id;
    }

    public int getTop_k() {
        return top_k;
    }

    public void setTop_k(int top_k) {
        this.top_k = top_k;
    }
}


package com.az.backed.controller;

import com.az.backed.dto.QuestionRequest;
import com.az.backed.service.PythonAiService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final PythonAiService pythonAiService;

    public ChatController(PythonAiService pythonAiService) {
        this.pythonAiService = pythonAiService;
    }

    @PostMapping("/ask")
    public Map<?, ?> ask(@Valid @RequestBody QuestionRequest request) {

        return pythonAiService.askQuestion(
                request.getQuestion(),
                request.getDocument_id(),
                request.getTop_k()
        );
    }
}

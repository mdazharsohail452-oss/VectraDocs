
package com.az.backed.controller;

import com.az.backed.dto.SummarizeRequest;
import com.az.backed.service.PythonAiService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class PythonHealthController {

    private final PythonAiService pythonAiService;

    public PythonHealthController(PythonAiService pythonAiService) {
        this.pythonAiService = pythonAiService;
    }

    @GetMapping("/python-health")
    public Map<?, ?> pythonHealth() {
        return pythonAiService.checkPythonHealth();
    }

    @PostMapping("/summarize")
    public Map<?, ?> summarize(@RequestBody SummarizeRequest request) {

        System.out.println("1. CONTROLLER REACHED");
        System.out.println("2. INPUT: " + request.getText());

        try {
            Map<?, ?> result =
                    pythonAiService.summarizeText(request.getText());

            System.out.println("3. FASTAPI RESPONSE: " + result);

            return result;

        } catch (Exception e) {
            System.err.println("FASTAPI CALL FAILED:");
            e.printStackTrace();
            throw e;
        }
    }
}

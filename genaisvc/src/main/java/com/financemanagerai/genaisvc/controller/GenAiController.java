package com.financemanagerai.genaisvc.controller;

import com.financemanagerai.genaisvc.dto.BulkCategorizationRequestDTO;
import com.financemanagerai.genaisvc.service.GeminiService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/genai")
public class GenAiController {

    private final GeminiService geminiService;

    public GenAiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/query")
    public Map<String, String> handleQuery(@RequestBody Map<String, String> request) {
        String userQuery = request.get("query");
        String aiResponse = geminiService.getCompletion(userQuery);

        return Map.of(
                "query", userQuery,
                "response", aiResponse
        );
    }

    @PostMapping("/categorize-batch")
    public Map<String, String> categorizeBatch(@RequestBody BulkCategorizationRequestDTO request) {
        String prompt = request.buildPrompt();
        String categorization = geminiService.getCompletion(prompt);

        return Map.of(
                "transactionCount", String.valueOf(request.getTransactions().size()),
                "categorization", categorization
        );
    }
}
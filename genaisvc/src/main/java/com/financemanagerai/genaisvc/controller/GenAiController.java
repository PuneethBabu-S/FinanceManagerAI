package com.financemanagerai.genaisvc.controller;

import com.financemanagerai.genaisvc.dto.BulkCategorizationRequestDTO;
import com.financemanagerai.genaisvc.service.GeminiService;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'ROLE_USER', 'ROLE_ADMIN')")
    public Map<String, String> handleQuery(@RequestBody Map<String, String> request) {
        String userQuery = request.get("query");
        String aiResponse = geminiService.getCompletion(userQuery);

        return Map.of(
                "query", userQuery,
                "response", aiResponse
        );
    }

    @PostMapping("/categorize-batch")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'ROLE_USER', 'ROLE_ADMIN')")
    public Map<String, Object> categorizeBatch(@RequestBody BulkCategorizationRequestDTO request) {
        String prompt = buildCategorizationPrompt(request);
        String categorization = geminiService.getCompletion(prompt);

        return Map.of(
                "transactionCount", request.getTransactions().size(),
                "categorization", categorization,
                "availableCategories", request.getAvailableCategories()
        );
    }

    /**
     * Build a comprehensive prompt with headers for categorization
     */
    private String buildCategorizationPrompt(BulkCategorizationRequestDTO request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a financial transaction categorizer. Categorize the following transactions into one of these categories: ");
        prompt.append(String.join(", ", request.getAvailableCategories())).append("\n\n");

        prompt.append("For each transaction, respond with ONLY a JSON object on a single line with this format:\n");
        prompt.append("{\"category\": \"<category_name>\", \"confidence\": \"<HIGH|MEDIUM|LOW>\"}\n\n");
        prompt.append("Rules:\n");
        prompt.append("- Match based on transaction description, amount, merchant, date patterns, or any other relevant details\n");
        prompt.append("- Use confidence scores: HIGH for certain matches, MEDIUM for reasonable matches, LOW for uncertain matches\n");
        prompt.append("- Respond with exactly one JSON object per transaction line\n");
        prompt.append("- Use ONLY the available categories listed above\n");
        prompt.append("- Do NOT use any other categories\n\n");
        prompt.append("Transactions:\n");

        for (int i = 0; i < request.getTransactions().size(); i++) {
            var tx = request.getTransactions().get(i);
            prompt.append((i + 1)).append(". ");
            if (tx.getDate() != null) prompt.append("Date: ").append(tx.getDate()).append(" | ");
            if (tx.getAmount() != null) prompt.append("Amount: ").append(tx.getAmount()).append(" | ");
            if (tx.getDescription() != null) prompt.append("Description: ").append(tx.getDescription());
            if (tx.getMerchant() != null) prompt.append(" | Merchant: ").append(tx.getMerchant());
            prompt.append("\n");
        }

        prompt.append("\nRespond with one JSON object per line, in the same order as the transactions above.\n");

        return prompt.toString();
    }
}
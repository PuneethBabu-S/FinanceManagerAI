package com.financemanagerai.file_processing_service.service;

import com.financemanagerai.file_processing_service.dto.BulkCategorizationRequestDTO;
import com.financemanagerai.file_processing_service.dto.CategorizedTransactionDTO;
import com.financemanagerai.file_processing_service.dto.ExtractedTransactionDTO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategorizationService {

    private final RestTemplate restTemplate;

    @Value("${genai-service.url}")
    private String genaiServiceUrl;

    public CategorizationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<CategorizedTransactionDTO> categorizeBatch(
            List<ExtractedTransactionDTO> transactions,
            List<String> availableCategories) {

        List<CategorizedTransactionDTO> categorizedTransactions = new ArrayList<>();

        try {
            // Build prompt for bulk categorization
            String prompt = buildCategorizationPrompt(transactions, availableCategories);

            // Call GenAI service
            String categorization = callGenAIService(prompt);

            // Parse response and map to transactions
            categorizedTransactions = parseCategorizationResponse(transactions, categorization, availableCategories);

        } catch (Exception e) {
            // Log error and return transactions with default categorization
            System.err.println("Error calling GenAI service: " + e.getMessage());
            return createDefaultCategorization(transactions, availableCategories);
        }

        return categorizedTransactions;
    }

    private String buildCategorizationPrompt(List<ExtractedTransactionDTO> transactions,
                                            List<String> availableCategories) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a financial transaction categorizer. Categorize the following transactions into one of these categories: ");
        prompt.append(String.join(", ", availableCategories)).append("\n\n");
        prompt.append("For each transaction, respond with ONLY the category name (nothing else).\n\n");
        prompt.append("Transactions:\n");

        for (int i = 0; i < transactions.size(); i++) {
            ExtractedTransactionDTO tx = transactions.get(i);
            prompt.append(i + 1).append(". ");
            prompt.append(tx.getDate()).append(" | ");
            prompt.append(tx.getAmount()).append(" | ");
            prompt.append(tx.getDescription()).append("\n");
        }

        return prompt.toString();
    }

    private String callGenAIService(String prompt) {
        try {
            String endpoint = genaiServiceUrl + "/genai/query";

            JSONObject requestBody = new JSONObject();
            requestBody.put("query", prompt);

            // Using RestTemplate to call the GenAI service
            String response = restTemplate.postForObject(endpoint, requestBody.toString(), String.class);

            // Parse the response to extract the actual categorization
            JSONObject jsonResponse = new JSONObject(response);
            if (jsonResponse.has("response")) {
                return jsonResponse.getString("response");
            }

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to call GenAI service: " + e.getMessage());
        }
    }

    private List<CategorizedTransactionDTO> parseCategorizationResponse(
            List<ExtractedTransactionDTO> transactions,
            String categorizationResponse,
            List<String> availableCategories) {

        List<CategorizedTransactionDTO> result = new ArrayList<>();
        String[] lines = categorizationResponse.split("\n");

        for (int i = 0; i < transactions.size(); i++) {
            ExtractedTransactionDTO transaction = transactions.get(i);
            String suggestedCategory = availableCategories.get(0); // default to first category

            // Parse response line
            if (i < lines.length) {
                String line = lines[i].trim();

                // Extract category from response (e.g., "1. Groceries" -> "Groceries")
                String categoryName = extractCategoryFromLine(line, availableCategories);
                if (categoryName != null) {
                    suggestedCategory = categoryName;
                }
            }

            CategorizedTransactionDTO categorized = CategorizedTransactionDTO.builder()
                    .transaction(transaction)
                    .suggestedCategory(suggestedCategory)
                    .confidence("MEDIUM") // Can be enhanced with actual confidence scores
                    .build();

            result.add(categorized);
        }

        return result;
    }

    private String extractCategoryFromLine(String line, List<String> availableCategories) {
        // Remove numbering (e.g., "1. ", "2. ")
        line = line.replaceAll("^\\d+\\.\\s*", "").trim();

        // Check if line matches any available category
        for (String category : availableCategories) {
            if (line.equalsIgnoreCase(category) || line.startsWith(category)) {
                return category;
            }
        }

        return null;
    }

    private List<CategorizedTransactionDTO> createDefaultCategorization(
            List<ExtractedTransactionDTO> transactions,
            List<String> availableCategories) {

        List<CategorizedTransactionDTO> result = new ArrayList<>();
        String defaultCategory = availableCategories.isEmpty() ? "Others" : availableCategories.get(0);

        for (ExtractedTransactionDTO transaction : transactions) {
            CategorizedTransactionDTO categorized = CategorizedTransactionDTO.builder()
                    .transaction(transaction)
                    .suggestedCategory(defaultCategory)
                    .confidence("LOW")
                    .build();
            result.add(categorized);
        }

        return result;
    }
}


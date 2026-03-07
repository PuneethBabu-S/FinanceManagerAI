package com.financemanagerai.file_processing_service.service;

import com.financemanagerai.file_processing_service.dto.CategorizedTransactionDTO;
import com.financemanagerai.file_processing_service.dto.ExtractedTransactionDTO;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategorizationService {

    private static final Logger log = LoggerFactory.getLogger(CategorizationService.class);

    private final RestTemplate restTemplate;

    @Value("${genai-service.url}")
    private String genaiServiceUrl;

    public CategorizationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Categorize transactions using GenAI service with token authentication
     * @param transactions List of extracted transactions
     * @param availableCategories List of available category names
     * @param token Bearer token for authentication
     * @return List of categorized transactions
     */
    public List<CategorizedTransactionDTO> categorizeBatch(
            List<ExtractedTransactionDTO> transactions,
            List<String> availableCategories,
            String token) {

        try {
            log.info("Starting categorization for {} transactions", transactions.size());

            // Filter out header rows before sending to GenAI to reduce token usage
            List<ExtractedTransactionDTO> dataRows = new ArrayList<>();
            ExtractedTransactionDTO headerRow = null;

            for (ExtractedTransactionDTO tx : transactions) {
                if (tx.isHeaderRow()) {
                    headerRow = tx;
                    log.debug("Found header row: {}", tx.getRowValues());
                } else {
                    dataRows.add(tx);
                }
            }

            if (dataRows.isEmpty()) {
                log.warn("No data rows found after filtering headers");
                return new ArrayList<>();
            }

            log.info("Filtered {} data rows (excluded {} header rows)", dataRows.size(), transactions.size() - dataRows.size());

            // Build prompt for bulk categorization with headers included
            String prompt = buildCategorizationPrompt(dataRows, headerRow, availableCategories);
            log.debug("Built categorization prompt with {} available categories", availableCategories.size());

            // Call GenAI service with token
            String categorizationResponse = callGenAIService(prompt, token);
            log.debug("Received GenAI response: {}", categorizationResponse.substring(0, Math.min(200, categorizationResponse.length())));

            // Parse response to extract categories and confidence scores
            List<CategorizedTransactionDTO> result = parseCategorizationResponse(dataRows, categorizationResponse, availableCategories);
            log.info("Successfully categorized {} transactions", result.size());

            return result;

        } catch (Exception e) {
            // Log error and return transactions with default categorization
            log.error("Error calling GenAI service: {}", e.getMessage(), e);
            return createDefaultCategorization(transactions, availableCategories);
        }
    }

    /**
     * Build a comprehensive prompt that includes headers for AI context
     */
    private String buildCategorizationPrompt(List<ExtractedTransactionDTO> transactions,
                                              ExtractedTransactionDTO headerRow,
                                              List<String> availableCategories) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a financial transaction categorizer. Categorize the following transactions into one of these categories: ");
        prompt.append(String.join(", ", availableCategories)).append("\n\n");

        // Include column header information for context
        if (headerRow != null) {
            prompt.append("Column Headers: ").append(headerRow.getRowValues()).append("\n\n");
        }

        prompt.append("For each transaction, respond with ONLY a JSON object on a single line with this format:\n");
        prompt.append("{\"category\": \"<category_name>\", \"confidence\": \"<HIGH|MEDIUM|LOW>\"}\n\n");
        prompt.append("Rules:\n");
        prompt.append("- Match based on transaction description, comments, vendor, amount patterns, or any other relevant details\n");
        prompt.append("- Use confidence scores: HIGH for certain matches, MEDIUM for reasonable matches, LOW for uncertain matches\n");
        prompt.append("- Respond with exactly one JSON object per transaction line\n");
        prompt.append("- Use only the available categories listed above\n\n");
        prompt.append("Transaction Data:\n");

        // Send only data rows (no headers)
        for (int i = 0; i < transactions.size(); i++) {
            ExtractedTransactionDTO tx = transactions.get(i);
            prompt.append((i + 1)).append(". ").append(tx.getRowValues()).append("\n");
        }

        prompt.append("\nRespond with one JSON object per line, in the same order as the rows above.\n");

        return prompt.toString();
    }

    /**
     * Call GenAI service with Bearer token authentication
     */
    private String callGenAIService(String prompt, String token) {
        try {
            String endpoint = genaiServiceUrl + "/genai/query";

            // Create headers with Bearer token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Content-Type", "application/json");

            // Create request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("query", prompt);

            // Create HttpEntity with headers and body
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            // Using RestTemplate to call the GenAI service
            String response = restTemplate.postForObject(endpoint, entity, String.class);

            // Parse the response to extract the actual categorization
            JSONObject jsonResponse = new JSONObject(response);
            if (jsonResponse.has("response")) {
                return jsonResponse.getString("response");
            }

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to call GenAI service: " + e.getMessage(), e);
        }
    }

    /**
     * Parse categorization response (expects JSON format with category and confidence)
     */
    private List<CategorizedTransactionDTO> parseCategorizationResponse(
            List<ExtractedTransactionDTO> transactions,
            String categorizationResponse,
            List<String> availableCategories) {

        List<CategorizedTransactionDTO> result = new ArrayList<>();

        // Split by newline and collect non-empty lines
        String[] allLines = categorizationResponse.split("\\r?\\n");
        List<String> validLines = new ArrayList<>();

        for (String line : allLines) {
            String trimmed = line.trim();
            // Skip empty lines and lines that are just whitespace
            if (!trimmed.isEmpty() && !trimmed.matches("^\\s*$")) {
                validLines.add(trimmed);
            }
        }

        log.debug("Parsed {} valid response lines for {} transactions", validLines.size(), transactions.size());

        // Match each transaction with a response line
        for (int i = 0; i < transactions.size(); i++) {
            ExtractedTransactionDTO transaction = transactions.get(i);
            String suggestedCategory = availableCategories.isEmpty() ? "Others" : availableCategories.get(0);
            String confidence = "MEDIUM";

            // Parse response line if available
            if (i < validLines.size()) {
                String line = validLines.get(i);

                // Try to parse as JSON first (preferred format)
                try {
                    JSONObject categoryJson = new JSONObject(line);
                    if (categoryJson.has("category")) {
                        String categoryName = extractCategoryFromLine(categoryJson.getString("category"), availableCategories);
                        if (categoryName != null) {
                            suggestedCategory = categoryName;
                        } else {
                            // Use the category as-is if it doesn't match available categories
                            suggestedCategory = categoryJson.getString("category").trim();
                        }
                        if (categoryJson.has("confidence")) {
                            confidence = categoryJson.getString("confidence").toUpperCase().trim();
                        }
                    }
                    log.debug("Parsed JSON for transaction {}: category={}, confidence={}", i, suggestedCategory, confidence);
                } catch (Exception e) {
                    // Not JSON, try plain text parsing with cleanup
                    log.debug("Response line {} is not JSON, parsing as plain text: {}", i, line);
                    String cleanedLine = line.replaceAll("^\\d+\\.\\s*", "").trim();
                    cleanedLine = cleanedLine.replaceAll("\\s+", " "); // Normalize whitespace

                    String categoryName = extractCategoryFromLine(cleanedLine, availableCategories);
                    if (categoryName != null) {
                        suggestedCategory = categoryName;
                    } else if (!cleanedLine.isEmpty() && !cleanedLine.equals("null")) {
                        // If AI returned a free-text category, use it as-is
                        suggestedCategory = cleanedLine;
                    }
                }
            } else {
                log.warn("No response line available for transaction {}, using default category", i);
            }

            CategorizedTransactionDTO categorized = CategorizedTransactionDTO.builder()
                    .transaction(transaction)
                    .suggestedCategory(suggestedCategory)
                    .confidence(confidence)
                    .build();

            result.add(categorized);
        }

        return result;
    }

    /**
     * Extract category name from response line by matching against available categories
     */
    private String extractCategoryFromLine(String line, List<String> availableCategories) {
        // Remove numbering (e.g., "1. ", "2. ")
        line = line.replaceAll("^\\d+\\.\\s*", "").trim();

        // Check if line matches any available category (exact match first, then prefix)
        for (String category : availableCategories) {
            if (line.equalsIgnoreCase(category)) {
                return category; // Exact match
            }
        }

        // Try prefix matching
        for (String category : availableCategories) {
            if (line.toLowerCase().startsWith(category.toLowerCase())) {
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


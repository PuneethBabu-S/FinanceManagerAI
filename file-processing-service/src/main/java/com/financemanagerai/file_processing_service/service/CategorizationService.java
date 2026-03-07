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
        prompt.append("You are a financial transaction categorizer. Analyze each transaction and categorize it into ONE of these EXACT categories:\n");
        prompt.append(String.join(", ", availableCategories)).append("\n\n");

        prompt.append("IMPORTANT INSTRUCTIONS:\n");
        prompt.append("- Return ONLY valid JSON objects, one per line\n");
        prompt.append("- DO NOT include markdown code blocks (no ``` or ```json)\n");
        prompt.append("- DO NOT add any explanatory text\n");
        prompt.append("- Use ONLY the exact category names from the list above\n");
        prompt.append("- Each line must be a valid JSON object in this exact format:\n");
        prompt.append("{\"category\": \"CategoryName\", \"confidence\": \"HIGH\"}\n\n");

        prompt.append("Confidence levels:\n");
        prompt.append("- HIGH: Very certain match based on merchant name or clear description\n");
        prompt.append("- MEDIUM: Reasonable match based on context\n");
        prompt.append("- LOW: Uncertain, best guess\n\n");

        prompt.append("Categorization logic:\n");
        prompt.append("- Prioritize merchant/vendor names (Uber→Transportation, Zomato/Swiggy→Food, Spotify→Entertainment)\n");
        prompt.append("- Use transaction description if merchant is empty\n");
        prompt.append("- If both are empty, look at amount patterns and payment method\n");
        prompt.append("- For salary/income transactions, use appropriate income category if available\n\n");

        // Include column header information for context
        if (headerRow != null) {
            prompt.append("Column Headers: ").append(headerRow.getRowValues()).append("\n\n");
        }

        prompt.append("Transactions to categorize:\n");

        // Send transaction details with mapped fields
        for (int i = 0; i < transactions.size(); i++) {
            ExtractedTransactionDTO tx = transactions.get(i);
            prompt.append((i + 1)).append(". ");

            // Use mapped fields if available, otherwise use raw values
            if (tx.getDate() != null) {
                prompt.append("Date: ").append(tx.getDate()).append(" | ");
            }
            if (tx.getDescription() != null && !tx.getDescription().isEmpty()) {
                prompt.append("Description: ").append(tx.getDescription()).append(" | ");
            }
            if (tx.getMerchant() != null && !tx.getMerchant().isEmpty()) {
                prompt.append("Merchant: ").append(tx.getMerchant()).append(" | ");
            }
            if (tx.getAmount() != null) {
                prompt.append("Amount: ").append(tx.getAmount()).append(" | ");
            }
            if (tx.getCurrency() != null && !tx.getCurrency().isEmpty()) {
                prompt.append("Currency: ").append(tx.getCurrency()).append(" | ");
            }
            if (tx.getPaymentMethod() != null && !tx.getPaymentMethod().isEmpty()) {
                prompt.append("Payment: ").append(tx.getPaymentMethod());
            }

            // Include raw values for additional context
            prompt.append(" [Raw: ").append(tx.getRowValues()).append("]");
            prompt.append("\n");
        }

        prompt.append("\nReturn exactly ").append(transactions.size()).append(" JSON objects, one per line, no markdown:\n");

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

        // Remove markdown code blocks if present
        String cleanedResponse = categorizationResponse
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        // Split by newline and collect non-empty lines
        String[] allLines = cleanedResponse.split("\\r?\\n");
        List<String> validLines = new ArrayList<>();

        for (String line : allLines) {
            String trimmed = line.trim();
            // Skip empty lines, whitespace-only, and markdown artifacts
            if (!trimmed.isEmpty()
                && !trimmed.matches("^\\s*$")
                && !trimmed.equals("```")
                && !trimmed.equals("```json")
                && trimmed.startsWith("{")) {  // Only keep lines that start with JSON
                validLines.add(trimmed);
            }
        }

        log.debug("Parsed {} valid response lines for {} transactions", validLines.size(), transactions.size());

        // Match each transaction with a response line
        for (int i = 0; i < transactions.size(); i++) {
            ExtractedTransactionDTO transaction = transactions.get(i);
            String suggestedCategory = availableCategories.isEmpty() ? "Uncategorized" : availableCategories.get(0);
            String confidence = "LOW";

            // Parse response line if available
            if (i < validLines.size()) {
                String line = validLines.get(i);

                // Try to parse as JSON first (preferred format)
                try {
                    JSONObject categoryJson = new JSONObject(line);
                    if (categoryJson.has("category")) {
                        String rawCategory = categoryJson.getString("category").trim();

                        // Clean up any remaining markdown artifacts
                        rawCategory = rawCategory.replaceAll("```json", "").replaceAll("```", "").trim();

                        // Match against available categories
                        String categoryName = extractCategoryFromLine(rawCategory, availableCategories);
                        if (categoryName != null) {
                            suggestedCategory = categoryName;
                        } else if (!rawCategory.isEmpty() && !rawCategory.equalsIgnoreCase("null")) {
                            // Use the category as-is if it doesn't match but is valid
                            suggestedCategory = rawCategory;
                        }

                        if (categoryJson.has("confidence")) {
                            confidence = categoryJson.getString("confidence").toUpperCase().trim();
                            // Validate confidence level
                            if (!confidence.equals("HIGH") && !confidence.equals("MEDIUM") && !confidence.equals("LOW")) {
                                confidence = "MEDIUM";
                            }
                        }
                    }
                    log.debug("Parsed JSON for transaction {}: category={}, confidence={}", i, suggestedCategory, confidence);
                } catch (Exception e) {
                    // Not JSON, try plain text parsing with cleanup
                    log.debug("Response line {} is not valid JSON, parsing as plain text: {}", i, line);
                    String cleanedLine = line.replaceAll("^\\d+\\.\\s*", "").trim();
                    cleanedLine = cleanedLine.replaceAll("\\s+", " "); // Normalize whitespace
                    cleanedLine = cleanedLine.replaceAll("```json", "").replaceAll("```", "").trim();

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


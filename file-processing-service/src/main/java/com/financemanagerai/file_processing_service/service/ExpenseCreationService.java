package com.financemanagerai.file_processing_service.service;

import com.financemanagerai.file_processing_service.dto.CategorizedTransactionDTO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExpenseCreationService {

    private final RestTemplate restTemplate;

    @Value("${expense-service.url}")
    private String expenseServiceUrl;

    public ExpenseCreationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetch available categories from expense service
     */
    public List<String> getAvailableCategories(String username, String token) {
        try {
            String endpoint = expenseServiceUrl + "/api/categories";

            // Call expense service to get categories
            String response = restTemplate.getForObject(endpoint, String.class);

            List<String> categories = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(response);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject categoryObj = jsonArray.getJSONObject(i);
                if (categoryObj.getBoolean("active")) {
                    categories.add(categoryObj.getString("name"));
                }
            }

            return categories;
        } catch (Exception e) {
            System.err.println("Error fetching categories: " + e.getMessage());
            // Return default categories as fallback
            return List.of("Groceries", "Utilities", "Transportation", "Entertainment", "Dining", "Shopping", "Others");
        }
    }

    /**
     * Get category ID by name from expense service
     */
    public Long getCategoryIdByName(String categoryName, String token) {
        try {
            String endpoint = expenseServiceUrl + "/api/categories";

            String response = restTemplate.getForObject(endpoint, String.class);
            JSONArray jsonArray = new JSONArray(response);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject categoryObj = jsonArray.getJSONObject(i);
                if (categoryObj.getString("name").equalsIgnoreCase(categoryName) &&
                    categoryObj.getBoolean("active")) {
                    return categoryObj.getLong("id");
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching category by name: " + e.getMessage());
        }

        return null;
    }

    /**
     * Create expense in expense service
     */
    public boolean createExpense(CategorizedTransactionDTO categorizedTx,
                                 Long categoryId,
                                 String username,
                                 String token) {
        try {
            String endpoint = expenseServiceUrl + "/api/expenses/add";

            // Build expense request
            JSONObject expenseRequest = new JSONObject();
            expenseRequest.put("amount", categorizedTx.getTransaction().getAmount());
            expenseRequest.put("date", categorizedTx.getTransaction().getDate().toString());
            expenseRequest.put("description", categorizedTx.getTransaction().getDescription());
            expenseRequest.put("paymentMethod", categorizedTx.getTransaction().getPaymentMethod());
            expenseRequest.put("currency", categorizedTx.getTransaction().getCurrency());
            expenseRequest.put("categoryId", categoryId);

            // Call expense service
            String response = restTemplate.postForObject(endpoint, expenseRequest.toString(), String.class);

            return response != null && !response.isEmpty();
        } catch (Exception e) {
            System.err.println("Error creating expense: " + e.getMessage());
            return false;
        }
    }

    /**
     * Batch create expenses from categorized transactions
     */
    public List<Long> createExpensesBatch(List<CategorizedTransactionDTO> categorizedTransactions,
                                          String username,
                                          String token) {
        List<Long> createdExpenseIds = new ArrayList<>();

        for (CategorizedTransactionDTO categorizedTx : categorizedTransactions) {
            // Get category ID
            Long categoryId = getCategoryIdByName(categorizedTx.getSuggestedCategory(), token);

            if (categoryId == null) {
                System.err.println("Category not found: " + categorizedTx.getSuggestedCategory());
                continue;
            }

            // Create expense
            boolean created = createExpense(categorizedTx, categoryId, username, token);
            if (created) {
                createdExpenseIds.add(1L); // Placeholder for actual expense ID
            }
        }

        return createdExpenseIds;
    }
}


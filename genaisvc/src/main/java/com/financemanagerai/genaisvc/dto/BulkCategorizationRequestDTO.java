package com.financemanagerai.genaisvc.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkCategorizationRequestDTO {

    private List<TransactionDTO> transactions;
    private List<String> availableCategories;

    /**
     * Build a prompt for bulk categorization
     */
    public String buildPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a financial transaction categorizer. Categorize the following transactions into one of these categories: ");
        prompt.append(String.join(", ", availableCategories)).append("\n\n");
        prompt.append("For each transaction, respond with ONLY the category name (nothing else).\n\n");
        prompt.append("Transactions:\n");

        for (int i = 0; i < transactions.size(); i++) {
            TransactionDTO tx = transactions.get(i);
            prompt.append(i + 1).append(". ");
            prompt.append(tx.getDate()).append(" | ");
            prompt.append(tx.getAmount()).append(" | ");
            prompt.append(tx.getDescription()).append("\n");
        }

        return prompt.toString();
    }
}


package com.financemanagerai.file_processing_service.service;

import com.financemanagerai.file_processing_service.dto.CategorizedTransactionDTO;
import com.financemanagerai.file_processing_service.dto.ExtractedTransactionDTO;
import com.financemanagerai.file_processing_service.dto.StatementProcessingResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class StatementProcessingService {

    private final FileExtractionService fileExtractionService;
    private final CategorizationService categorizationService;
    private final ExpenseCreationService expenseCreationService;

    public StatementProcessingService(FileExtractionService fileExtractionService,
                                      CategorizationService categorizationService,
                                      ExpenseCreationService expenseCreationService) {
        this.fileExtractionService = fileExtractionService;
        this.categorizationService = categorizationService;
        this.expenseCreationService = expenseCreationService;
    }

    /**
     * Process a statement file (CSV/Excel): extract transactions with headers, categorize them, and create expenses
     */
    public StatementProcessingResponseDTO processStatement(MultipartFile file,
                                                           String username,
                                                           String token) {
        String statementId = UUID.randomUUID().toString();
        List<String> errors = new ArrayList<>();

        try {
            // Step 1: Extract transactions from file (CSV/Excel)
            // First entry will be headers so AI understands the columns
            List<ExtractedTransactionDTO> extractedTransactions = fileExtractionService.extractTransactions(file);

            if (extractedTransactions.isEmpty()) {
                return StatementProcessingResponseDTO.builder()
                        .statementId(statementId)
                        .status("FAILED")
                        .totalTransactionsExtracted(0L)
                        .totalTransactionsCategorized(0L)
                        .totalExpensesCreated(0L)
                        .message("No transactions found in the file")
                        .errors(errors)
                        .build();
            }

            // Log extracted data including headers
            System.out.println("Extracted " + extractedTransactions.size() + " rows from file (including headers).");
            for (int i = 0; i < extractedTransactions.size(); i++) {
                ExtractedTransactionDTO row = extractedTransactions.get(i);
                if (row.isHeaderRow()) {
                    System.out.println("[HEADERS] " + row.getRowValues());
                } else {
                    System.out.println("[ROW " + i + "] " + row.getRowValues());
                }
            }

//            // Step 2: Get available categories
//            List<String> availableCategories = expenseCreationService.getAvailableCategories(username, token);
//
//            // Step 3: Batch categorize transactions (headers will help AI understand context)
//            List<CategorizedTransactionDTO> categorizedTransactions =
//                    categorizationService.categorizeBatch(extractedTransactions, availableCategories);
//
//            // Step 4: Create expenses in expense service
//            List<Long> createdExpenseIds = expenseCreationService.createExpensesBatch(categorizedTransactions, username, token);
//
//            return StatementProcessingResponseDTO.builder()
//                    .statementId(statementId)
//                    .status("COMPLETED")
//                    .totalTransactionsExtracted((long) extractedTransactions.size() - 1) // -1 for header row
//                    .totalTransactionsCategorized((long) categorizedTransactions.size())
//                    .totalExpensesCreated((long) createdExpenseIds.size())
//                    .message("Statement processed successfully")
//                    .errors(errors)
//                    .build();
            return StatementProcessingResponseDTO.builder()
                    .statementId(statementId)
                    .status("EXTRACTED")
                    .totalTransactionsExtracted((long) extractedTransactions.size() - 1) // -1 for header row
                    .totalTransactionsCategorized(0L)
                    .totalExpensesCreated(0L)
                    .message("Statement extracted successfully, ready for categorization")
                    .errors(errors)
                    .build();

        } catch (IOException e) {
            errors.add("File extraction failed: " + e.getMessage());
            return buildFailureResponse(statementId, errors);
        } catch (Exception e) {
            errors.add("Statement processing failed: " + e.getMessage());
            return buildFailureResponse(statementId, errors);
        }
    }

    /**
     * Process multiple transactions in batches for categorization
     */
//    public List<CategorizedTransactionDTO> categorizeBatch(List<ExtractedTransactionDTO> transactions,
//                                                          List<String> availableCategories) {
//        int batchSize = 20;
//        List<CategorizedTransactionDTO> allCategorized = new ArrayList<>();
//
//        for (int i = 0; i < transactions.size(); i += batchSize) {
//            int end = Math.min(i + batchSize, transactions.size());
//            List<ExtractedTransactionDTO> batch = transactions.subList(i, end);
//
//            List<CategorizedTransactionDTO> categorized = categorizationService.categorizeBatch(batch, availableCategories);
//            allCategorized.addAll(categorized);
//        }
//
//        return allCategorized;
//    }
//
    private StatementProcessingResponseDTO buildFailureResponse(String statementId, List<String> errors) {
        return StatementProcessingResponseDTO.builder()
                .statementId(statementId)
                .status("FAILED")
                .totalTransactionsExtracted(0L)
                .totalTransactionsCategorized(0L)
                .totalExpensesCreated(0L)
                .errors(errors)
                .message("Statement processing failed")
                .build();
    }
}


package com.financemanagerai.file_processing_service.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatementProcessingResponseDTO {

    private String statementId;
    private String status; // PROCESSING, COMPLETED, FAILED
    private Long totalTransactionsExtracted;
    private Long totalTransactionsCategorized;
    private Long totalExpensesCreated;
    private List<String> errors;
    private String message;
}


package com.financemanagerai.file_processing_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorizedTransactionDTO {

    private ExtractedTransactionDTO transaction;
    private String suggestedCategory;
    private String confidence; // HIGH, MEDIUM, LOW based on AI response
}


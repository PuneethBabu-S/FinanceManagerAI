package com.financemanagerai.file_processing_service.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkCategorizationRequestDTO {

    private List<ExtractedTransactionDTO> transactions;
    private List<String> availableCategories; // category names available in system
}


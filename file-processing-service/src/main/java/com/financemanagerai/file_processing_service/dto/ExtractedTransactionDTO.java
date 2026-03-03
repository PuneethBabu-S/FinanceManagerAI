package com.financemanagerai.file_processing_service.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractedTransactionDTO {

    // Parsed fields
    private LocalDate date;
    private Double amount;
    private String description;
    private String merchant;
    private String paymentMethod; // extracted from statement if available
    private String currency; // extracted from statement if available

    // Raw row data for AI understanding
    private List<String> rowValues;        // Raw values from the row
    private List<String> columnHeaders;    // Column headers from the file
    private boolean isHeaderRow;           // Indicates if this is a header row
}


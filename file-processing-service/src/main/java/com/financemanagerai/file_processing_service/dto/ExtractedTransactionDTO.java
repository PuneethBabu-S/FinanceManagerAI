package com.financemanagerai.file_processing_service.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractedTransactionDTO {

    private LocalDate date;
    private Double amount;
    private String description;
    private String merchant;
    private String paymentMethod; // extracted from statement if available
    private String currency; // extracted from statement if available
}


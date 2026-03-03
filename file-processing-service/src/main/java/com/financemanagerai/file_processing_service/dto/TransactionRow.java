package com.financemanagerai.file_processing_service.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRow {

    private List<String> values;  // Row values
    private boolean isHeader;     // Flag to indicate if this is a header row

    public static TransactionRow header(List<String> headerValues) {
        return TransactionRow.builder()
                .values(headerValues)
                .isHeader(true)
                .build();
    }

    public static TransactionRow data(List<String> dataValues) {
        return TransactionRow.builder()
                .values(dataValues)
                .isHeader(false)
                .build();
    }
}


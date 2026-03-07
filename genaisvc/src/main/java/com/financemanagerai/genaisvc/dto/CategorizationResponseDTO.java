package com.financemanagerai.genaisvc.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorizationResponseDTO {

    private String category;
    private String confidence; // HIGH, MEDIUM, LOW
}


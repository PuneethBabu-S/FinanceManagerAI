package com.financemanagerai.genaisvc.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDTO {

    private LocalDate date;
    private Double amount;
    private String description;
    private String merchant;
}


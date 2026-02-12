package com.financemanagerai.expense_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expense_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 255)
    private String description; // explains purpose of category

    @Column(nullable = false)
    private boolean isGlobal = false; // system-defined vs user-defined

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String createdBy; // track who created (username or 'SYSTEM')

    @Column(nullable = false)
    private boolean active = true; // soft delete support

    // Self-referencing hierarchy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ExpenseCategory parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<ExpenseCategory> subcategories = new ArrayList<>();
}
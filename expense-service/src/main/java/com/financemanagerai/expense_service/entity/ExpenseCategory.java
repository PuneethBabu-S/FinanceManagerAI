package com.financemanagerai.expense_service.entity;

import com.financemanagerai.expense_service.common.BaseEntity;
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
public class ExpenseCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 255)
    private String description;

    @Column(nullable = false)
    private boolean isGlobal = false;

    @Column(nullable = false)
    private boolean active = true;

    // Self-referencing hierarchy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ExpenseCategory parent;

    @Builder.Default
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseCategory> subcategories = new ArrayList<>();

    public void addSubcategory(ExpenseCategory subcategory) {
        subcategories.add(subcategory);
        subcategory.setParent(this);
    }
}
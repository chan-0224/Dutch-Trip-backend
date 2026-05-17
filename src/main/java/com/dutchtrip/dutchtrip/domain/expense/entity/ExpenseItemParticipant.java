package com.dutchtrip.dutchtrip.domain.expense.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "expense_item_participants")
public class ExpenseItemParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_item_id")
    private ExpenseItem expenseItem;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Builder
    public ExpenseItemParticipant(ExpenseItem expenseItem, Long userId) {
        this.expenseItem = expenseItem;
        this.userId = userId;
    }
}
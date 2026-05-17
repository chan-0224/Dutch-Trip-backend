package com.dutchtrip.dutchtrip.domain.expense.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "expense_members")
public class ExpenseMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount_paid", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "amount_owed", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountOwed;

    @Builder
    public ExpenseMember(Expense expense, Long userId, BigDecimal amountPaid, BigDecimal amountOwed) {
        this.expense = expense;
        this.userId = userId;
        this.amountPaid = amountPaid;
        this.amountOwed = amountOwed;
    }
}
package com.dutchtrip.dutchtrip.domain.expense.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "expense_type")
    private String expenseType;

    @Column(name = "payment_time")
    private LocalDateTime paymentTime;

    @Column(name = "currency")
    private String currency;

    @Column(name = "exchange_rate", precision = 10, scale = 4)
    private BigDecimal exchangeRate;

    @Column(name = "receipt_image_url")
    private String receiptImageUrl;

    @Column(name = "payer_user_id", nullable = false)
    private Long payerUserId;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseItem> items = new ArrayList<>();

    @Builder
    public Expense(Long tripId, String title, BigDecimal totalAmount, String expenseType,
                   LocalDateTime paymentTime, String currency, BigDecimal exchangeRate,
                   String receiptImageUrl, Long payerUserId) {
        this.tripId = tripId;
        this.title = title;
        this.totalAmount = totalAmount;
        this.expenseType = expenseType;
        this.paymentTime = paymentTime;
        this.currency = currency;
        this.exchangeRate = exchangeRate;
        this.receiptImageUrl = receiptImageUrl;
        this.payerUserId = payerUserId;
    }

    public void updateExpenseInfo(String title, BigDecimal totalAmount, String expenseType,
                                  LocalDateTime paymentTime, String currency,
                                  BigDecimal exchangeRate, String receiptImageUrl, Long payerUserId) {
        this.title = title;
        this.totalAmount = totalAmount;
        this.expenseType = expenseType;
        this.paymentTime = paymentTime;
        this.currency = currency;
        this.exchangeRate = exchangeRate;
        this.receiptImageUrl = receiptImageUrl;
        this.payerUserId = payerUserId;
    }
}
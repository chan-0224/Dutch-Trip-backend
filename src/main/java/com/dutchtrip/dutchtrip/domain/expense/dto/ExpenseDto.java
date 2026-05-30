package com.dutchtrip.dutchtrip.domain.expense.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ExpenseDto {

    @Getter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class OcrResponse {
        @JsonProperty("parsed_title")
        private String parsedTitle;
        @JsonProperty("parsed_payment_time")
        private String parsedPaymentTime;
        @JsonProperty("parsed_total_amount")
        private BigDecimal parsedTotalAmount;
        @JsonProperty("parsed_items")
        private List<ParsedItem> parsedItems;
    }

    @Getter @AllArgsConstructor @NoArgsConstructor
    public static class ParsedItem {
        @JsonProperty("item_name")
        private String itemName;
        private BigDecimal price;
    }

    @Getter
    public static class CreateRequest {
        private String title;
        @JsonProperty("total_amount")
        private BigDecimal totalAmount;
        @JsonProperty("expense_type")
        private String expenseType;
        @JsonProperty("payment_time")
        private LocalDateTime paymentTime;
        private String currency;
        @JsonProperty("exchange_rate")
        private BigDecimal exchangeRate;
        @JsonProperty("receipt_image_url")
        private String receiptImageUrl;
        @JsonProperty("payer_user_id")
        private Long payerUserId;
        private List<ItemRequest> items;
    }

    @Getter
    public static class ItemRequest {
        @JsonProperty("item_name")
        private String itemName;
        private BigDecimal price;
        @JsonProperty("participant_user_ids")
        private List<Long> participantUserIds;
    }

    @Getter @Builder @AllArgsConstructor
    public static class SummaryResponse {
        @JsonProperty("expense_id")
        private Long expenseId;
        private String title;
        @JsonProperty("total_amount")
        private BigDecimal totalAmount;
        private PayerInfo payer;
        @JsonProperty("payment_time")
        private LocalDateTime paymentTime;
        @JsonProperty("expense_type")
        private String expenseType;
        @JsonProperty("item_count")
        private int itemCount;
    }

    @Getter @AllArgsConstructor
    public static class PayerInfo {
        @JsonProperty("user_id")
        private Long userId;
        private String nickname;
    }

    @Getter @Builder @AllArgsConstructor
    public static class DetailResponse {
        @JsonProperty("expense_id")
        private Long expenseId;
        private String title;

        @JsonProperty("total_amount")
        private BigDecimal totalAmount;
        private PayerInfo payer;

        @JsonProperty("payment_time")
        private LocalDateTime paymentTime;

        @JsonProperty("expense_type")
        private String expenseType;

        @JsonProperty("item_count")
        private int itemCount;

        @JsonProperty("receipt_image_url")
        private String receiptImageUrl;
        private List<DetailItem> items;
    }

    @Getter @Builder @AllArgsConstructor
    public static class DetailItem {
        @JsonProperty("item_name")
        private String itemName;
        private BigDecimal price;
        private List<PayerInfo> participants;
    }
}
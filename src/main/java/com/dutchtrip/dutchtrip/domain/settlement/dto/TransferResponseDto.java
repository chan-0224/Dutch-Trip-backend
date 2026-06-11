package com.dutchtrip.dutchtrip.domain.settlement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class TransferResponseDto {

    private SenderInfo sender;
    private ReceiverInfo receiver;

    private BigDecimal amountToSend;

    private String tripName;

    private List<RelatedExpenseDto> relatedExpenses;

    @Getter
    @AllArgsConstructor
    public static class SenderInfo {
        @JsonProperty("user_id")
        private Long userId;
        private String nickname;
    }

    @Getter
    @AllArgsConstructor
    public static class ReceiverInfo {
        @JsonProperty("user_id")
        private Long userId;
        private String nickname;
        @JsonProperty("bank_name")
        private String bankName;
        @JsonProperty("account_number")
        private String accountNumber;
    }
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RelatedExpenseDto {
        @JsonProperty("expense_id")
        private Long expenseId;
        @JsonProperty("expense_title")
        private String expenseTitle;
        private BigDecimal amount;
        @JsonProperty("expense_type")
        private String expenseType;
        @JsonProperty("payer_nickname")
        private String payerNickname;
        @JsonProperty("payment_time")
        private LocalDateTime paymentTime;
    }
}
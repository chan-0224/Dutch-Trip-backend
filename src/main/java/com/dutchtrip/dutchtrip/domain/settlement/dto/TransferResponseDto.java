package com.dutchtrip.dutchtrip.domain.settlement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class TransferResponseDto {

    private SenderInfo sender;
    private ReceiverInfo receiver;

    @JsonProperty("amount_to_send")
    private BigDecimal amountToSend;

    @JsonProperty("trip_name")
    private String tripName;

    @JsonProperty("related_expenses")
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
        @JsonProperty("expense_title")
        private String expenseTitle;
        private BigDecimal amount;
    }
}
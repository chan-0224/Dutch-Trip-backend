package com.dutchtrip.dutchtrip.domain.settlement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

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

    @JsonProperty("related_expenses")
    private List<String> relatedExpenses;

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
}
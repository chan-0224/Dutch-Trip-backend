package com.dutchtrip.dutchtrip.domain.settlement.dto;

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
    private BigDecimal amountToSend;
    private List<String> relatedExpenses;

    @Getter
    @AllArgsConstructor
    public static class SenderInfo {
        private Long userId;
        private String nickname;
    }

    @Getter
    @AllArgsConstructor
    public static class ReceiverInfo {
        private Long userId;
        private String nickname;
        private String bankName;
        private String accountNumber;
    }
}

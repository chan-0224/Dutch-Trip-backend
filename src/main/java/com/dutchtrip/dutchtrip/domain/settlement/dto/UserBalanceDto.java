package com.dutchtrip.dutchtrip.domain.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class UserBalanceDto {
    private Long userId;
    private BigDecimal netBalance; //결제 할 금액 - 내야 할 금액
}
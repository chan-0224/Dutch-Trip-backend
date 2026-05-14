package com.dutchtrip.dutchtrip.domain.settlement.controller;

import com.dutchtrip.dutchtrip.domain.settlement.dto.TransferResponseDto;
import com.dutchtrip.dutchtrip.domain.settlement.service.SettlementService;
import com.dutchtrip.dutchtrip.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trips/{tripId}/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransferResponseDto>>> getSettlements(@PathVariable("tripId") Long tripId) {

        List<TransferResponseDto> response = settlementService.calculateAndGetSettlements(tripId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

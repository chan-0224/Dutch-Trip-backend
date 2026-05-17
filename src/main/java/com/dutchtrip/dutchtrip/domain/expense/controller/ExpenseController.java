package com.dutchtrip.dutchtrip.domain.expense.controller;

import com.dutchtrip.dutchtrip.domain.expense.dto.ExpenseDto;
import com.dutchtrip.dutchtrip.domain.expense.service.ExpenseService;
import com.dutchtrip.dutchtrip.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trips/{tripId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping("/ocr")
    public ResponseEntity<ApiResponse<ExpenseDto.OcrResponse>> analyzeReceipt(
            @AuthenticationPrincipal Long userId,
            @PathVariable("tripId") Long tripId,
            @RequestParam("image") MultipartFile image) {
        ExpenseDto.OcrResponse response = expenseService.analyzeReceipt(userId, tripId, image);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createExpense(
            @AuthenticationPrincipal Long userId,
            @PathVariable("tripId") Long tripId,
            @RequestBody ExpenseDto.CreateRequest request) {
        expenseService.createExpense(userId, tripId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExpenseDto.DetailResponse>>> getExpenses(
            @AuthenticationPrincipal Long userId,
            @PathVariable("tripId") Long tripId) {
        List<ExpenseDto.DetailResponse> response = expenseService.getExpensesByTrip(userId, tripId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
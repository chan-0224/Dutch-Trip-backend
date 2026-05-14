package com.dutchtrip.dutchtrip.domain.expense.controller;

import com.dutchtrip.dutchtrip.domain.expense.dto.ExpenseDto;
import com.dutchtrip.dutchtrip.domain.expense.service.ExpenseService;
import com.dutchtrip.dutchtrip.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
            @PathVariable("tripId") Long tripId,
            @RequestParam("image") MultipartFile image) {
        ExpenseDto.OcrResponse response = expenseService.analyzeReceipt(image);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createExpense(
            @PathVariable("tripId") Long tripId,
            @RequestBody ExpenseDto.CreateRequest request) {
        expenseService.createExpense(tripId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExpenseDto.SummaryResponse>>> getExpenses(
            @PathVariable("tripId") Long tripId) {
        List<ExpenseDto.SummaryResponse> response = expenseService.getExpensesByTrip(tripId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
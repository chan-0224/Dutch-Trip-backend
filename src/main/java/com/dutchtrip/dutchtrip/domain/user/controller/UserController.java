package com.dutchtrip.dutchtrip.domain.user.controller;

import com.dutchtrip.dutchtrip.domain.user.dto.UserResponse;
import com.dutchtrip.dutchtrip.domain.user.dto.UserUpdateRequest;
import com.dutchtrip.dutchtrip.domain.user.service.UserService;
import com.dutchtrip.dutchtrip.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getMe(userId)));
    }

    @PutMapping("/me/bank-info")
    public ResponseEntity<ApiResponse<UserResponse>> updateBankInfo(
            @AuthenticationPrincipal Long userId,
            @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateBankInfo(userId, request)));
    }
}

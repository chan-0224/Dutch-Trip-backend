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
    public ResponseEntity<ApiResponse<Void>> updateBankInfo(
            @AuthenticationPrincipal Long userId,
            @RequestBody UserUpdateRequest request) {
        userService.updateBankInfo(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(null, "계좌 정보가 성공적으로 업데이트되었습니다."));
    }
}

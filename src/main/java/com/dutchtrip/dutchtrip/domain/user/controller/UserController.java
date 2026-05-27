package com.dutchtrip.dutchtrip.domain.user.controller;

import com.dutchtrip.dutchtrip.domain.user.dto.UserResponse;
import com.dutchtrip.dutchtrip.domain.user.dto.UserUpdateRequest;
import com.dutchtrip.dutchtrip.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userService.getMe(userId));
    }

    @PutMapping("/me/bank-info")
    public ResponseEntity<Map<String, Object>> updateBankInfo(
            @AuthenticationPrincipal Long userId,
            @RequestBody UserUpdateRequest request) {
        userService.updateBankInfo(userId, request);
        return ResponseEntity.ok(Map.of("status", 200, "message", "계좌 정보가 성공적으로 업데이트되었습니다."));
    }
}

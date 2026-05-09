package com.dutchtrip.dutchtrip.domain.auth.controller;

import com.dutchtrip.dutchtrip.domain.auth.dto.KakaoLoginRequest;
import com.dutchtrip.dutchtrip.domain.auth.dto.TokenResponse;
import com.dutchtrip.dutchtrip.domain.auth.service.AuthService;
import com.dutchtrip.dutchtrip.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/kakao")
    public ResponseEntity<ApiResponse<TokenResponse>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.kakaoLogin(request)));
    }
}

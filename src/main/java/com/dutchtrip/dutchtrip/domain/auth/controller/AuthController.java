package com.dutchtrip.dutchtrip.domain.auth.controller;

import com.dutchtrip.dutchtrip.domain.auth.dto.KakaoLoginRequest;
import com.dutchtrip.dutchtrip.domain.auth.dto.LoginResponse;
import com.dutchtrip.dutchtrip.domain.auth.service.AuthService;
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
    public ResponseEntity<LoginResponse> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request) {
        return ResponseEntity.ok(authService.kakaoLogin(request));
    }
}

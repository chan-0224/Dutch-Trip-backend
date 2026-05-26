package com.dutchtrip.dutchtrip.domain.auth.controller;

import com.dutchtrip.dutchtrip.domain.auth.dto.LoginResponse;
import com.dutchtrip.dutchtrip.domain.user.entity.User;
import com.dutchtrip.dutchtrip.domain.user.repository.UserRepository;
import com.dutchtrip.dutchtrip.global.common.ApiResponse;
import com.dutchtrip.dutchtrip.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class DevAuthController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/dev-login")
    public ResponseEntity<ApiResponse<LoginResponse>> devLogin(
            @RequestParam(defaultValue = "1") Long userId) {

        User user = userRepository.findById(userId).orElseGet(() ->
                userRepository.save(User.builder()
                        .kakaoId("dev-" + userId)
                        .nickname("테스트유저" + userId)
                        .email("dev" + userId + "@test.com")
                        .build()));

        String token = jwtTokenProvider.generateToken(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(
                new LoginResponse(token, user.getId(), user.getNickname(), false)));
    }
}

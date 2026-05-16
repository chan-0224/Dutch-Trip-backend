package com.dutchtrip.dutchtrip.domain.auth.service;

import com.dutchtrip.dutchtrip.domain.auth.dto.KakaoLoginRequest;
import com.dutchtrip.dutchtrip.domain.auth.dto.LoginResponse;
import com.dutchtrip.dutchtrip.domain.user.entity.User;
import com.dutchtrip.dutchtrip.domain.user.repository.UserRepository;
import com.dutchtrip.dutchtrip.global.security.JwtTokenProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public LoginResponse kakaoLogin(KakaoLoginRequest request) {
        KakaoUserInfo userInfo = getKakaoUserInfo(request.getKakaoAccessToken());

        Optional<User> existing = userRepository.findByKakaoId(String.valueOf(userInfo.getId()));
        boolean isNewUser = existing.isEmpty();

        User user = existing.orElseGet(() -> userRepository.save(User.builder()
                .kakaoId(String.valueOf(userInfo.getId()))
                .email(userInfo.getEmail())
                .nickname(userInfo.getNickname())
                .build()));

        return new LoginResponse(jwtTokenProvider.generateToken(user.getId()), user.getId(), user.getNickname(), isNewUser);
    }

    private KakaoUserInfo getKakaoUserInfo(String accessToken) {
        return RestClient.create()
                .get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(KakaoUserInfo.class);
    }

    @Getter
    private static class KakaoUserInfo {
        private Long id;

        @JsonProperty("kakao_account")
        private KakaoAccount kakaoAccount;

        public String getEmail() {
            return kakaoAccount != null ? kakaoAccount.getEmail() : null;
        }

        public String getNickname() {
            if (kakaoAccount == null || kakaoAccount.getProfile() == null) return null;
            return kakaoAccount.getProfile().getNickname();
        }

        @Getter
        private static class KakaoAccount {
            private String email;
            private Profile profile;

            @Getter
            private static class Profile {
                private String nickname;
            }
        }
    }
}

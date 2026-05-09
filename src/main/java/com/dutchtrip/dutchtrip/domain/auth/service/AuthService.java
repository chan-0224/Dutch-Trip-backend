package com.dutchtrip.dutchtrip.domain.auth.service;

import com.dutchtrip.dutchtrip.domain.auth.dto.KakaoLoginRequest;
import com.dutchtrip.dutchtrip.domain.auth.dto.TokenResponse;
import com.dutchtrip.dutchtrip.domain.user.entity.User;
import com.dutchtrip.dutchtrip.domain.user.repository.UserRepository;
import com.dutchtrip.dutchtrip.global.security.JwtTokenProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${kakao.client-id:}")
    private String clientId;

    @Value("${kakao.redirect-uri:}")
    private String redirectUri;

    @Transactional
    public TokenResponse kakaoLogin(KakaoLoginRequest request) {
        String kakaoAccessToken = getKakaoAccessToken(request.getCode());
        KakaoUserInfo userInfo = getKakaoUserInfo(kakaoAccessToken);

        User user = userRepository.findByKakaoId(String.valueOf(userInfo.getId()))
                .orElseGet(() -> userRepository.save(User.builder()
                        .kakaoId(String.valueOf(userInfo.getId()))
                        .email(userInfo.getEmail())
                        .nickname(userInfo.getNickname())
                        .build()));

        return new TokenResponse(jwtTokenProvider.generateToken(user.getId()));
    }

    private String getKakaoAccessToken(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        KakaoTokenResponse response = RestClient.create()
                .post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(KakaoTokenResponse.class);

        return response.getAccessToken();
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
    private static class KakaoTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
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

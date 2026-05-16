package com.dutchtrip.dutchtrip.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private Long userId;
    private String nickname;

    @JsonProperty("is_new_user")
    private boolean newUser;
}

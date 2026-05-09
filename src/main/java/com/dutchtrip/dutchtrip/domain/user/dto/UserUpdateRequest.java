package com.dutchtrip.dutchtrip.domain.user.dto;

import lombok.Getter;

@Getter
public class UserUpdateRequest {

    private String nickname;
    private String bankName;
    private String accountNumber;
}

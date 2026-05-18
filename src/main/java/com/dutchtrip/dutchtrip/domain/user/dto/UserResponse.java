package com.dutchtrip.dutchtrip.domain.user.dto;

import com.dutchtrip.dutchtrip.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String bankName;
    private String accountNumber;

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getBankName(),
                user.getAccountNumber()
        );
    }
}

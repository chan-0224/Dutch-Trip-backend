package com.dutchtrip.dutchtrip.domain.user.entity;

import com.dutchtrip.dutchtrip.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String kakaoId;

    private String email;

    private String nickname;

    private String profileImageUrl;

    private String bankName;

    private String accountNumber;

    public void updateProfile(String nickname, String bankName, String accountNumber) {
        if (nickname != null) this.nickname = nickname;
        if (bankName != null) this.bankName = bankName;
        if (accountNumber != null) this.accountNumber = accountNumber;
    }

    public void updateBankInfo(String bankName, String accountNumber) {
        if (bankName != null) this.bankName = bankName;
        if (accountNumber != null) this.accountNumber = accountNumber;
    }
}

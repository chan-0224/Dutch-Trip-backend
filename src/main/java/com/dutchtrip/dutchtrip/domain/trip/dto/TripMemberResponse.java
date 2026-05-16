package com.dutchtrip.dutchtrip.domain.trip.dto;

import com.dutchtrip.dutchtrip.domain.trip.entity.TripMember;
import com.dutchtrip.dutchtrip.domain.trip.entity.TripMemberRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TripMemberResponse {

    private Long userId;
    private String nickname;
    private String role;

    public static TripMemberResponse from(TripMember member) {
        return new TripMemberResponse(
                member.getUser().getId(),
                member.getUser().getNickname(),
                member.getRole() == TripMemberRole.OWNER ? "방장" : "일반"
        );
    }
}

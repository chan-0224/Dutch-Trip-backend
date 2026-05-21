package com.dutchtrip.dutchtrip.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    TRIP_NOT_FOUND(HttpStatus.NOT_FOUND, "여행 방을 찾을 수 없습니다."),
    INVALID_INVITE_CODE(HttpStatus.NOT_FOUND, "유효하지 않은 초대 코드입니다."),
    ALREADY_JOINED(HttpStatus.CONFLICT, "이미 참여 중인 여행 방입니다."),
    NOT_TRIP_MEMBER(HttpStatus.FORBIDDEN, "해당 여행 방의 멤버가 아닙니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}

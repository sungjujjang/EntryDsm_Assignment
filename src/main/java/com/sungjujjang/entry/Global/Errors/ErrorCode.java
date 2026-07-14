package com.sungjujjang.entry.Global.Errors;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorCode {
    APPLICATION_NOT_FOUND_ERR(404, "지원서를 찾을 수 없습니다."),
    USER_NOT_FOUND_ERR(404, "유저를 찾을 수 없습니다."),
    PHONE_DUPLICATION_ERR(409, "휴대전화 번호가 중복되었습니다."),
    ALREADY_SUBMITTED(409, "이미 제출되어 수정이 불가합니다."),

    NOT_VALID_DTO_ERR(400, "Body 값이 잘못되었습니다."),

    INTERNAL_SERVER_ERR(500, "서버 측 오류가 발생했습니다.");

    private Integer errorCode;
    private String errorMessage;
}
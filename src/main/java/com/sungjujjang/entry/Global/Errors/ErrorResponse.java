package com.sungjujjang.entry.Global.Errors;

import lombok.Builder;

@Builder
public record ErrorResponse(
        Boolean status,
        Integer errorCode,
        String errorMsg,
        String errorDescription
) {
    public static ErrorResponse errorCodeFrom(ErrorCode errorCode, String description) {
        return new ErrorResponse(
                Boolean.FALSE,
                errorCode.getErrorCode(),
                errorCode.getErrorMessage(),
                description
        );
    }

    public static ErrorResponse errorCodeOf(Integer errorCode, String errorMsg, String description) {
        return new ErrorResponse(
                Boolean.FALSE,
                errorCode,
                errorMsg,
                description
        );
    }
}
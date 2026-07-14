package com.sungjujjang.entry.Global.Errors.exception;

import com.sungjujjang.entry.Global.Errors.BusinessException;
import com.sungjujjang.entry.Global.Errors.ErrorCode;

public class ALREADY_SUBMITTED extends BusinessException {
    public static final BusinessException EXCEPTION = new ALREADY_SUBMITTED();
    public ALREADY_SUBMITTED() {
        super(ErrorCode.ALREADY_SUBMITTED, "이미 제출되어 수정이 불가능합니다.");
    }
}

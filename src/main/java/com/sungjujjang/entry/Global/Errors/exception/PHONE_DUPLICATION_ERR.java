package com.sungjujjang.entry.Global.Errors.exception;

import com.sungjujjang.entry.Global.Errors.BusinessException;
import com.sungjujjang.entry.Global.Errors.ErrorCode;

public class PHONE_DUPLICATION_ERR extends BusinessException {
    public static final BusinessException EXCEPTION = new PHONE_DUPLICATION_ERR();
    public PHONE_DUPLICATION_ERR() {
        super(ErrorCode.PHONE_DUPLICATION_ERR, "휴대폰 번호가 이미 존재합니다.");
    }
}

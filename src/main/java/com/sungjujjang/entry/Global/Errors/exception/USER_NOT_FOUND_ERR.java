package com.sungjujjang.entry.Global.Errors.exception;

import com.sungjujjang.entry.Global.Errors.BusinessException;
import com.sungjujjang.entry.Global.Errors.ErrorCode;

public class USER_NOT_FOUND_ERR extends BusinessException {
    public static final BusinessException EXCEPTION = new USER_NOT_FOUND_ERR();
    public USER_NOT_FOUND_ERR() {
        super(ErrorCode.USER_NOT_FOUND_ERR, "유저를 찾을 수 없습니다.");
    }
}

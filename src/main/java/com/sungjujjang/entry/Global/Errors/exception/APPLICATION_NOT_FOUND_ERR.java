package com.sungjujjang.entry.Global.Errors.exception;

import com.sungjujjang.entry.Global.Errors.BusinessException;
import com.sungjujjang.entry.Global.Errors.ErrorCode;

public class APPLICATION_NOT_FOUND_ERR extends BusinessException {
    public static final BusinessException EXCEPTION = new APPLICATION_NOT_FOUND_ERR();
    public APPLICATION_NOT_FOUND_ERR() {
        super(ErrorCode.APPLICATION_NOT_FOUND_ERR, "지원서를 찾을 수 없습니다.");
    }
}

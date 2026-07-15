package com.sungjujjang.entry.Global.Errors.exception;

import com.sungjujjang.entry.Global.Errors.BusinessException;
import com.sungjujjang.entry.Global.Errors.ErrorCode;

public class BLANK_FIELD_EXIST extends BusinessException {
    public static final BusinessException EXCEPTION = new BLANK_FIELD_EXIST();
    public BLANK_FIELD_EXIST() {
        super(ErrorCode.BLANK_FIELD_EXIST, "빈 칸이 존재하여 제출이 불가능합니다.");
    }
}

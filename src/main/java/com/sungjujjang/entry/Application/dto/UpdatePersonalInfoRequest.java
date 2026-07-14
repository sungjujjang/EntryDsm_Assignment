package com.sungjujjang.entry.Application.dto;

import com.sungjujjang.entry.Global.Enums.Gender;
import com.sungjujjang.entry.Global.Enums.Region;
import jakarta.validation.constraints.Pattern;


public record UpdatePersonalInfoRequest(
        String name,
        @Pattern(
                regexp = "^(?:d{2})(?:0[1-9]|1[0-2])(?:0[1-9]|[12]d|3[01])$",
                message = "생년월일은 YYMMDD 형태로 입력하여 주세요."
        )
        String birth,
        Gender gender,
        Region region
) {
}
package com.sungjujjang.entry.Application.dto;

import com.sungjujjang.entry.Global.Enums.Gender;
import com.sungjujjang.entry.Global.Enums.Region;
import jakarta.validation.constraints.Pattern;


public record UpdatePersonalInfoRequest(
        String name,
        String birth,
        Gender gender,
        Region region
) {
}
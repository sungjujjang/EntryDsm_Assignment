package com.sungjujjang.entry.Auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

public record LoginRequest(
        @NotBlank(message = "전화번호는 필수입니다")
        String phone,

        @NotBlank(message = "비밀번호는 필수입니다")
        String password
) {}

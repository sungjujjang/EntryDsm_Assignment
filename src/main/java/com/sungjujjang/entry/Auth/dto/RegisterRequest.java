package com.sungjujjang.entry.Auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

public record RegisterRequest(
        @NotBlank(message = "전화번호는 필수입니다")
        String phone,
        @NotBlank(message = "이름은 필수입니다")
                String name,
        @NotBlank(message = "비밀번호는 필수입니다")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,20}$",
                message = "비밀번호는 8~20자의 영문, 숫자, 특수문자를 포함해야 합니다."
        )
        String password
) {}
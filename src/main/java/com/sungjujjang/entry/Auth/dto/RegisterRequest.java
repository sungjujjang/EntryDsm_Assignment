package com.sungjujjang.entry.Auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RegisterRequest {

    @NotBlank(message = "전화번호는 필수입니다")
    private String phone;

    @NotBlank(message = "이름은 필수입니다")
    private String name;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
}

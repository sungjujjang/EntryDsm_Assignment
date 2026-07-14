package com.sungjujjang.entry.Auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

public record LoginResponse (
        String accessToken
) {}

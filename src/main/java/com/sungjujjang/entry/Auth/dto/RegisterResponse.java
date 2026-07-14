package com.sungjujjang.entry.Auth.dto;

import lombok.Builder;

@Builder
public record RegisterResponse (
        Boolean status
) {}
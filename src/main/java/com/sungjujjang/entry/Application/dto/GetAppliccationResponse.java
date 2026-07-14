package com.sungjujjang.entry.Application.dto;

import lombok.Builder;

@Builder
public record GetAppliccationResponse(
        Boolean status,
        ApplicationDTO data
) {
}

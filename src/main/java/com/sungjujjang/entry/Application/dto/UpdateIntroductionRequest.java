package com.sungjujjang.entry.Application.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateIntroductionRequest(
        @Size(max = 2000, message = "자기소개서는 1600자 이내로 작성해 주세요.")
        String introduction
) {
}

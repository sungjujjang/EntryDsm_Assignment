package com.sungjujjang.entry.Application.dto;

import com.sungjujjang.entry.Application.Application;
import com.sungjujjang.entry.Auth.User;
import com.sungjujjang.entry.Auth.dto.UserDTO;
import com.sungjujjang.entry.Global.Enums.Gender;
import com.sungjujjang.entry.Global.Enums.Region;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ApplicationDTO(
        Long id,
        String name,
        String birth,
        Gender gender,
        Region region,
        String introduction,
        String propose,
        LocalDateTime submitedAt,
        UserDTO user
) {
    public static ApplicationDTO from(Application application) {
        return new ApplicationDTO(
                application.getId(),
                application.getName(),
                application.getBirth(),
                application.getGender(),
                application.getRegion(),
                application.getIntroduction(),
                application.getPropose(),
                application.getSubmitedAt(),
                UserDTO.from(application.getUser())
        );
    }
}

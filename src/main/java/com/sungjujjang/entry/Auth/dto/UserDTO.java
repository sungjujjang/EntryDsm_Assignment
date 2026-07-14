package com.sungjujjang.entry.Auth.dto;

import com.sungjujjang.entry.Application.Application;
import com.sungjujjang.entry.Application.dto.ApplicationDTO;
import com.sungjujjang.entry.Auth.User;
import lombok.Builder;

@Builder
public record UserDTO(
        Long id,
        String phone,
        String name
) {
    public static UserDTO from(User user) {
        return new UserDTO(
                user.getId(),
                user.getPhone(),
                user.getName()
        );
    }
}

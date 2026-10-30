package com.certverify.dto;

import com.certverify.entity.Role;
import com.certverify.entity.User;
import com.certverify.entity.UserStatus;

import java.time.Instant;

public record UserDto(
        Long id,
        String name,
        String email,
        Role role,
        UserStatus status,
        String institutionName,
        String rejectionReason,
        Instant createdAt
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getInstitutionName(),
                user.getRejectionReason(),
                user.getCreatedAt()
        );
    }
}

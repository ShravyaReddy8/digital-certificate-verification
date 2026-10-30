package com.certverify.dto;

public record RegisterResponse(
        String message,
        UserDto user
) {
}

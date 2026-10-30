package com.certverify.dto;

import com.certverify.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IssuerStatusRequest(
        @NotNull(message = "Status is required")
        UserStatus status,

        @Size(max = 500, message = "Reason must be at most 500 characters")
        String reason
) {
}

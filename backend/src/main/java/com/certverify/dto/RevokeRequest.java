package com.certverify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevokeRequest(
        @NotBlank(message = "A revocation reason is required")
        @Size(max = 500, message = "Reason must be at most 500 characters")
        String reason
) {
}

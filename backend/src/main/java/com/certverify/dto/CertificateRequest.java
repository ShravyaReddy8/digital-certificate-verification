package com.certverify.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record CertificateRequest(
        @NotBlank(message = "Student name is required")
        @Size(max = 160, message = "Student name must be at most 160 characters")
        String studentName,

        @Email(message = "Enter a valid student email address")
        @Size(max = 160)
        String studentEmail,

        @NotBlank(message = "Course or title is required")
        @Size(max = 200, message = "Course or title must be at most 200 characters")
        String courseTitle,

        @NotBlank(message = "Institution name is required")
        @Size(max = 200, message = "Institution name must be at most 200 characters")
        String institutionName,

        @NotNull(message = "Issue date is required")
        @PastOrPresent(message = "Issue date cannot be in the future")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate issueDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate expiryDate,

        @Size(max = 100, message = "Grade or remarks must be at most 100 characters")
        String grade
) {
}

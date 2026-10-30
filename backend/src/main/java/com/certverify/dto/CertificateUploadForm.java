package com.certverify.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * Form-backing bean for multipart certificate uploads (upload option B).
 */
@Getter
@Setter
public class CertificateUploadForm {

    @NotBlank(message = "Student name is required")
    @Size(max = 160)
    private String studentName;

    @Email(message = "Enter a valid student email address")
    @Size(max = 160)
    private String studentEmail;

    @NotBlank(message = "Course or title is required")
    @Size(max = 200)
    private String courseTitle;

    @NotBlank(message = "Institution name is required")
    @Size(max = 200)
    private String institutionName;

    @NotNull(message = "Issue date is required")
    @PastOrPresent(message = "Issue date cannot be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate issueDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;

    @Size(max = 100)
    private String grade;

    @NotNull(message = "A certificate file (PDF/JPG/PNG) is required")
    private MultipartFile file;

    public CertificateRequest toRequest() {
        return new CertificateRequest(studentName, studentEmail, courseTitle, institutionName,
                issueDate, expiryDate, grade);
    }
}

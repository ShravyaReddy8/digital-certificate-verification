package com.certverify.service;

import com.certverify.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final long MAX_SIZE_BYTES = 15L * 1024 * 1024;

    @Getter
    private final Path rootDirectory;

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.rootDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void initializeDirectories() {
        try {
            Files.createDirectories(rootDirectory.resolve("certificates"));
            Files.createDirectories(rootDirectory.resolve("qrcodes"));
            Files.createDirectories(rootDirectory.resolve("keys"));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create upload directories", e);
        }
    }

    public StoredFile storeCertificate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a certificate file to upload.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("Certificate file is too large. Maximum allowed size is 15 MB.");
        }

        String originalName = sanitizeFileName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Unsupported file type. Allowed formats: PDF, JPG, JPEG, PNG.");
        }

        String storedName = UUID.randomUUID() + "." + extension;
        Path target = rootDirectory.resolve("certificates").resolve(storedName);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to store the certificate file", e);
        }

        String contentType = file.getContentType() != null
                ? file.getContentType()
                : (extension.equals("pdf") ? "application/pdf" : "image/" + extension);
        return new StoredFile(originalName, storedName, target, file.getSize(), contentType);
    }

    public StoredFile storeBytes(byte[] content, String extension, String folder, String baseName) {
        String safeBase = baseName.replaceAll("[^A-Za-z0-9_-]", "_");
        String fileName = safeBase + "-" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        Path target = rootDirectory.resolve(folder).resolve(fileName);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to store generated file", e);
        }
        String contentType = extension.equalsIgnoreCase("pdf") ? "application/pdf" : "image/png";
        return new StoredFile(fileName, fileName, target, content.length, contentType);
    }

    public byte[] readBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read the stored file", e);
        }
    }

    public StoredFile storeQrCode(String certificateId, byte[] qrPng) {
        return storeBytes(qrPng, "png", "qrcodes", "qr-" + certificateId);
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "certificate";
        }
        String cleaned = fileName.replace('\\', '/');
        cleaned = cleaned.substring(cleaned.lastIndexOf('/') + 1);
        cleaned = cleaned.replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.isBlank() ? "certificate" : cleaned;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredFile(String originalFileName, String storedFileName, Path path, long size, String contentType) {
    }
}

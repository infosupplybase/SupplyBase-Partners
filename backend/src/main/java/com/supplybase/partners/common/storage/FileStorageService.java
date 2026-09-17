package com.supplybase.partners.common.storage;

import com.supplybase.partners.common.domain.BadRequestException;
import com.supplybase.partners.common.domain.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * Private, authorization-gated file storage: files sit under a root directory
 * never served statically, addressed only by a randomly generated storage key.
 * Callers are responsible for checking the requester is allowed to read a given
 * key (see e.g. VerificationController) before calling {@link #load}.
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf");
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private final Path root;

    public FileStorageService(@Value("${supplybase.storage.root-dir}") String rootDir) {
        this.root = Path.of(rootDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create storage root directory: " + root, e);
        }
    }

    public record StoredFile(String storageKey, String originalFilename, String contentType, long sizeBytes) {}

    public StoredFile store(String subfolder, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("File exceeds the 10MB size limit.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Unsupported file type. Allowed: JPEG, PNG, WEBP, PDF.");
        }
        String extension = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".pdf";
        };
        String safeName = UUID.randomUUID() + extension;
        String storageKey = subfolder + "/" + safeName;
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new BadRequestException("Invalid storage path.");
        }
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store uploaded file.", e);
        }
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : safeName;
        return new StoredFile(storageKey, originalFilename, contentType, file.getSize());
    }

    public InputStream load(String storageKey) {
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root) || !Files.exists(target)) {
            throw new NotFoundException("File not found.");
        }
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new NotFoundException("File not found.");
        }
    }

    public void copyTo(String storageKey, java.io.OutputStream out) {
        try (InputStream in = load(storageKey)) {
            in.transferTo(out);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read stored file.", e);
        }
    }
}

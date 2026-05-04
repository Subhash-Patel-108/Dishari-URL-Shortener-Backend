package com.dishari.in.infrastructure.qr;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service responsible for storing and deleting QR code files from the filesystem.
 *
 * Production considerations:
 * - Creates directory if not exists
 * - Safe file deletion with existence check
 * - Proper logging
 * - Handles both relative and absolute paths
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QrCodeStorageService {

    @Value("${app.qr.storage-path:./uploads/qr-codes}")
    private String storagePath;

    /**
     * Ensures the QR storage directory exists when the service starts.
     */
    @PostConstruct
    public void init() {
        try {
            Path dir = Paths.get(storagePath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                log.info("QR storage directory created at: {}", dir.toAbsolutePath());
            } else {
                log.info("QR storage directory already exists at: {}", dir.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Failed to create QR storage directory at path: {}", storagePath, e);
            throw new RuntimeException("Could not initialize QR storage directory", e);
        }
    }

    /**
     * Saves QR code image to filesystem and returns the relative file path.
     */
    public String saveQrCode(BufferedImage image, String fileName, String format) throws IOException {
        Path dir = Paths.get(storagePath);
        Path filePath = dir.resolve(fileName);

        // Ensure directory exists (in case it was deleted after startup)
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        ImageIO.write(image, format.toLowerCase(), filePath.toFile());

        String relativePath = storagePath + "/" + fileName;
        log.debug("QR Code saved successfully: {}", relativePath);

        return relativePath;
    }

    /**
     * Deletes a QR code file from storage.
     * Safe deletion - does not throw exception if file doesn't exist.
     *
     * @param fileUrl The stored file path (e.g., "./uploads/qr-codes/abc123.png")
     * @return true if file was deleted, false if it didn't exist or deletion failed
     */
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            log.warn("deleteFile called with null or empty fileUrl");
            return false;
        }

        try {
            // Extract filename from the stored path
            String fileName = extractFileName(fileUrl);
            Path filePath = Paths.get(storagePath, fileName);

            if (Files.exists(filePath)) {
                boolean deleted = Files.deleteIfExists(filePath);
                if (deleted) {
                    log.info("QR Code file deleted successfully: {}", filePath);
                    return true;
                } else {
                    log.warn("Failed to delete QR file: {}", filePath);
                    return false;
                }
            } else {
                log.debug("QR file does not exist, nothing to delete: {}", filePath);
                return false;
            }

        } catch (IOException e) {
            log.error("Error while deleting QR code file: {}", fileUrl, e);
            return false;
        }
    }

    /**
     * Extracts filename from the stored fileUrl.
     * Handles both relative paths and full paths.
     */
    private String extractFileName(String fileUrl) {
        // Example inputs:
        // "./uploads/qr-codes/abc123.png"
        // "/opt/dishari/qr-codes/xyz789.png"
        // "abc123.png"

        int lastSeparator = Math.max(fileUrl.lastIndexOf('/'), fileUrl.lastIndexOf('\\'));
        if (lastSeparator != -1) {
            return fileUrl.substring(lastSeparator + 1);
        }
        return fileUrl; // already just filename
    }

    /**
     * Returns the QR code as a Spring Resource for efficient streaming.
     */
    public Resource getQrCodeAsResource(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        try {
            // 1. Path Traversal Protection
            // Prevent users from passing "../config/application.yml" to escape the QR directory
            Path storageDir = Paths.get(storagePath).toAbsolutePath().normalize();
            Path filePath = storageDir.resolve(fileName).normalize();

            if (!filePath.startsWith(storageDir)) {
                log.warn("Security Alert: Path traversal attempt blocked for filename: {}", fileName);
                return null;
            }

            // 2. Existence and Readability Check
            if (Files.exists(filePath) && Files.isReadable(filePath)) {
                Resource resource = new UrlResource(filePath.toUri());
                if (resource.exists() || resource.isReadable()) {
                    return resource;
                }
            }

            log.warn("QR code not found or not readable: {}", fileName);
            return null;

        } catch (MalformedURLException e) {
            log.error("Invalid URL format for QR code file: {}", fileName, e);
            return null;
        } catch (Exception e) {
            log.error("Internal error retrieving QR code resource: {}", fileName, e);
            return null;
        }
    }

    /**
     * Checks if QR code file exists in storage.
     */
    public boolean exists(String fileName) {
        if (fileName == null) return false;
        Path filePath = Paths.get(storagePath, fileName);
        return Files.exists(filePath) && Files.isReadable(filePath);
    }

    /**
     * Optional: Delete all QR codes for a specific slug (useful for cleanup)
     */
    public void deleteAllForSlug(String slug) {
        // Can be implemented later if needed
        log.info("deleteAllForSlug called for slug={}", slug);
    }
}
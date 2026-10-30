package com.doch.common;

import com.doch.common.exceptions.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

/** Validates user-supplied images by declared type, size and actual file signature (magic bytes). */
public final class ImageUploadValidator {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private ImageUploadValidator() {
    }

    public static void validate(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(label + " is required.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException(label + " must be 5 MB or smaller.");
        }
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!ALLOWED_TYPES.contains(type)) {
            throw new BusinessException(label + " must be a JPEG, PNG or WEBP image.");
        }
        try {
            byte[] head = file.getInputStream().readNBytes(12);
            if (!hasImageSignature(head)) {
                throw new BusinessException(label + " is not a valid image file.");
            }
        } catch (IOException e) {
            throw new BusinessException("Could not read " + label.toLowerCase() + ".");
        }
    }

    static boolean hasImageSignature(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return true; // JPEG
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return true;       // PNG
        return h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';                                  // WEBP
    }

    /** Safe storage key: never trust the client-provided filename. */
    public static String extensionFor(MultipartFile file) {
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        return switch (type) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
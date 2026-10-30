package com.doch.common;

import com.doch.common.exceptions.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class ImageUploadValidatorTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};

    @Test
    void acceptsRealImages() {
        assertDoesNotThrow(() -> ImageUploadValidator.validate(new MockMultipartFile("image", "a.png", "image/png", PNG), "Photo"));
        assertDoesNotThrow(() -> ImageUploadValidator.validate(new MockMultipartFile("image", "a.jpg", "image/jpeg", JPEG), "Photo"));
    }

    @Test
    void rejectsSpoofedContentTypeAndOtherFiles() {
        byte[] html = "<html><script>alert(1)</script>".getBytes();
        assertThrows(BusinessException.class, () -> ImageUploadValidator.validate(new MockMultipartFile("image", "x.png", "image/png", html), "Photo"));
        assertThrows(BusinessException.class, () -> ImageUploadValidator.validate(new MockMultipartFile("image", "x.pdf", "application/pdf", PNG), "Photo"));
        assertThrows(BusinessException.class, () -> ImageUploadValidator.validate(new MockMultipartFile("image", "x.png", "image/png", new byte[0]), "Photo"));
        assertThrows(BusinessException.class, () -> ImageUploadValidator.validate(null, "Photo"));
    }

    @Test
    void rejectsOversizedFiles() {
        byte[] big = new byte[(int) ImageUploadValidator.MAX_BYTES + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        assertThrows(BusinessException.class, () -> ImageUploadValidator.validate(new MockMultipartFile("image", "x.png", "image/png", big), "Photo"));
    }
}

package com.doch.common.media;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.concurrent.TimeUnit;

@Controller // Use @Controller, not @RestController, to allow for redirects
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @GetMapping("/{filename:.+}") // The ':.+' is important to handle filenames with dots
    public ResponseEntity<Void> getMediaFile(@PathVariable String filename) {
        // Delivery evidence (proof of delivery, sealed parcels, Xender parcel photos) is private and only
        // exposed through short-lived presigned URLs inside authorized API responses.
        String lower = filename.toLowerCase();
        if (lower.startsWith("pod") || lower.startsWith("sealed") || lower.startsWith("xender") || lower.contains("..")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        try {
            // Generate a pre-signed URL that is valid for 5 minutes
            String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(filename)
                            .expiry(5, TimeUnit.MINUTES)
                            .build());

            // Return an HTTP 302 Redirect to the pre-signed URL
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", presignedUrl)
                    .build();

        } catch (Exception e) {
            System.err.println("Error generating pre-signed URL for " + filename + ": " + e.getMessage());
            // Return a 404 Not Found if the file doesn't exist or an error occurs
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
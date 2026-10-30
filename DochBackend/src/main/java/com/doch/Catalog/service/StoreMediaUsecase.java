package com.doch.Catalog.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StoreMediaUsecase {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public String storeInMinio(byte[] fileContent, String filename, String contentType) throws Exception {
        try {
            // log.info(" nom du bucket {}",bucketName);
            System.out.println("Bucket: " + bucketName);
            // Ensure bucket exists
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            // Upload file
            ObjectWriteResponse response = minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .stream(new ByteArrayInputStream(fileContent), fileContent.length, -1)
                            .contentType(contentType)
                            .build());

            System.out.println("Upload successful:");
            System.out.println("ETag: " + response.etag());
            System.out.println("Version ID: " + response.versionId());

            return filename; // or return full path if needed

        } catch (Exception e) {
            System.err.println("Error storing in MinIO: " + e);
            e.printStackTrace();
            // log.error(" message error {}",e.getMessage());
            throw e;
        }
    }

    // public byte[] getFromMinio(String filename) throws Exception {
    //     try (InputStream stream = minioClient.getObject(
    //             GetObjectArgs.builder()
    //                     .bucket(bucketName)
    //                     .object(filename)
    //                     .build())) {
    //         return stream.readAllBytes();
    //     }
    // }

    public Resource getFromMinio(String filename) throws Exception {
        try {

            // log.info(" nom du bucket dans le download {}",bucketName);

            // Check if the object exists
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .build());

            // Get the object as InputStream
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .build());

            // Create an InputStreamResource with the stream and filename
            return new InputStreamResource(stream) {
                @Override
                public String getFilename() {
                    return filename;
                }

                @Override
                public long contentLength() {
                    return stat.size();
                }

                @Override
                public String getDescription() {
                    return "MinIO resource [" + filename + "]";
                }
            };
        } catch (Exception e) {
            // log.info(" Dans e catch ");
            System.err.println("Error retrieving from MinIO:");
            e.printStackTrace();
            throw e;
        }
    }

    public String getPresignedUrl(String filename) {
    try {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(filename)
                        .expiry(60 * 60) // 1 hour
                        .build()
        );
    } catch (Exception e) {
        throw new RuntimeException("Error generating presigned URL for " + filename, e);
    }
}

    
}

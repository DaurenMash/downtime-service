package com.example.downtime.service;

import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final S3Template s3Template;
    private final S3Client s3Client; // Добавляем S3Client для управления бакетами

    @Value("${aws.s3.bucket:downtime-photos}")
    private String bucketName;

    @PostConstruct
    public void init() {
        createBucketIfNotExists();
    }

    private void createBucketIfNotExists() {
        try {
            // Проверяем существует ли бакет
            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build());

            log.info("Bucket '{}' already exists", bucketName);

        } catch (NoSuchBucketException e) {
            log.info("Bucket '{}' does not exist. Creating...", bucketName);

            try {
                // Создаем бакет
                s3Client.createBucket(CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .build());

                log.info("Bucket '{}' created successfully", bucketName);

                // Устанавливаем политику доступа
                setBucketPolicy();

            } catch (S3Exception ex) {
                log.error("Failed to create bucket: {}", ex.getMessage(), ex);
                throw new RuntimeException("Failed to create S3 bucket", ex);
            }
        } catch (S3Exception e) {
            log.error("Error checking bucket existence: {}", e.getMessage(), e);
            throw new RuntimeException("S3 error", e);
        }
    }

    private void setBucketPolicy() {
        try {
            // Политика для публичного чтения
            String policy = String.format("""
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Principal": "*",
                            "Action": ["s3:GetObject"],
                            "Resource": ["arn:aws:s3:::%s/*"]
                        }
                    ]
                }
                """, bucketName);

            s3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
                    .bucket(bucketName)
                    .policy(policy)
                    .build());

            log.info("Bucket policy set for '{}'", bucketName);

        } catch (S3Exception e) {
            log.warn("Could not set bucket policy: {}", e.getMessage());
        }
    }

    public String uploadFile(MultipartFile file, String downtimeId) {
        log.info("Starting file upload for downtime: {}", downtimeId);
        log.info("File details: name={}, size={}, type={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        validateFile(file);

        String fileName = generateFileName(downtimeId, file);
        log.info("Generated file path: {}", fileName);

        try (InputStream inputStream = file.getInputStream()) {
            // Загружаем файл в S3
            S3Resource resource = s3Template.upload(
                    bucketName,
                    fileName,
                    inputStream
            );

            String fileUrl = resource.getURL().toString();
            log.info("File uploaded successfully: {}", fileUrl);

            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to upload file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file", e);
        } catch (Exception e) {
            log.error("Unexpected error during file upload: {}", e.getMessage(), e);
            throw new RuntimeException("File upload failed", e);
        }
    }

    private String generateFileName(String downtimeId, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = ".jpg"; // значение по умолчанию

        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Формат: downtimes/{downtimeId}/{uuid}.{ext}
        return String.format("downtimes/%s/%s%s",
                downtimeId,
                UUID.randomUUID(),
                fileExtension.toLowerCase()
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new RuntimeException("File size exceeds limit (10MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed. Received: " + contentType);
        }

        // Проверка расширения файла
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!extension.matches("jpg|jpeg|png|gif|bmp")) {
                throw new RuntimeException("Unsupported file extension: " + extension);
            }
        }
    }
}
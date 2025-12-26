package com.example.downtime.config;

import io.awspring.cloud.autoconfigure.s3.S3AutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@ImportAutoConfiguration(S3AutoConfiguration.class)
public class S3Config {
    // Spring Cloud AWS автоматически настраивает S3Template
}
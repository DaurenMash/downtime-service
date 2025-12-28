package com.example.downtime.integrationtest;

import com.example.downtime.factory.DowntimeTestFactory;
import com.example.downtime.model.DowntimeRequest;
import com.example.downtime.model.DowntimeResponse;
import com.example.downtime.repository.DowntimeRepository;
import com.example.downtime.service.DowntimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest
public class DowntimeServiceIntegrationTest {

    @Container
    private static final MongoDBContainer mongoDBContainer =
            new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("s3.enabled", () -> "false"); // Отключаем S3
    }

    @MockBean
    private S3Client s3Client;

    @Autowired
    private DowntimeRepository downtimeRepository;

    @Autowired
    private DowntimeService downtimeService;

    @BeforeEach
    void setUp() {
        downtimeRepository.deleteAll();
    }

    @Test
    void createDowntime_Success() {
        // Подготавливаем данные
        DowntimeRequest request = DowntimeTestFactory.createRequestWithDefaults();

        // Выполняем тестируемый метод
        DowntimeResponse response = downtimeService.createDowntime(request);

        // Проверяем результат
        assertThat(response).isNotNull();
        assertThat(response.getEquipmentId()).isEqualTo("EQ-001");

        // Проверяем, что данные сохранены в MongoDB
        assertThat(downtimeRepository.count()).isEqualTo(1);
    }
}
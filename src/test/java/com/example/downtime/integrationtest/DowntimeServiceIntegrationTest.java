package com.example.downtime.integrationtest;

import com.example.downtime.model.DowntimeEvent;
import com.example.downtime.repository.DowntimeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DataMongoTest
@TestPropertySource(properties = {
        "spring.data.mongodb.uri=mongodb://admin:password@localhost:27017/downtime_db?authSource=admin",
        "s3.enabled=false"
})
public class DowntimeServiceIntegrationTest {

    @Autowired
    private DowntimeRepository downtimeRepository;

    @Test
    void findByOperatorIdSorted_ShouldWorkWithRealData() {
        // Находим любого оператора, у которого есть события в базе
        Optional<DowntimeEvent> anyEvent = downtimeRepository.findAll()
                .stream()
                .filter(event -> event.getOperatorId() != null)
                .findFirst();

        // Если в базе нет данных с операторами, пропускаем тест
        assumeTrue(anyEvent.isPresent(), "В базе нет событий с операторами для тестирования");

        String operatorId = anyEvent.get().getOperatorId();

        // Act
        List<DowntimeEvent> result = downtimeRepository.findByOperatorIdSorted(operatorId);

        // Assert: проверяем базовые свойства метода
        assertThat(result).isNotNull();

        if (!result.isEmpty()) {
            // Проверяем, что все события принадлежат указанному оператору
            assertThat(result)
                    .allMatch(event -> operatorId.equals(event.getOperatorId()));

            // Проверяем сортировку по убыванию времени начала
            assertThat(result)
                    .extracting(DowntimeEvent::getStartTime)
                    .isSortedAccordingTo(Comparator.reverseOrder());
        }

        // Выводим информацию для отладки
        System.out.println("Оператор: " + operatorId);
        System.out.println("Найдено событий: " + result.size());
        result.forEach(event ->
                System.out.println("  ID: " + event.getId() +
                        ", Start: " + event.getStartTime() +
                        ", Equipment: " + event.getEquipmentId()));
    }
}
package com.example.downtime.service;

import com.example.downtime.model.DowntimeEvent;
import com.example.downtime.model.DowntimeStatus;
import com.example.downtime.model.QDowntimeEvent;
import com.example.downtime.repository.DowntimeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.data.mongodb.uri=mongodb://admin:password@localhost:27017/downtime_db?authSource=admin"
})
class DowntimeEventRepositoryLiveTest {

    @Autowired
    private DowntimeRepository repository;

    @Test
    void findByOperatorIdSorted() {
        System.out.println("🔍 Тестируем QueryDSL метод findByOperatorIdSorted()");

        String operatorId = "01";

        System.out.println("Operator ID (string): \"" + operatorId + "\"");

        try {
            // ВЫБОРКА ИЗ БАЗЫ
            List<DowntimeEvent> results = repository.findByOperatorIdSorted(operatorId);

            System.out.println("\n✅ Успешно подключились к MongoDB!");
            System.out.println("📊 Найдено записей для operatorId \"" + operatorId + "\": " + results.size());

            if (results.isEmpty()) {
                System.out.println("\n⚠️  Записей не найдено!");

                // Давайте проверим, что есть в базе
                List<DowntimeEvent> allEvents = repository.findAll();
                System.out.println("\n📋 Всего записей в базе: " + allEvents.size());

                if (!allEvents.isEmpty()) {
                    System.out.println("\n📌 Примеры записей:");
                    allEvents.stream()
                            .limit(5)
                            .forEach(e -> System.out.printf(
                                    "ID: %d, Operator: [%s] (type: %s), Equipment: %s, Start: %s%n",
                                    e.getId(),
                                    e.getOperatorId(),
                                    e.getOperatorId() != null ? e.getOperatorId().getClass().getSimpleName() : "null",
                                    e.getEquipmentId(),
                                    e.getStartTime()
                            ));

                    // Уникальные operatorId
                    List<String> uniqueOperators = allEvents.stream()
                            .map(DowntimeEvent::getOperatorId)
                            .distinct()
                            .toList();
                    System.out.println("\n🎯 Уникальные operatorId в базе: " + uniqueOperators);

                    // Проверяем типы
                    System.out.println("\n🔍 Проверка типов данных:");
                    allEvents.forEach(e -> {
                        if (e.getOperatorId() != null) {
                            System.out.printf("ID %d: operatorId='%s' (type=%s)%n",
                                    e.getId(), e.getOperatorId(), e.getOperatorId().getClass().getName());
                        }
                    });
                }
            } else {
                System.out.println("\n🎉 QueryDSL работает! Найдены записи:");
                System.out.println("=========================================");

                results.forEach(event -> System.out.printf(
                        "ID: %d | Operator: %s | Equipment: %s | Start: %s | End: %s | Status: %s%n",
                        event.getId(),
                        event.getOperatorId(),
                        event.getEquipmentId(),
                        event.getStartTime(),
                        event.getEndTime() != null ? event.getEndTime() : "N/A",
                        event.getStatus()
                ));

                // Проверяем сортировку
                System.out.println("\n✅ Проверка сортировки по убыванию startTime:");
                for (int i = 0; i < results.size() - 1; i++) {
                    boolean isCorrect = results.get(i).getStartTime()
                            .isAfter(results.get(i + 1).getStartTime());
                    System.out.printf("  Record %d > Record %d: %s (Start: %s vs %s)%n",
                            i + 1, i + 2,
                            isCorrect ? "✅" : "❌",
                            results.get(i).getStartTime(),
                            results.get(i + 1).getStartTime()
                    );
                }
            }

        } catch (Exception e) {
            System.err.println("\n❌ Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
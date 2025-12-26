package com.example.downtime.repository;

import com.example.downtime.model.DowntimeEvent;
import com.example.downtime.model.DowntimeStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DowntimeRepository extends MongoRepository<DowntimeEvent, String> {

    List<DowntimeEvent> findByEquipmentId(String equipmentId);

    List<DowntimeEvent> findByEquipmentIdAndStatus(String equipmentId, DowntimeStatus status);

    List<DowntimeEvent> findByStatus(DowntimeStatus status);

    List<DowntimeEvent> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    List<DowntimeEvent> findByOperatorId(String operatorId);

    Optional<DowntimeEvent> findByIdAndOperatorId(String id, String operatorId);

    boolean existsByEquipmentIdAndStatus(String equipmentId, DowntimeStatus status);

    long countByStatus(DowntimeStatus status);

    // ИСПРАВЛЕНО: для подсчета сегодняшних событий
    long countByStartTimeGreaterThanEqual(LocalDateTime dateTime);

    // УДАЛИТЬ методы, которые не являются стандартными для Spring Data MongoDB:
    // long countToday();                    ← удалить (реализовать в сервисе)
    // String getAverageDuration();          ← удалить (реализовать в сервисе)
    // long countTotalPhotos();              ← удалить (реализовать в сервисе)
    // List<Map<String, String>> getAllEquipment(); ← удалить (реализовать в сервисе)
    // Page<DowntimeEvent> getFilteredDowntimes(...); ← удалить (реализовать в сервисе)
    // DowntimeEvent getDowntimeEntity(String id); ← удалить (использовать findById)
}
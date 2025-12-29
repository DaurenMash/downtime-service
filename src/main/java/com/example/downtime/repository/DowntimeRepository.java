package com.example.downtime.repository;

import com.example.downtime.model.DowntimeEvent;
import com.example.downtime.model.DowntimeStatus;
import com.example.downtime.model.QDowntimeEvent;
import com.querydsl.core.types.Predicate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


import org.springframework.data.mongodb.repository.Query;

@Repository
public interface DowntimeRepository extends
        MongoRepository<DowntimeEvent, Long>,
        QuerydslPredicateExecutor<DowntimeEvent>
{

    // Основные методы поиска
    List<DowntimeEvent> findByEquipmentId(String equipmentId);

    List<DowntimeEvent> findByEquipmentIdAndStatus(String equipmentId, DowntimeStatus status);

    List<DowntimeEvent> findByStatus(DowntimeStatus status);

    List<DowntimeEvent> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    List<DowntimeEvent> findByOperatorId(String operatorId);

    default List<DowntimeEvent> findByOperatorIdSorted(String operatorId) {
        QDowntimeEvent q = QDowntimeEvent.downtimeEvent;
        Predicate predicate = q.operatorId.eq(operatorId);
        return (List<DowntimeEvent>) findAll(predicate, q.startTime.desc());
    }

    Optional<DowntimeEvent> findByIdAndOperatorId(Long id, String operatorId);

    boolean existsByEquipmentIdAndStatus(String equipmentId, DowntimeStatus status);

    long countByStatus(DowntimeStatus status);

    long countByStartTimeGreaterThanEqual(LocalDateTime dateTime);

    // Дополнительные методы для улучшения производительности
    List<DowntimeEvent> findByEquipmentIdAndStartTimeAfter(String equipmentId, LocalDateTime startTime);

    List<DowntimeEvent> findByEquipmentIdAndStartTimeBefore(String equipmentId, LocalDateTime endTime);

    // Методы для поиска по диапазону дат и статусу
    List<DowntimeEvent> findByStatusAndStartTimeBetween(DowntimeStatus status, LocalDateTime start, LocalDateTime end);

    List<DowntimeEvent> findByEquipmentIdAndStatusAndStartTimeBetween(
            String equipmentId,
            DowntimeStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    // Удаление по оборудованию
    void deleteByEquipmentId(String equipmentId);

    // Поиск активных простоев для определенного оборудования
    @Query("{ 'equipmentId': ?0, 'status': 'ACTIVE' }")
    List<DowntimeEvent> findActiveDowntimesByEquipment(String equipmentId);

    // Поиск последнего простоя для оборудования
    @Query(value = "{ 'equipmentId': ?0 }", sort = "{ 'startTime': -1 }")
    List<DowntimeEvent> findLatestByEquipmentId(String equipmentId, org.springframework.data.domain.Pageable pageable);

    // Подсчет простоев по оборудованию
    long countByEquipmentId(String equipmentId);

    // Подсчет простоев по оператору
    long countByOperatorId(String operatorId);

    // Проверка существования простоя с определенным ID и оборудованием
    boolean existsByIdAndEquipmentId(Long id, String equipmentId);

    // Поиск по нескольким статусам
    List<DowntimeEvent> findByStatusIn(List<DowntimeStatus> statuses);

    // Поиск по причине (частичное совпадение)
    @Query("{ 'reason': { $regex: ?0, $options: 'i' } }")
    List<DowntimeEvent> findByReasonContainingIgnoreCase(String reason);

    // Агрегационные методы для статистики
    @Query(value = "{}", fields = "{ 'equipmentId': 1, 'equipmentName': 1 }")
    List<DowntimeEvent> findDistinctEquipment();

    // Метод для поиска простоев с фото
    @Query("{ 'photoUrls': { $exists: true, $not: { $size: 0 } } }")
    List<DowntimeEvent> findDowntimesWithPhotos();

    // Метод для поиска по комментарию
    @Query("{ 'comment': { $regex: ?0, $options: 'i' } }")
    List<DowntimeEvent> findByCommentContaining(String searchText);
}
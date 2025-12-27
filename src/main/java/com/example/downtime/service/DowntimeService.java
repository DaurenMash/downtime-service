package com.example.downtime.service;

import com.example.downtime.model.DowntimeEvent;
import com.example.downtime.model.DowntimeRequest;
import com.example.downtime.model.DowntimeResponse;
import com.example.downtime.model.DowntimeStatus;
import com.example.downtime.repository.DowntimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DowntimeService {

    private final DowntimeRepository downtimeRepository;
    private final MongoTemplate mongoTemplate;
    private final SequenceGeneratorService sequenceGenerator;

    // ========== СОЗДАНИЕ ПРОСТОЯ ==========

    @Transactional
    public DowntimeResponse createDowntime(DowntimeRequest request) {
        log.info("СОЗДАНИЕ ПРОСТОЯ - Сервис начал работу");
        log.info("Получен запрос: {}", request);

        // ID теперь генерируется автоматически через Listener
        DowntimeEvent event = DowntimeEvent.builder()
                .equipmentId(request.getEquipmentId())
                .equipmentName(request.getEquipmentName())
                .operatorId(request.getOperatorId())
                .operatorName(request.getOperatorName())
                .startTime(request.getStartTime() != null ?
                        request.getStartTime() : LocalDateTime.now())
                .endTime(request.getEndTime())
                .reason(request.getReason())
                .comment(request.getComment())
                .status(DowntimeStatus.ACTIVE)
                .photoUrls(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        log.info("Событие создано (до сохранения): {}", event);

        // Сохраняем - ID сгенерируется автоматически в Listener
        DowntimeEvent saved = downtimeRepository.save(event);
        log.info("Событие сохранено в БД: {}", saved);
        log.info("ID сохраненного события: {}", saved.getId());

        return mapToResponse(saved);
    }

    // ========== ПОЛУЧЕНИЕ ПРОСТОЯ ПО ID ==========

    public DowntimeResponse getDowntime(Long id) {
        log.debug("Получение простоя по ID (Long): {}", id);
        DowntimeEvent event = findEventById(id);
        return mapToResponse(event);
    }

    // Для обратной совместимости
    public DowntimeResponse getDowntime(String id) {
        log.debug("Получение простоя по ID (String): {}", id);
        try {
            Long numericId = Long.parseLong(id);
            return getDowntime(numericId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный формат ID. Ожидается числовой ID: " + id);
        }
    }

    // ========== ПОИСК И ФИЛЬТРАЦИЯ ==========

    public List<DowntimeResponse> getDowntimesByEquipment(String equipmentId) {
        log.debug("Получение простоев для оборудования: {}", equipmentId);
        return downtimeRepository.findByEquipmentId(equipmentId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<DowntimeResponse> getActiveDowntimes() {
        log.debug("Получение активных простоев");
        return downtimeRepository.findByStatus(DowntimeStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<DowntimeResponse> getDowntimesByOperator(String operatorId) {
        log.debug("Получение простоев оператора: {}", operatorId);
        return downtimeRepository.findByOperatorId(operatorId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Page<DowntimeResponse> getFilteredDowntimes(
            String equipmentId,
            DowntimeStatus status,
            String operator,
            LocalDate dateFrom,
            Pageable pageable) {

        log.debug("Фильтрация простоев: equipmentId={}, status={}, operator={}, dateFrom={}",
                equipmentId, status, operator, dateFrom);

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (equipmentId != null && !equipmentId.trim().isEmpty()) {
            criteriaList.add(Criteria.where("equipmentId").is(equipmentId.trim()));
        }

        if (status != null) {
            criteriaList.add(Criteria.where("status").is(status));
        }

        if (operator != null && !operator.trim().isEmpty()) {
            criteriaList.add(Criteria.where("operatorId").is(operator.trim()));
        }

        if (dateFrom != null) {
            criteriaList.add(Criteria.where("startTime").gte(dateFrom.atStartOfDay()));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        // Настройка сортировки и пагинации
        pageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.DESC, "startTime"))
        );

        long total = mongoTemplate.count(query, DowntimeEvent.class);
        query.with(pageable);

        List<DowntimeEvent> events = mongoTemplate.find(query, DowntimeEvent.class);
        List<DowntimeResponse> responses = events.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, total);
    }

    // ========== ОПЕРАЦИИ С ФОТО ==========

    @Transactional
    public DowntimeResponse addPhotoToDowntime(Long downtimeId, String photoUrl) {
        log.info("Добавление фото к простою: {}", downtimeId);
        DowntimeEvent event = findEventById(downtimeId);

        if (event.getPhotoUrls() == null) {
            event.setPhotoUrls(new ArrayList<>());
        }

        if (!event.getPhotoUrls().contains(photoUrl)) {
            event.getPhotoUrls().add(photoUrl);
            event.setUpdatedAt(LocalDateTime.now());
            DowntimeEvent updated = downtimeRepository.save(event);
            log.info("Фото добавлено к простою: {}", downtimeId);
            return mapToResponse(updated);
        } else {
            log.warn("Фото уже существует для простоя: {}", downtimeId);
            return mapToResponse(event);
        }
    }

    // Для обратной совместимости
    public DowntimeResponse addPhotoToDowntime(String downtimeId, String photoUrl) {
        try {
            Long id = Long.parseLong(downtimeId);
            return addPhotoToDowntime(id, photoUrl);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный формат ID: " + downtimeId);
        }
    }

    // ========== ЗАКРЫТИЕ ПРОСТОЯ ==========

    @Transactional
    public DowntimeResponse resolveDowntime(Long id, String resolutionComment) {
        log.info("Закрытие простоя: {}", id);
        DowntimeEvent event = findEventById(id);

        if (event.getStatus() == DowntimeStatus.RESOLVED) {
            throw new IllegalStateException("Простой уже закрыт");
        }

        // Обновляем поля
        event.setStatus(DowntimeStatus.RESOLVED);
        event.setEndTime(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());

        // Добавляем комментарий решения
        if (resolutionComment != null && !resolutionComment.trim().isEmpty()) {
            StringBuilder commentBuilder = new StringBuilder();
            if (event.getComment() != null && !event.getComment().isEmpty()) {
                commentBuilder.append(event.getComment()).append("\n\n");
            }
            commentBuilder.append("=== РЕШЕНИЕ ===\n")
                    .append(resolutionComment.trim())
                    .append("\nДата решения: ")
                    .append(LocalDateTime.now());
            event.setComment(commentBuilder.toString());
        }

        DowntimeEvent updated = downtimeRepository.save(event);
        log.info("Простой {} закрыт", id);
        return mapToResponse(updated);
    }

    // Для обратной совместимости
    public DowntimeResponse resolveDowntime(String id, String resolutionComment) {
        try {
            Long numericId = Long.parseLong(id);
            return resolveDowntime(numericId, resolutionComment);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный формат ID: " + id);
        }
    }

    // ========== СТАТИСТИКА И АНАЛИТИКА ==========

    public long countByStatus(DowntimeStatus status) {
        return downtimeRepository.countByStatus(status);
    }

    public long countToday() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        List<DowntimeEvent> todayEvents = downtimeRepository.findByStartTimeBetween(startOfDay, endOfDay);
        return todayEvents.size();
    }

    public String getAverageDuration() {
        List<DowntimeEvent> resolvedEvents = downtimeRepository.findByStatus(DowntimeStatus.RESOLVED);

        if (resolvedEvents.isEmpty()) {
            return "0ч 0м";
        }

        long totalMinutes = resolvedEvents.stream()
                .filter(event -> event.getEndTime() != null)
                .mapToLong(event -> ChronoUnit.MINUTES.between(event.getStartTime(), event.getEndTime()))
                .sum();

        if (totalMinutes == 0) {
            return "0ч 0м";
        }

        long avgMinutes = totalMinutes / resolvedEvents.size();
        long hours = avgMinutes / 60;
        long minutes = avgMinutes % 60;

        return String.format("%dч %02dм", hours, minutes);
    }

    public long countTotalPhotos() {
        List<DowntimeEvent> allEvents = downtimeRepository.findAll();
        return allEvents.stream()
                .filter(event -> event.getPhotoUrls() != null)
                .mapToInt(event -> event.getPhotoUrls().size())
                .sum();
    }

    public List<Map<String, String>> getAllEquipment() {
        try {
            Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.group("equipmentId", "equipmentName"),
                    Aggregation.sort(Sort.Direction.ASC, "equipmentId")
            );

            AggregationResults<Map> results = mongoTemplate.aggregate(
                    aggregation, "downtime_events", Map.class);

            return results.getMappedResults().stream()
                    .map(item -> {
                        Map<String, String> equipment = new HashMap<>();
                        Map<?, ?> idMap = (Map<?, ?>) item.get("_id");
                        equipment.put("id", String.valueOf(idMap.get("equipmentId")));
                        equipment.put("name", String.valueOf(idMap.get("equipmentName")));
                        return equipment;
                    })
                    .filter(equipment -> equipment.get("id") != null &&
                            !"null".equals(equipment.get("id")) &&
                            equipment.get("name") != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Ошибка при агрегации оборудования, используем fallback метод", e);

            return downtimeRepository.findAll().stream()
                    .collect(Collectors.toMap(
                            DowntimeEvent::getEquipmentId,
                            DowntimeEvent::getEquipmentName,
                            (existing, replacement) -> existing
                    ))
                    .entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> {
                        Map<String, String> map = new HashMap<>();
                        map.put("id", entry.getKey());
                        map.put("name", entry.getValue());
                        return map;
                    })
                    .collect(Collectors.toList());
        }
    }

    // ========== ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ ==========

    public boolean hasActiveDowntime(String equipmentId) {
        return downtimeRepository.existsByEquipmentIdAndStatus(
                equipmentId, DowntimeStatus.ACTIVE);
    }

    public Map<String, Object> getStatistics(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(23, 59, 59);

        List<DowntimeEvent> events = downtimeRepository.findByStartTimeBetween(start, end);

        Map<String, Object> stats = new HashMap<>();
        stats.put("period", from + " - " + to);
        stats.put("totalEvents", events.size());

        Map<DowntimeStatus, Long> statusStats = events.stream()
                .collect(Collectors.groupingBy(
                        DowntimeEvent::getStatus,
                        Collectors.counting()
                ));
        stats.put("byStatus", statusStats);

        long totalDuration = events.stream()
                .filter(event -> event.getEndTime() != null)
                .mapToLong(event -> ChronoUnit.MINUTES.between(event.getStartTime(), event.getEndTime()))
                .sum();
        stats.put("totalDurationMinutes", totalDuration);

        Map<String, Long> equipmentStats = events.stream()
                .collect(Collectors.groupingBy(
                        DowntimeEvent::getEquipmentName,
                        Collectors.counting()
                ));
        stats.put("byEquipment", equipmentStats);

        return stats;
    }

    @Transactional
    public void deleteDowntime(Long id) {
        log.info("Удаление простоя: {}", id);
        if (!downtimeRepository.existsById(id)) {
            throw new IllegalArgumentException("Простой не найден: " + id);
        }
        downtimeRepository.deleteById(id);
        log.info("Простой {} удален", id);
    }

    // Для обратной совместимости
    public void deleteDowntime(String id) {
        try {
            Long numericId = Long.parseLong(id);
            deleteDowntime(numericId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный формат ID: " + id);
        }
    }

    public DowntimeEvent getDowntimeEntity(Long id) {
        return findEventById(id);
    }

    // Для обратной совместимости
    public DowntimeEvent getDowntimeEntity(String id) {
        try {
            Long numericId = Long.parseLong(id);
            return findEventById(numericId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный формат ID: " + id);
        }
    }

    // ========== ПРИВАТНЫЕ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private DowntimeEvent findEventById(Long id) {
        return downtimeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Простой не найден с ID: " + id));
    }

    private DowntimeResponse mapToResponse(DowntimeEvent event) {
        Long durationMinutes = null;
        if (event.getEndTime() != null) {
            durationMinutes = ChronoUnit.MINUTES.between(event.getStartTime(), event.getEndTime());
        }

        return DowntimeResponse.builder()
                .id(event.getId()) // Конвертируем Long в String для фронтенда
                .equipmentId(event.getEquipmentId())
                .equipmentName(event.getEquipmentName())
                .operatorId(event.getOperatorId())
                .operatorName(event.getOperatorName())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .reason(event.getReason())
                .comment(event.getComment())
                .photoUrls(event.getPhotoUrls() != null ?
                        new ArrayList<>(event.getPhotoUrls()) : new ArrayList<>())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .durationMinutes(durationMinutes)
                .withCalculatedFields()
                .build();
    }

    private String formatDuration(Long minutes) {
        if (minutes == null) {
            return "в процессе";
        }

        long hours = minutes / 60;
        long mins = minutes % 60;

        if (hours > 0) {
            return String.format("%dч %02dм", hours, mins);
        } else {
            return String.format("%dм", mins);
        }
    }
}
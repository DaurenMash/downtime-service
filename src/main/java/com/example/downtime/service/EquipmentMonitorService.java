package com.example.downtime.service;

import com.example.downtime.model.EquipmentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EquipmentMonitorService {

    // ConcurrentHashMap для хранения статусов оборудования (потокобезопасная)
    private final ConcurrentHashMap<String, EquipmentStatus> equipmentStatusMap = new ConcurrentHashMap<>();

    // Пул потоков для имитации работы оборудования
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private final List<Future<?>> runningTasks = new CopyOnWriteArrayList<>();

    // Счетчик для статистики
    private final AtomicInteger totalStatusChanges = new AtomicInteger(0);

    // Статичный список оборудования
    private static final List<EquipmentStatus> STATIC_EQUIPMENT_LIST = Arrays.asList(
            EquipmentStatus.builder()
                    .equipmentId("EQ001")
                    .equipmentName("Токарный станок ЧПУ")
                    .currentStatus(EquipmentStatus.Status.WORKING)
                    .statusChangedAt(LocalDateTime.now())
                    .uptimeMinutes(0L)
                    .downtimeMinutes(0L)
                    .statusChangesCount(0)
                    .build(),

            EquipmentStatus.builder()
                    .equipmentId("EQ002")
                    .equipmentName("Фрезерный станок")
                    .currentStatus(EquipmentStatus.Status.WORKING)
                    .statusChangedAt(LocalDateTime.now())
                    .uptimeMinutes(0L)
                    .downtimeMinutes(0L)
                    .statusChangesCount(0)
                    .build(),

            EquipmentStatus.builder()
                    .equipmentId("EQ003")
                    .equipmentName("Сварочный аппарат")
                    .currentStatus(EquipmentStatus.Status.DOWNTIME)
                    .statusChangedAt(LocalDateTime.now())
                    .uptimeMinutes(0L)
                    .downtimeMinutes(0L)
                    .statusChangesCount(0)
                    .build(),

            EquipmentStatus.builder()
                    .equipmentId("EQ004")
                    .equipmentName("Пресс-форма")
                    .currentStatus(EquipmentStatus.Status.WORKING)
                    .statusChangedAt(LocalDateTime.now())
                    .uptimeMinutes(0L)
                    .downtimeMinutes(0L)
                    .statusChangesCount(0)
                    .build(),

            EquipmentStatus.builder()
                    .equipmentId("EQ005")
                    .equipmentName("Конвейерная линия")
                    .currentStatus(EquipmentStatus.Status.DOWNTIME)
                    .statusChangedAt(LocalDateTime.now())
                    .uptimeMinutes(0L)
                    .downtimeMinutes(0L)
                    .statusChangesCount(0)
                    .build()
    );

    @PostConstruct
    public void init() {
        log.info("Инициализация сервиса мониторинга оборудования...");

        // Инициализируем карту статичным оборудованием
        for (EquipmentStatus equipment : STATIC_EQUIPMENT_LIST) {
            equipmentStatusMap.put(equipment.getEquipmentId(), equipment);
        }

        log.info("Загружено {} единиц оборудования", equipmentStatusMap.size());

        // Запускаем мониторинг для каждого оборудования в отдельном потоке
        for (EquipmentStatus equipment : STATIC_EQUIPMENT_LIST) {
            startMonitoringEquipment(equipment.getEquipmentId());
        }

        // Запускаем сбор статистики каждые 30 секунд
        scheduler.scheduleAtFixedRate(this::logStatistics, 30, 30, TimeUnit.SECONDS);
    }

    private void startMonitoringEquipment(String equipmentId) {
        // Каждое оборудование обрабатывается в отдельном потоке
        Runnable monitoringTask = () -> {
            log.info("Запущен мониторинг оборудования: {}", equipmentId);

            Random random = new Random();

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Имитация случайной смены статуса (раз в 5-15 секунд)
                    int delay = 5000 + random.nextInt(10000);
                    Thread.sleep(delay);

                    // Меняем статус оборудования
                    changeEquipmentStatus(equipmentId);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("Мониторинг оборудования {} остановлен", equipmentId);
                    break;
                } catch (Exception e) {
                    log.error("Ошибка в мониторинге оборудования {}: {}", equipmentId, e.getMessage());
                }
            }
        };

        Future<?> taskFuture = scheduler.submit(monitoringTask);
        runningTasks.add(taskFuture);
    }

    /**
     * Потокобезопасное изменение статуса оборудования
     */
    public void changeEquipmentStatus(String equipmentId) {
        equipmentStatusMap.computeIfPresent(equipmentId, (id, status) -> {
            // Сохраняем предыдущий статус для расчета времени
            EquipmentStatus.Status previousStatus = status.getCurrentStatus();
            LocalDateTime previousChangeTime = status.getStatusChangedAt();

            // Определяем новый статус
            EquipmentStatus.Status newStatus = (previousStatus == EquipmentStatus.Status.WORKING)
                    ? EquipmentStatus.Status.DOWNTIME
                    : EquipmentStatus.Status.WORKING;

            // Рассчитываем время в предыдущем статусе
            long minutesInPreviousStatus = ChronoUnit.MINUTES.between(
                    previousChangeTime, LocalDateTime.now());

            // Обновляем счетчики
            if (previousStatus == EquipmentStatus.Status.WORKING) {
                status.setUptimeMinutes(status.getUptimeMinutes() + minutesInPreviousStatus);
            } else {
                status.setDowntimeMinutes(status.getDowntimeMinutes() + minutesInPreviousStatus);
            }

            // Обновляем статус
            status.setCurrentStatus(newStatus);
            status.setStatusChangedAt(LocalDateTime.now());
            status.setStatusChangesCount(status.getStatusChangesCount() + 1);

            totalStatusChanges.incrementAndGet();

            log.debug("Оборудование {} сменило статус: {} -> {}",
                    equipmentId, previousStatus.getDisplayName(), newStatus.getDisplayName());

            return status;
        });
    }

    /**
     * Получение всех статусов оборудования (потокобезопасно)
     */
    public List<EquipmentStatus> getAllEquipmentStatuses() {
        return new ArrayList<>(equipmentStatusMap.values());
    }

    /**
     * Получение статуса конкретного оборудования
     */
    public EquipmentStatus getEquipmentStatus(String equipmentId) {
        return equipmentStatusMap.get(equipmentId);
    }

    /**
     * Принудительная смена статуса (для ручного управления)
     */
    public EquipmentStatus setEquipmentStatus(String equipmentId, EquipmentStatus.Status newStatus) {
        return equipmentStatusMap.computeIfPresent(equipmentId, (id, status) -> {
            // Рассчитываем время в предыдущем статусе
            long minutesInPreviousStatus = ChronoUnit.MINUTES.between(
                    status.getStatusChangedAt(), LocalDateTime.now());

            if (status.getCurrentStatus() == EquipmentStatus.Status.WORKING) {
                status.setUptimeMinutes(status.getUptimeMinutes() + minutesInPreviousStatus);
            } else {
                status.setDowntimeMinutes(status.getDowntimeMinutes() + minutesInPreviousStatus);
            }

            status.setCurrentStatus(newStatus);
            status.setStatusChangedAt(LocalDateTime.now());
            status.setStatusChangesCount(status.getStatusChangesCount() + 1);

            return status;
        });
    }

    /**
     * Получение статистики по всем оборудованию
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<EquipmentStatus> allStatuses = getAllEquipmentStatuses();

        long totalWorking = allStatuses.stream()
                .filter(s -> s.getCurrentStatus() == EquipmentStatus.Status.WORKING)
                .count();

        long totalDowntime = allStatuses.stream()
                .filter(s -> s.getCurrentStatus() == EquipmentStatus.Status.DOWNTIME)
                .count();

        long totalUptime = allStatuses.stream()
                .mapToLong(EquipmentStatus::getUptimeMinutes)
                .sum();

        long totalDowntimeMinutes = allStatuses.stream()
                .mapToLong(EquipmentStatus::getDowntimeMinutes)
                .sum();

        int totalChanges = allStatuses.stream()
                .mapToInt(EquipmentStatus::getStatusChangesCount)
                .sum();

        stats.put("totalEquipment", allStatuses.size());
        stats.put("workingNow", totalWorking);
        stats.put("downtimeNow", totalDowntime);
        stats.put("totalUptimeMinutes", totalUptime);
        stats.put("totalDowntimeMinutes", totalDowntimeMinutes);
        stats.put("totalStatusChanges", totalChanges);
        stats.put("monitorChanges", totalStatusChanges.get());
        stats.put("lastUpdate", LocalDateTime.now());

        return stats;
    }

    private void logStatistics() {
        Map<String, Object> stats = getStatistics();
        log.info("=== СТАТИСТИКА МОНИТОРИНГА ===");
        log.info("Оборудование всего: {}", stats.get("totalEquipment"));
        log.info("Работает сейчас: {}", stats.get("workingNow"));
        log.info("В простое сейчас: {}", stats.get("downtimeNow"));
        log.info("Всего смен статусов: {}", stats.get("totalStatusChanges"));
        log.info("============================");
    }

    @PreDestroy
    public void shutdown() {
        log.info("Остановка сервиса мониторинга оборудования...");

        // Останавливаем все задачи
        for (Future<?> task : runningTasks) {
            task.cancel(true);
        }

        // Завершаем scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }

        log.info("Сервис мониторинга остановлен");
    }
}

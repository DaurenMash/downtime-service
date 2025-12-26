package com.example.downtime.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Не включать null поля в JSON
public class DowntimeResponse {

    private Long id;
    private String equipmentId;
    private String equipmentName;
    private String operatorId;
    private String operatorName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    private String reason;
    private String comment;
    private List<String> photoUrls;
    private DowntimeStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    // ========== ВЫЧИСЛЯЕМЫЕ ПОЛЯ ==========

    // Продолжительность в минутах (null если еще не завершен)
    private Long durationMinutes;

    // Форматированная продолжительность (например, "2ч 15м", "45м", "в процессе")
    private String durationFormatted;

    // Является ли простой активным (удобно для фронтенда)
    private Boolean isActive;

    // Простой длился более 1 часа? (для стилизации)
    private Boolean isLongDowntime;

    // Количество фотографий (удобно для отображения)
    private Integer photoCount;

    // Текущая продолжительность активного простоя (в минутах)
    private Long currentDurationMinutes;

    // Форматированная текущая продолжительность
    private String currentDurationFormatted;

    // ========== СТАТУСНЫЕ ПОЛЯ ==========

    // Цвет для отображения статуса (CSS класс)
    private String statusColor;

    // Иконка статуса
    private String statusIcon;

    // Текст статуса для отображения
    private String statusText;

    // ========== МЕТОДЫ ДЛЯ УДОБСТВА ==========

    // Можно добавить метод для вычисления полей
    public void calculateDerivedFields() {
        // Вычисляем количество фото
        this.photoCount = this.photoUrls != null ? this.photoUrls.size() : 0;

        // Статус активности
        this.isActive = this.status == DowntimeStatus.ACTIVE;

        // Текст статуса
        this.statusText = getStatusDisplayText();

        // Цвет статуса
        this.statusColor = getStatusColor();

        // Иконка статуса
        this.statusIcon = getStatusIcon();

        // Вычисляем продолжительность
        calculateDurations();
    }

    private void calculateDurations() {
        if (this.endTime != null && this.startTime != null) {
            // Для завершенных простоев
            this.durationMinutes = java.time.Duration.between(this.startTime, this.endTime).toMinutes();
            this.durationFormatted = formatDuration(this.durationMinutes);
            this.isLongDowntime = this.durationMinutes > 60;
            this.currentDurationMinutes = null;
            this.currentDurationFormatted = null;
        } else if (this.isActive && this.startTime != null) {
            // Для активных простоев
            this.currentDurationMinutes = java.time.Duration.between(this.startTime, LocalDateTime.now()).toMinutes();
            this.currentDurationFormatted = formatDuration(this.currentDurationMinutes);
            this.isLongDowntime = this.currentDurationMinutes > 60;
            this.durationMinutes = null;
            this.durationFormatted = null;
        } else {
            this.durationMinutes = null;
            this.durationFormatted = null;
            this.currentDurationMinutes = null;
            this.currentDurationFormatted = null;
            this.isLongDowntime = false;
        }
    }

    private String formatDuration(long minutes) {
        long hours = minutes / 60;
        long mins = minutes % 60;

        if (hours > 0 && mins > 0) {
            return String.format("%dч %02dм", hours, mins);
        } else if (hours > 0) {
            return String.format("%dч", hours);
        } else {
            return String.format("%dм", mins);
        }
    }

    private String getStatusDisplayText() {
        return switch (this.status) {
            case ACTIVE -> "Активен";
            case RESOLVED -> "Решен";
            case UNPLANNED -> "Внеплановый";
            case PLANNED -> "Плановый";
            default -> this.status.toString();
        };
    }

    private String getStatusColor() {
        return switch (this.status) {
            case ACTIVE -> "danger";    // красный
            case RESOLVED -> "success"; // зеленый
            case UNPLANNED -> "warning"; // желтый
            case PLANNED -> "info";     // синий
            default -> "secondary";
        };
    }

    private String getStatusIcon() {
        return switch (this.status) {
            case ACTIVE -> "⏱️";
            case RESOLVED -> "✅";
            case UNPLANNED -> "⚠️";
            case PLANNED -> "📅";
            default -> "📊";
        };
    }

    public static class DowntimeResponseBuilder {


        public DowntimeResponseBuilder withCalculatedFields() {
            // Создаем временный объект для вычислений
            DowntimeResponse response = this.build();
            response.calculateDerivedFields();

            // Возвращаем значения в билдер
            return this
                    .durationMinutes(response.durationMinutes)
                    .durationFormatted(response.durationFormatted)
                    .isActive(response.isActive)
                    .isLongDowntime(response.isLongDowntime)
                    .photoCount(response.photoCount)
                    .currentDurationMinutes(response.currentDurationMinutes)
                    .currentDurationFormatted(response.currentDurationFormatted)
                    .statusColor(response.statusColor)
                    .statusIcon(response.statusIcon)
                    .statusText(response.statusText);
        }
    }
}
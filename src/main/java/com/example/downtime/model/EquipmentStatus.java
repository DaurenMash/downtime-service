package com.example.downtime.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentStatus {
    private String equipmentId;
    private String equipmentName;
    private Status currentStatus;
    private LocalDateTime statusChangedAt;
    private Long uptimeMinutes;
    private Long downtimeMinutes;
    private Integer statusChangesCount;

    public enum Status {
        WORKING("Работает", "success"),
        DOWNTIME("Простой", "danger");

        private final String displayName;
        private final String color;

        Status(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
    }
}

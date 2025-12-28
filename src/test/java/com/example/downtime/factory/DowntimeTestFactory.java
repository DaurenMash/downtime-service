package com.example.downtime.factory;

import com.example.downtime.model.DowntimeEvent;
import com.example.downtime.model.DowntimeRequest;
import com.example.downtime.model.DowntimeStatus;

import java.time.LocalDateTime;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public class DowntimeTestFactory {

    private static final LocalDateTime FIXED_NOW =
            LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    public static DowntimeRequest createRequestWithDefaults() {
        return DowntimeRequest.builder()
                .equipmentId("EQ-001")
                .equipmentName("Default Equipment")
                .operatorId("OP-001")
                .operatorName("Default Operator")
                .startTime(FIXED_NOW)
                .endTime(FIXED_NOW.plusHours(1))
                .reason("Default Reason")
                .comment("Default Comment")
                .build();
    }

    public static DowntimeEvent createEventFromRequest(DowntimeRequest request) {
        return DowntimeEvent.builder()
                .id(0L)
                .equipmentId(request.getEquipmentId())
                .equipmentName(request.getEquipmentName())
                .operatorId(request.getOperatorId())
                .operatorName(request.getOperatorName())
                .startTime(FIXED_NOW)
                .endTime(FIXED_NOW.plusHours(1))
                .reason(request.getReason())
                .comment(request.getComment())
                .status(DowntimeStatus.ACTIVE)
                .photoUrls(new ArrayList<>())
                .createdAt(FIXED_NOW)
                .updatedAt(FIXED_NOW)
                .build();
    }

    // Дополнительный метод для создания события, которое возвращается из save()
    public static DowntimeEvent createSavedEventFromRequest(DowntimeRequest request) {
        return DowntimeEvent.builder()
                .id(0L) // После сохранения появляется ID
                .equipmentId(request.getEquipmentId())
                .equipmentName(request.getEquipmentName())
                .operatorId(request.getOperatorId())
                .operatorName(request.getOperatorName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .reason(request.getReason())
                .comment(request.getComment())
                .status(DowntimeStatus.ACTIVE)
                .photoUrls(new ArrayList<>())
                .createdAt(FIXED_NOW)
                .updatedAt(FIXED_NOW)
                .build();
    }
}
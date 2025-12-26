package com.example.downtime.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DowntimeRequest {

    @NotBlank(message = "Equipment ID is required")
    private String equipmentId;

    private String equipmentName;

    @NotBlank(message = "Operator ID is required")
    private String operatorId;

    private String operatorName;

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String comment;
}

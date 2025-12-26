package com.example.downtime.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "downtime_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DowntimeEvent {

    @Id
    private Long id;

    private String equipmentId;

    private String equipmentName;

    private String operatorId;

    private String operatorName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String reason;

    private String comment;

    @Builder.Default
    private List<String> photoUrls = new ArrayList<>();

    @Builder.Default
    private DowntimeStatus status = DowntimeStatus.ACTIVE;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Transient  // Это поле не сохраняется в БД
    public static final String SEQUENCE_NAME = "downtime_events_sequence";
}


package com.example.downtime.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "settings")
public class Settings {

    @Id
    private String id;

    private String key;
    private String value;
    private String description;
    private String category;
    private LocalDateTime updatedAt;

    // Конструктор по умолчанию
    public Settings() {
        this.updatedAt = LocalDateTime.now();
    }

    // Конструктор с ключом и значением
    public Settings(String key, String value) {
        this.key = key;
        this.value = value;
        this.updatedAt = LocalDateTime.now();
    }

    public Settings(String key, String value, String description, String category) {
        this.key = key;
        this.value = value;
        this.description = description;
        this.category = category;
        this.updatedAt = LocalDateTime.now();
    }
}
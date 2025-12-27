package com.example.downtime.service;

import com.example.downtime.model.Settings;
import com.example.downtime.repository.SettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SettingsRepository settingsRepository;

    // Значения по умолчанию
    private static final Map<String, String> DEFAULT_SETTINGS = new HashMap<>();

    static {
        // Автообновление
        DEFAULT_SETTINGS.put("refresh.interval", "30");
        DEFAULT_SETTINGS.put("refresh.enabled", "true");

        // Фотографии
        DEFAULT_SETTINGS.put("photos.max.file.size", "10"); // MB
        DEFAULT_SETTINGS.put("photos.compress.enabled", "true");
        DEFAULT_SETTINGS.put("photos.allowed.extensions", "jpg,jpeg,png,gif");

        // Уведомления
        DEFAULT_SETTINGS.put("notifications.email.enabled", "true");
        DEFAULT_SETTINGS.put("notifications.email.recipients", "");
        DEFAULT_SETTINGS.put("notifications.active.downtime.enabled", "true");

        // Экспорт
        DEFAULT_SETTINGS.put("export.default.format", "excel");
        DEFAULT_SETTINGS.put("export.include.photos", "false");

        // Интерфейс
        DEFAULT_SETTINGS.put("ui.items.per.page", "10");
        DEFAULT_SETTINGS.put("ui.show.filters.by.default", "true");
    }

    /**
     * Инициализация настроек по умолчанию при первом запуске
     */
    public void initializeDefaultSettings() {
        log.info("Проверка инициализации настроек по умолчанию");

        DEFAULT_SETTINGS.forEach((key, defaultValue) -> {
            if (!settingsRepository.existsByKey(key)) {
                Settings setting = new Settings(
                        key,
                        defaultValue,
                        getDescription(key),
                        getCategory(key)
                );
                settingsRepository.save(setting);
                log.info("Создана настройка по умолчанию: {} = {}", key, defaultValue);
            }
        });
    }

    /**
     * Получение значения настройки
     */
    public String getSetting(String key) {
        return settingsRepository.findByKey(key)
                .map(Settings::getValue)
                .orElseGet(() -> DEFAULT_SETTINGS.getOrDefault(key, ""));
    }

    /**
     * Получение значения настройки с типом Integer
     */
    public Integer getIntSetting(String key) {
        String value = getSetting(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Невозможно преобразовать настройку {} в число: {}", key, value);
            return 0;
        }
    }

    /**
     * Получение значения настройки с типом Boolean
     */
    public Boolean getBooleanSetting(String key) {
        String value = getSetting(key);
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    /**
     * Сохранение настройки
     */
    public void saveSetting(String key, String value) {
        Optional<Settings> existingSetting = settingsRepository.findByKey(key);

        Settings setting;
        if (existingSetting.isPresent()) {
            setting = existingSetting.get();
            setting.setValue(value);
        } else {
            setting = new Settings(
                    key,
                    value,
                    getDescription(key),
                    getCategory(key)
            );
        }

        setting.setUpdatedAt(LocalDateTime.now());
        settingsRepository.save(setting);
        log.info("Сохранена настройка: {} = {}", key, value);
    }

    /**
     * Сохранение нескольких настроек
     */
    public void saveSettings(Map<String, String> settings) {
        settings.forEach(this::saveSetting);
    }

    /**
     * Получение всех настроек
     */
    public List<Settings> getAllSettings() {
        return settingsRepository.findAll();
    }

    /**
     * Получение настроек по категории
     */
    public List<Settings> getSettingsByCategory(String category) {
        return settingsRepository.findByCategory(category);
    }

    /**
     * Получение всех настроек в виде Map
     */
    public Map<String, String> getAllSettingsAsMap() {
        Map<String, String> settingsMap = new HashMap<>();
        getAllSettings().forEach(setting ->
                settingsMap.put(setting.getKey(), setting.getValue()));
        return settingsMap;
    }

    /**
     * Удаление настройки
     */
    public void deleteSetting(String key) {
        settingsRepository.deleteByKey(key);
        log.info("Удалена настройка: {}", key);
    }

    /**
     * Сброс к значениям по умолчанию
     */
    public void resetToDefaults() {
        DEFAULT_SETTINGS.forEach(this::saveSetting);
        log.info("Настройки сброшены к значениям по умолчанию");
    }

    // Вспомогательные методы для определения категорий и описаний
    private String getCategory(String key) {
        if (key.startsWith("refresh.")) return "autorefresh";
        if (key.startsWith("photos.")) return "photos";
        if (key.startsWith("notifications.")) return "notifications";
        if (key.startsWith("export.")) return "export";
        if (key.startsWith("ui.")) return "ui";
        return "general";
    }

    private String getDescription(String key) {
        switch (key) {
            case "refresh.interval":
                return "Интервал автообновления в секундах";
            case "refresh.enabled":
                return "Включить автообновление";
            case "photos.max.file.size":
                return "Максимальный размер файла в MB";
            case "photos.compress.enabled":
                return "Сжимать изображения перед загрузкой";
            case "notifications.email.enabled":
                return "Включить email уведомления";
            case "ui.items.per.page":
                return "Количество элементов на странице";
            default:
                return "Настройка системы";
        }
    }
}

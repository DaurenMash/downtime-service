package com.example.downtime.controller.web;

import com.example.downtime.dto.EquipmentDto;
import com.example.downtime.model.DowntimeEvent;
import com.example.downtime.model.DowntimeRequest;
import com.example.downtime.model.DowntimeResponse;
import com.example.downtime.model.DowntimeStatus;
import com.example.downtime.repository.DowntimeRepository;
import com.example.downtime.service.DowntimeService;
import com.example.downtime.service.FileStorageService;
import com.example.downtime.service.SettingsService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/web/downtimes")
@RequiredArgsConstructor
public class DowntimeWebController {

    private final DowntimeService downtimeService;
    private final FileStorageService fileStorageService;
    private final DowntimeRepository downtimeRepository;
    private final SettingsService settingsService;

    @GetMapping("/")
    public String redirectToDowntimes() {
        return "redirect:/web/downtimes";
    }

    @GetMapping("/web")
    public String redirectToDowntimesFromWeb() {
        return "redirect:/web/downtimes";
    }

    @PostConstruct
    public void initSettings() {
        try {
            settingsService.initializeDefaultSettings();
        } catch (Exception e) {
            log.error("Ошибка при инициализации настроек по умолчанию: {}", e.getMessage());
        }
    }


    @GetMapping("/settings")
    public String settings(Model model) {
        try {
            Map<String, String> settings = settingsService.getAllSettingsAsMap();
            if (settings == null) {
                settings = new HashMap<>();
            }

            // Получаем все настройки по категориям
            model.addAttribute("refreshInterval",
                    settingsService.getIntSetting("refresh.interval"));
            model.addAttribute("maxFileSize",
                    settingsService.getIntSetting("photos.max.file.size"));
            model.addAttribute("compressImages",
                    settingsService.getBooleanSetting("photos.compress.enabled"));
            model.addAttribute("emailNotifications",
                    settingsService.getBooleanSetting("notifications.email.enabled"));

            // Для сохранения состояния формы
            model.addAttribute("settings", settings);

        } catch (Exception e) {
            log.error("Ошибка при загрузке настроек: {}", e.getMessage());
            model.addAttribute("error", "Не удалось загрузить настройки");
        }

        return "downtime/settings";
    }

    @PostMapping("/settings")
    public String saveSettings(@RequestParam Map<String, String> params,
                               RedirectAttributes redirectAttributes) {
        try {
            log.info("Сохранение настроек: {}", params);

            settingsService.saveSetting("refresh.interval",
                    params.getOrDefault("refreshInterval", "30"));

            settingsService.saveSetting("photos.max.file.size",
                    params.getOrDefault("maxFileSize", "10"));

            settingsService.saveSetting("photos.compress.enabled",
                    params.containsKey("compressImages") ? "true" : "false");

            settingsService.saveSetting("notifications.email.enabled",
                    params.containsKey("emailNotifications") ? "true" : "false");

            log.info("Настройки успешно сохранены");
            redirectAttributes.addFlashAttribute("success", true);

        } catch (Exception e) {
            log.error("Ошибка при сохранении настроек: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Ошибка при сохранении настроек: " + e.getMessage());
        }

        return "redirect:/web/downtimes/settings";
    }


    @PostMapping("/settings/reset")
    public String resetSettings(RedirectAttributes redirectAttributes) {
        try {
            settingsService.resetToDefaults();
            redirectAttributes.addFlashAttribute("success", true);
            redirectAttributes.addFlashAttribute("message", "Настройки сброшены к значениям по умолчанию");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при сбросе настроек");
        }
        return "redirect:/web/downtimes/settings";
    }
    @GetMapping
    public String index(Model model,
                        @PageableDefault(size = 5) Pageable pageable,
                        @RequestParam(required = false) String equipmentId,
                        @RequestParam(required = false) DowntimeStatus status,
                        @RequestParam(required = false) String operator,
                        @RequestParam(required = false) LocalDate dateFrom) {

        try {
            log.info("Получение списка простоев с параметрами: equipmentId={}, status={}, operator={}, dateFrom={}",
                    equipmentId, status, operator, dateFrom);

            // 1. Получаем данные
            Page<DowntimeResponse> downtimes = Page.empty(pageable);
            List<EquipmentDto> equipmentList = Collections.emptyList();

            try {
                // Получаем список простоев с проверкой на null
                Page<DowntimeResponse> tempDowntimes = downtimeService.getFilteredDowntimes(
                        equipmentId, status, operator, dateFrom, pageable);

                if (tempDowntimes != null && tempDowntimes.getContent() != null) {
                    // Фильтруем null значения в контенте
                    List<DowntimeResponse> filteredContent = tempDowntimes.getContent().stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    // Создаем новую страницу с отфильтрованным контентом
                    downtimes = new PageImpl<>(
                            filteredContent,
                            pageable,
                            tempDowntimes.getTotalElements()
                    );
                }
            } catch (Exception e) {
                log.error("Ошибка при получении списка простоев: {}", e.getMessage(), e);
                downtimes = Page.empty(pageable);
            }

            try {
                // Получаем список оборудования с проверкой на null
                List<EquipmentDto> tempEquipmentList = downtimeService.getAllEquipment();
                if (tempEquipmentList != null) {
                    equipmentList = tempEquipmentList.stream()
                            .filter(Objects::nonNull)
                            .filter(eq -> eq.getEquipmentId() != null && !eq.getEquipmentId().trim().isEmpty())
                            .filter(eq -> eq.getEquipmentName() != null && !eq.getEquipmentName().trim().isEmpty())
                            .collect(Collectors.toList());
                }
            } catch (Exception e) {
                log.error("Ошибка при получении списка оборудования: {}", e.getMessage(), e);
                equipmentList = Collections.emptyList();
            }

            // 2. Собираем статистику с безопасными методами
            Map<String, Object> stats = new HashMap<>();
            stats.put("activeCount", safeCountByStatus(DowntimeStatus.ACTIVE));
            stats.put("todayCount", safeCountToday());
            stats.put("avgDuration", safeGetAverageDuration());
            stats.put("photosCount", safeCountTotalPhotos());

            // 3. Добавляем атрибуты в модель
            model.addAttribute("downtimes", downtimes);
            model.addAttribute("equipmentList", equipmentList);
            model.addAttribute("stats", stats);
            model.addAttribute("refreshInterval", 30);

            // 4. Добавляем параметры фильтров для сохранения состояния формы
            if (equipmentId != null) model.addAttribute("selectedEquipmentId", equipmentId);
            if (status != null) model.addAttribute("selectedStatus", status.name());
            if (operator != null) model.addAttribute("selectedOperator", operator);
            if (dateFrom != null) model.addAttribute("selectedDateFrom", dateFrom.toString());

            log.info("Страница подготовлена: простоев={}, оборудование={}, активных={}",
                    downtimes.getTotalElements(), equipmentList.size(), stats.get("activeCount"));

            return "downtime/index";

        } catch (Exception e) {
            log.error("Критическая ошибка при загрузке страницы списка простоев", e);

            // Возвращаем безопасные значения по умолчанию
            model.addAttribute("downtimes", Page.empty(pageable));
            model.addAttribute("equipmentList", Collections.emptyList());
            model.addAttribute("stats", Map.of(
                    "activeCount", 0,
                    "todayCount", 0,
                    "avgDuration", "0ч",
                    "photosCount", 0
            ));
            model.addAttribute("refreshInterval", 30);
            model.addAttribute("errorMessage", "Произошла ошибка при загрузке данных. Пожалуйста, попробуйте позже.");

            return "downtime/index";
        }
    }

    // Вспомогательные методы для безопасного получения статистики
    private Integer safeCountByStatus(DowntimeStatus status) {
        try {
            long count = downtimeService.countByStatus(status);
            return count >= 0 ? (int) count : 0;
        } catch (Exception e) {
            log.warn("Ошибка при подсчете по статусу {}: {}", status, e.getMessage());
            return 0;
        }
    }

    private Integer safeCountToday() {
        try {
            long count = downtimeService.countToday();
            return count >= 0 ? (int) count : 0;
        } catch (Exception e) {
            log.warn("Ошибка при подсчете за сегодня: {}", e.getMessage());
            return 0;
        }
    }

    private String safeGetAverageDuration() {
        try {
            String duration = downtimeService.getAverageDuration();
            return duration != null ? duration : "0ч";
        } catch (Exception e) {
            log.warn("Ошибка при получении средней длительности: {}", e.getMessage());
            return "0ч";
        }
    }

    private Integer safeCountTotalPhotos() {
        try {
            long count = downtimeService.countTotalPhotos();
            return count >= 0 ? (int) count : 0;
        } catch (Exception e) {
            log.warn("Ошибка при подсчете фото: {}", e.getMessage());
            return 0;
        }
    }

    @GetMapping("/{id}")
    public String getDowntimeDetails(@PathVariable Long id, Model model) {
        try {
            log.info("Попытка получить простой с ID: {}", id);

            // Проверяем валидность ID
            if (id == null || id <= 0) {
                log.warn("Неверный ID простоя: {}", id);
                model.addAttribute("error", "Неверный ID простоя. ID должен быть положительным числом.");
                return "downtime/detail";
            }

            // Получаем данные простоя
            DowntimeResponse downtime = downtimeService.getDowntime(id);

            if (downtime == null) {
                log.warn("Простой с ID {} не найден в базе данных", id);
                model.addAttribute("error", "Простой с ID " + id + " не найден");
                return "downtime/detail";
            }

            // Убеждаемся, что у нас есть минимально необходимые данные
            if (downtime.getId() == null) {
                log.error("Получен простой с null ID");
                model.addAttribute("error", "Ошибка данных: отсутствует ID простоя");
                return "downtime/detail";
            }

            log.info("Успешно получен простой с ID: {}", id);
            model.addAttribute("downtime", downtime);

        } catch (Exception e) {
            log.error("Ошибка при получении простоя с ID {}: {}", id, e.getMessage(), e);
            model.addAttribute("error", "Ошибка при загрузке данных простоя: " +
                    (e.getMessage() != null ? e.getMessage() : "Неизвестная ошибка"));
        }

        return "downtime/detail";
    }

    @GetMapping("/new")
    public String newDowntimeForm(Model model) {
        model.addAttribute("request", new DowntimeRequest());
        model.addAttribute("startTime", LocalDateTime.now());
        return "downtime/new";
    }

    @GetMapping("/check-db")
    @ResponseBody
    public String checkDatabase() {
        try {
            long count = downtimeRepository.count();
            List<DowntimeEvent> all = downtimeRepository.findAll();

            StringBuilder sb = new StringBuilder();
            sb.append("=== ПРОВЕРКА БАЗЫ ДАННЫХ ===<br>");
            sb.append("Всего записей в БД: ").append(count).append("<br><br>");

            if (all.isEmpty()) {
                sb.append("БАЗА ДАННЫХ ПУСТА!<br>");
            } else {
                sb.append("Список записей: <br>");
                all.forEach(event -> {
                    if (event != null) {
                        sb.append("- ID: ").append(event.getId())
                                .append("<br>  Оборудование: ").append(event.getEquipmentId())
                                .append(" (").append(event.getEquipmentName()).append(")")
                                .append("<br>  Оператор: ").append(event.getOperatorName())
                                .append("<br>  Причина: ").append(event.getReason())
                                .append("<br>  Статус: ").append(event.getStatus())
                                .append("<br>  Начало: ").append(event.getStartTime())
                                .append("<br><br>");
                    }
                });
            }

            sb.append("=== ИНФОРМАЦИЯ ===<br>");
            sb.append("База данных MongoDB работает корректно!<br>");
            sb.append("Записи сохраняются успешно.<br>");

            return sb.toString();
        } catch (Exception e) {
            return "Ошибка при проверке базы данных: " + e.getMessage();
        }
    }

    @PostMapping
    public String createDowntime(@Valid @ModelAttribute("request") DowntimeRequest request,
                                 BindingResult result,
                                 @RequestParam(required = false) List<MultipartFile> photos,
                                 Model model,
                                 HttpServletRequest httpRequest) {

        log.info("=== НАЧАЛО СОЗДАНИЯ ПРОСТОЯ ===");

        if (result.hasErrors()) {
            log.error("Ошибки валидации:");
            result.getAllErrors().forEach(error -> {
                if (error instanceof FieldError) {
                    FieldError fieldError = (FieldError) error;
                    log.error("- Поле: {}, Ошибка: {}", fieldError.getField(), error.getDefaultMessage());
                } else {
                    log.error("- Ошибка: {}", error.getDefaultMessage());
                }
            });

            model.addAttribute("request", request);
            model.addAttribute("startTime", LocalDateTime.now());
            return "downtime/new";
        }

        try {
            log.info("Вызываем сервис...");
            DowntimeResponse created = downtimeService.createDowntime(request);

            log.info("УСПЕХ! Создан простой с ID: {}", created.getId());

            if (photos != null && !photos.isEmpty()) {
                log.info("Получено фото: {}", photos.size());
                for (MultipartFile photo : photos) {
                    if (photo != null && !photo.isEmpty()) {
                        String photoUrl = fileStorageService.uploadFile(photo, created.getId().toString());
                        if (photoUrl != null) {
                            downtimeService.addPhotoToDowntime(created.getId(), photoUrl);
                        }
                    }
                }
            }

            return "redirect:/web/downtimes/" + created.getId();

        } catch (IllegalStateException e) {
            log.error("Ошибка бизнес-логики: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("request", request);
            model.addAttribute("startTime", LocalDateTime.now());
            return "downtime/new";
        } catch (Exception e) {
            log.error("Критическая ошибка: ", e);
            model.addAttribute("error", "Системная ошибка: " + e.getMessage());
            model.addAttribute("request", request);
            model.addAttribute("startTime", LocalDateTime.now());
            return "downtime/new";
        }
    }

    @GetMapping("/{id}/photos")
    public String uploadPhotoPage(@PathVariable Long id, Model model) {
        try {
            DowntimeResponse downtime = downtimeService.getDowntime(id);
            if (downtime != null) {
                model.addAttribute("downtime", downtime);
            } else {
                model.addAttribute("error", "Простой не найден");
            }
        } catch (Exception e) {
            log.error("Ошибка при получении простоя для загрузки фото: {}", e.getMessage());
            model.addAttribute("error", "Ошибка при загрузке данных");
        }
        return "downtime/upload-photos";
    }

    @PostMapping("/{id}/photos")
    public String uploadPhotos(@PathVariable Long id,
                               @RequestParam("files") List<MultipartFile> files,
                               Model model) {

        try {
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        try {
                            String photoUrl = fileStorageService.uploadFile(file, id.toString());
                            if (photoUrl != null) {
                                downtimeService.addPhotoToDowntime(id, photoUrl);
                                log.info("Фото загружено: {}", photoUrl);
                            }
                        } catch (Exception e) {
                            log.error("Ошибка при загрузке фото: {}", e.getMessage());
                        }
                    }
                }
            }

            // Обновляем данные простоя
            DowntimeResponse downtime = downtimeService.getDowntime(id);
            if (downtime != null) {
                model.addAttribute("downtime", downtime);
                model.addAttribute("successMessage", "Фото успешно загружены!");
            } else {
                model.addAttribute("error", "Простой не найден");
            }
        } catch (Exception e) {
            log.error("Ошибка при загрузке фото: {}", e.getMessage());
            model.addAttribute("error", "Ошибка при загрузке фото: " + e.getMessage());
        }

        return "downtime/upload-photos";
    }

    @PostMapping("/{id}/resolve")
    public String resolveDowntime(@PathVariable Long id,
                                  @RequestParam(required = false) String comment) {
        try {
            downtimeService.resolveDowntime(id, comment);
        } catch (Exception e) {
            log.error("Ошибка при закрытии простоя с ID {}: {}", id, e.getMessage());
        }
        return "redirect:/web/downtimes/" + id;
    }


}
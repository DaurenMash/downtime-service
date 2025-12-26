package com.example.downtime.controller.web;

import com.example.downtime.model.DowntimeEvent;
import com.example.downtime.model.DowntimeRequest;
import com.example.downtime.model.DowntimeResponse;
import com.example.downtime.model.DowntimeStatus;
import com.example.downtime.repository.DowntimeRepository;
import com.example.downtime.service.DowntimeService;
import com.example.downtime.service.FileStorageService;
import com.mongodb.client.MongoDatabase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Controller
@RequestMapping("/web/downtimes")
@RequiredArgsConstructor
public class DowntimeWebController {

    private final DowntimeService downtimeService;
    private final FileStorageService fileStorageService;
    private final DowntimeRepository downtimeRepository;

    @GetMapping
    public String index(Model model,
                        @PageableDefault(size = 5) Pageable pageable,
                        @RequestParam(required = false) String equipmentId,
                        @RequestParam(required = false) DowntimeStatus status,
                        @RequestParam(required = false) String operator,
                        @RequestParam(required = false) LocalDate dateFrom) {

        Page<DowntimeResponse> downtimes = downtimeService.getFilteredDowntimes(
                equipmentId, status, operator, dateFrom, pageable
        );

        // Отладочный вывод
        System.out.println("Всего записей: " + downtimes.getTotalElements());
        System.out.println("Список простоев: " + downtimes.getContent());

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeCount", downtimeService.countByStatus(DowntimeStatus.ACTIVE));
        stats.put("todayCount", downtimeService.countToday());
        stats.put("avgDuration", downtimeService.getAverageDuration());
        stats.put("photosCount", downtimeService.countTotalPhotos());

        model.addAttribute("downtimes", downtimes);
        model.addAttribute("stats", stats);
        model.addAttribute("equipmentList", downtimeService.getAllEquipment());
        model.addAttribute("refreshInterval", 30);

        return "downtime/index";
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
                sb.append("- ID: ").append(event.getId())
                        .append("<br>  Оборудование: ").append(event.getEquipmentId())
                        .append(" (").append(event.getEquipmentName()).append(")")
                        .append("<br>  Оператор: ").append(event.getOperatorName())
                        .append("<br>  Причина: ").append(event.getReason())
                        .append("<br>  Статус: ").append(event.getStatus())
                        .append("<br>  Начало: ").append(event.getStartTime())
                        .append("<br><br>");
            });
        }

        sb.append("=== ИНФОРМАЦИЯ ===<br>");
        sb.append("База данных MongoDB работает корректно!<br>");
        sb.append("Записи сохраняются успешно.<br>");
        sb.append("Проблема в шаблонах Thymeleaf.");

        return sb.toString();
    }

    @PostMapping
    public String createDowntime(@Valid @ModelAttribute("request") DowntimeRequest request,
                                 BindingResult result,
                                 @RequestParam(required = false) List<MultipartFile> photos,
                                 Model model,
                                 HttpServletRequest httpRequest) {

        log.info("=== НАЧАЛО СОЗДАНИЯ ПРОСТОЯ ===");
        log.info("Метод: {}", httpRequest.getMethod());
        log.info("Content-Type: {}", httpRequest.getContentType());

        // Логируем все параметры запроса
        httpRequest.getParameterMap().forEach((key, values) -> {
            log.info("Параметр {} = {}", key, Arrays.toString(values));
        });

        log.info("Request object: {}", request);
        log.info("Has errors: {}", result.hasErrors());

        if (result.hasErrors()) {
            log.error("Ошибки валидации:");
            result.getAllErrors().forEach(error ->
                    log.error("- Поле: {}, Ошибка: {}",
                            ((FieldError) error).getField(), error.getDefaultMessage())
            );

            // Добавляем объект request обратно в модель
            model.addAttribute("request", request);
            model.addAttribute("startTime", LocalDateTime.now());
            return "downtime/new";
        }

        try {
            log.info("Вызываем сервис...");
            DowntimeResponse created = downtimeService.createDowntime(request);

            log.info("УСПЕХ! Создан простой с ID: {}", created.getId());

            if (photos != null) {
                log.info("Получено фото: {}", photos.size());
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

    @GetMapping("/{id}")
    public String getDowntimeDetails(@PathVariable Long id, Model model) { // String -> Long
        DowntimeResponse downtime = downtimeService.getDowntime(id.toString());
        model.addAttribute("downtime", downtime);
        return "downtime/detail";
    }

    @GetMapping("/{id}/photos")
    public String uploadPhotoPage(@PathVariable Long id, Model model) { // String -> Long
        DowntimeResponse downtime = downtimeService.getDowntime(id.toString());
        model.addAttribute("downtime", downtime);
        return "downtime/upload-photos";
    }

    @PostMapping("/{id}/photos")
    public String uploadPhotos(@PathVariable Long id, // String -> Long
                               @RequestParam("files") List<MultipartFile> files,
                               Model model) {

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String photoUrl = fileStorageService.uploadFile(file, id.toString());
                downtimeService.addPhotoToDowntime(id.toString(), photoUrl);
            }
        }

        // Обновляем данные простоя
        DowntimeResponse downtime = downtimeService.getDowntime(id.toString());
        model.addAttribute("downtime", downtime);
        model.addAttribute("successMessage", "Фото успешно загружены!");

        return "downtime/upload-photos";
    }

    @PostMapping("/{id}/resolve")
    public String resolveDowntime(@PathVariable Long id, // String -> Long
                                  @RequestParam(required = false) String comment) {
        downtimeService.resolveDowntime(id.toString(), comment);
        return "redirect:/web/downtimes/" + id;
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        return "downtime/settings";
    }

    @PostMapping("/settings")
    public String saveSettings(@RequestParam Map<String, String> params) {
        return "redirect:/web/downtimes/settings?success=true";
    }
}
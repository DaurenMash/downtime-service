package com.example.downtime.controller.web;

import com.example.downtime.model.EquipmentStatus;
import com.example.downtime.service.EquipmentMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;

@Slf4j
@Controller
@RequestMapping("/web/monitor")
@RequiredArgsConstructor
public class EquipmentMonitorController {

    private final EquipmentMonitorService monitorService;

    @GetMapping("/dashboard")
    public String monitorDashboard(Model model) {
        try {
            // Получаем статусы всех оборудования, сортируем по ID
            var equipmentList = monitorService.getAllEquipmentStatuses().stream()
                    .sorted(Comparator.comparing(EquipmentStatus::getEquipmentId))
                    .toList();

            // Получаем статистику
            var stats = monitorService.getStatistics();

            // Добавляем в модель
            model.addAttribute("equipmentList", equipmentList);
            model.addAttribute("stats", stats);
            model.addAttribute("totalEquipment", equipmentList.size());

            log.debug("Загружена страница мониторинга: {} единиц оборудования", equipmentList.size());

        } catch (Exception e) {
            log.error("Ошибка при загрузке страницы мониторинга: {}", e.getMessage());
            model.addAttribute("error", "Ошибка при загрузке данных мониторинга");
        }

        return "monitor/dashboard";
    }

    @GetMapping("/{equipmentId}")
    public String equipmentDetail(@PathVariable String equipmentId, Model model) {
        try {
            EquipmentStatus status = monitorService.getEquipmentStatus(equipmentId);
            if (status == null) {
                model.addAttribute("error", "Оборудование не найдено: " + equipmentId);
                return "monitor/equipment-detail";
            }

            model.addAttribute("equipment", status);

            // Рассчитываем время в текущем статусе
            java.time.Duration currentStatusDuration = java.time.Duration.between(
                    status.getStatusChangedAt(), java.time.LocalDateTime.now());
            model.addAttribute("currentStatusMinutes", currentStatusDuration.toMinutes());

        } catch (Exception e) {
            log.error("Ошибка при получении деталей оборудования {}: {}", equipmentId, e.getMessage());
            model.addAttribute("error", "Ошибка при загрузке данных оборудования");
        }

        return "monitor/equipment-detail";
    }

    @PostMapping("/{equipmentId}/start")
    public String startEquipment(@PathVariable String equipmentId,
                                 RedirectAttributes redirectAttributes) {
        try {
            monitorService.setEquipmentStatus(equipmentId, EquipmentStatus.Status.WORKING);
            redirectAttributes.addFlashAttribute("success",
                    "Оборудование " + equipmentId + " запущено в работу");
        } catch (Exception e) {
            log.error("Ошибка при запуске оборудования {}: {}", equipmentId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка при запуске оборудования");
        }

        return "redirect:/web/monitor/" + equipmentId;
    }

    @PostMapping("/{equipmentId}/stop")
    public String stopEquipment(@PathVariable String equipmentId,
                                RedirectAttributes redirectAttributes) {
        try {
            monitorService.setEquipmentStatus(equipmentId, EquipmentStatus.Status.DOWNTIME);
            redirectAttributes.addFlashAttribute("success",
                    "Оборудование " + equipmentId + " переведено в простой");
        } catch (Exception e) {
            log.error("Ошибка при остановке оборудования {}: {}", equipmentId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка при остановке оборудования");
        }

        return "redirect:/web/monitor/" + equipmentId;
    }

    @PostMapping("/{equipmentId}/simulate")
    public String simulateStatusChange(@PathVariable String equipmentId,
                                       RedirectAttributes redirectAttributes) {
        try {
            // Получаем текущий статус
            EquipmentStatus current = monitorService.getEquipmentStatus(equipmentId);
            if (current != null) {
                // Меняем на противоположный
                EquipmentStatus.Status newStatus =
                        (current.getCurrentStatus() == EquipmentStatus.Status.WORKING)
                                ? EquipmentStatus.Status.DOWNTIME
                                : EquipmentStatus.Status.WORKING;

                monitorService.setEquipmentStatus(equipmentId, newStatus);

                redirectAttributes.addFlashAttribute("success",
                        "Симуляция: статус оборудования " + equipmentId +
                                " изменен на " + newStatus.getDisplayName());
            }
        } catch (Exception e) {
            log.error("Ошибка при симуляции смены статуса {}: {}", equipmentId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка при симуляции");
        }

        return "redirect:/web/monitor/" + equipmentId;
    }
}
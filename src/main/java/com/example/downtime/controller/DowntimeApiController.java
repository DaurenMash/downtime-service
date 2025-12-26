package com.example.downtime.controller;

import com.example.downtime.model.DowntimeRequest;
import com.example.downtime.model.DowntimeResponse;
import com.example.downtime.service.DowntimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/downtimes")
@RequiredArgsConstructor
@Tag(name = "Downtime Management", description = "API for managing equipment downtime events")
public class DowntimeApiController {

    private final DowntimeService downtimeService;

    @PostMapping
    @Operation(summary = "Create a new downtime event")
    public ResponseEntity<DowntimeResponse> createDowntime(
            @Valid @RequestBody DowntimeRequest request) {
        return ResponseEntity.ok(downtimeService.createDowntime(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get downtime event by ID")
    public ResponseEntity<DowntimeResponse> getDowntime(@PathVariable Long id) { // String -> Long
        return ResponseEntity.ok(downtimeService.getDowntime(id.toString()));
    }

    @GetMapping("/equipment/{equipmentId}")
    @Operation(summary = "Get all downtime events for equipment")
    public ResponseEntity<List<DowntimeResponse>> getDowntimesByEquipment(
            @PathVariable String equipmentId) {
        return ResponseEntity.ok(downtimeService.getDowntimesByEquipment(equipmentId));
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active downtime events")
    public ResponseEntity<List<DowntimeResponse>> getActiveDowntimes() {
        return ResponseEntity.ok(downtimeService.getActiveDowntimes());
    }

    @GetMapping("/operator/{operatorId}")
    @Operation(summary = "Get downtime events created by operator")
    public ResponseEntity<List<DowntimeResponse>> getDowntimesByOperator(
            @PathVariable String operatorId) {
        return ResponseEntity.ok(downtimeService.getDowntimesByOperator(operatorId));
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "Resolve a downtime event")
    public ResponseEntity<DowntimeResponse> resolveDowntime(
            @PathVariable Long id, // String -> Long
            @RequestParam(required = false) String comment) {
        return ResponseEntity.ok(downtimeService.resolveDowntime(id.toString(), comment));
    }
}
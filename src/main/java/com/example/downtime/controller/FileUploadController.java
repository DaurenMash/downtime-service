package com.example.downtime.controller;

import com.example.downtime.service.DowntimeService;
import com.example.downtime.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/downtimes/{downtimeId}/photos")
@RequiredArgsConstructor
@Tag(name = "File Upload", description = "API for uploading photos for downtime events")
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final DowntimeService downtimeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload photo for downtime event")
    public ResponseEntity<String> uploadPhoto(
            @PathVariable String downtimeId,
            @RequestParam("file") MultipartFile file) {

        String photoUrl = fileStorageService.uploadFile(file, downtimeId);
        downtimeService.addPhotoToDowntime(downtimeId, photoUrl);

        return ResponseEntity.ok(photoUrl);
    }
}
package com.unlimsync.controller;

import com.unlimsync.model.MediaFile;
import com.unlimsync.repository.MediaFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.unlimsync.service.telegram.TelegramStorageService;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.time.Instant;

import java.util.List;
import java.util.Optional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

@RestController
@RequestMapping("/api/v1/media")
@Tag(name = "Media", description = "Operations for managing media files")
public class MediaFileController {

    private final MediaFileRepository mediaFileRepository;
    private final com.unlimsync.service.telegram.TelegramStorageService telegramStorageService;

    @Autowired
    public MediaFileController(MediaFileRepository mediaFileRepository, com.unlimsync.service.telegram.TelegramStorageService telegramStorageService) {
        this.mediaFileRepository = mediaFileRepository;
        this.telegramStorageService = telegramStorageService;
    }

    /** List all stored media entries */
    @GetMapping("/list")
    public ResponseEntity<List<MediaFile>> listAll() {
        List<MediaFile> all = mediaFileRepository.findAll();
        return ResponseEntity.ok(all);
    }

    /**
     * Paginated list of media files.
     * @param page zero‑based page index
     * @param size page size
     */
    @GetMapping("/list/paged")
    public ResponseEntity<Page<MediaFile>> listPaginated(@RequestParam(name = "page", defaultValue = "0") int page,
                                                          @RequestParam(name = "size", defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MediaFile> pageResult = mediaFileRepository.findAll(pageable);
        return ResponseEntity.ok(pageResult);
    }

    /** Upload a file to Telegram and persist its metadata */
    @PostMapping("/upload")
    public ResponseEntity<MediaFile> upload(@RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "chatId", required = false) String chatId) throws IOException {
        String effectiveChatId = (chatId != null && !chatId.isEmpty()) ? chatId : null;
        // Convert multipart to temporary file
        File tempFile = File.createTempFile("upload-", file.getOriginalFilename());
        file.transferTo(tempFile);
        // Upload to Telegram
        String telegramFileId = telegramStorageService.uploadFile(tempFile, effectiveChatId);
        // Build and save MediaFile entity
        MediaFile mediaFile = new MediaFile();
        mediaFile.setTelegramFileId(telegramFileId);
        mediaFile.setOriginalName(file.getOriginalFilename());
        mediaFile.setChatId(effectiveChatId);
        mediaFile.setUploadedAt(Instant.now());
        MediaFile saved = mediaFileRepository.save(mediaFile);
        // Cleanup temp file
        tempFile.delete();
        return ResponseEntity.ok(saved);
    }

    /** Retrieve a single media entry by its database ID */
    @GetMapping("/{id}")
    public ResponseEntity<MediaFile> getById(@PathVariable("id") Long id) {
        Optional<MediaFile> opt = mediaFileRepository.findById(id);
        return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Download the media file from Telegram using stored file_id.
     * Streams the file back to the client as octet‑stream.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> downloadById(@PathVariable("id") Long id) throws IOException {
        Optional<MediaFile> opt = mediaFileRepository.findById(id);
        if (!opt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        MediaFile media = opt.get();
        // Download to temp file
        File temp = File.createTempFile("download-", "tmp");
        telegramStorageService.downloadFile(media.getTelegramFileId(), temp.getAbsolutePath());
        InputStreamResource resource = new InputStreamResource(new java.io.FileInputStream(temp));
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + temp.getName() + "\"");
        headers.setContentLength(temp.length());
        // Ensure temp file cleanup after response
        temp.deleteOnExit();
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /** Delete a media entry (also optionally delete from Telegram) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        if (!mediaFileRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        mediaFileRepository.deleteById(id);
        // Note: actual deletion from Telegram would require a bot call; omitted for now.
        return ResponseEntity.noContent().build();
    }
}

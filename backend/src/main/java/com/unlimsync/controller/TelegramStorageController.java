package com.unlimsync.controller;

import com.unlimsync.service.telegram.TelegramStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1/telegram")
public class TelegramStorageController {

    private final TelegramStorageService telegramStorageService;

    @Autowired
    public TelegramStorageController(TelegramStorageService telegramStorageService) {
        this.telegramStorageService = telegramStorageService;
    }

    /**
     * Upload a file to Telegram.
     * @param file   the multipart file received from the client
     * @param chatId the Telegram chat identifier (e.g., a private channel ID) – can be passed by the client
     * @return the Telegram file_id that can be stored in the DB for later retrieval
     */
    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam("chatId") String chatId) throws IOException {
        // Convert MultipartFile to a temporary file on disk
        File tempFile = File.createTempFile("upload-", file.getOriginalFilename());
        file.transferTo(tempFile);
        try {
            String fileId = telegramStorageService.uploadFile(tempFile, chatId);
            return ResponseEntity.ok(fileId);
        } finally {
            // Clean up the temporary file
            if (!tempFile.delete()) {
                tempFile.deleteOnExit();
            }
        }
    }

    /**
     * Download a file from Telegram and stream it back to the client.
     * @param fileId the Telegram file identifier
     * @return the file content as an octet-stream response
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<InputStreamResource> download(@PathVariable String fileId) throws IOException {
        // Download to a temporary location
        File temp = File.createTempFile("download-", "tmp");
        telegramStorageService.downloadFile(fileId, temp.getAbsolutePath());
        InputStreamResource resource = new InputStreamResource(new java.io.FileInputStream(temp));
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + temp.getName() + "\"");
        headers.setContentLength(temp.length());
        // Stream the file; the temporary file will be deleted after response is committed
        ResponseEntity<InputStreamResource> response = ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
        // Schedule deletion after response is sent (best‑effort)
        temp.deleteOnExit();
        return response;
    }

    /**
     * Retrieve a direct download URL for a Telegram file.
     * @param fileId the Telegram file identifier
     * @return a URL string that the client can use to download the file directly from Telegram
     */
    @GetMapping("/url/{fileId}")
    public ResponseEntity<String> getDownloadUrl(@PathVariable String fileId) {
        String url = telegramStorageService.getFileDownloadUrl(fileId);
        return ResponseEntity.ok(url);
    }
}

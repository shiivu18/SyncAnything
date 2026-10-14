package com.unlimsync.controller;

import com.unlimsync.service.telegram.TelegramStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TelegramStorageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TelegramStorageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TelegramStorageService telegramStorageService;

    @Test
    void testUpload_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                MediaType.TEXT_PLAIN_VALUE, "hello telegram".getBytes());
        Mockito.when(telegramStorageService.uploadFile(any(File.class), anyString()))
                .thenReturn("tg_file_456");
        mockMvc.perform(multipart("/api/v1/telegram/upload")
                        .file(file)
                        .param("chatId", "12345")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(content().string("tg_file_456"));
    }

    @Test
    void testDownload_Success() throws Exception {
        // Prepare a temporary file with known content
        File temp = File.createTempFile("download-", ".txt");
        byte[] data = "download content".getBytes();
        try (FileOutputStream fos = new FileOutputStream(temp)) {
            fos.write(data);
        }
        // Mock service to write the same temp file (simulating download)
        Mockito.doAnswer(invocation -> {
            String fileId = invocation.getArgument(0);
            String destPath = invocation.getArgument(1);
            java.nio.file.Files.copy(temp.toPath(), new File(destPath).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return null;
        }).when(telegramStorageService).downloadFile(anyString(), anyString());

        mockMvc.perform(get("/api/v1/telegram/download/{fileId}", "tg_file_456"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment; filename=\"")))
                .andExpect(content().bytes(data));
    }

    @Test
    void testGetDownloadUrl_Success() throws Exception {
        Mockito.when(telegramStorageService.getFileDownloadUrl(anyString()))
                .thenReturn("https://api.telegram.org/file/botTOKEN/abc123.txt");
        mockMvc.perform(get("/api/v1/telegram/url/{fileId}", "tg_file_456"))
                .andExpect(status().isOk())
                .andExpect(content().string("https://api.telegram.org/file/botTOKEN/abc123.txt"));
    }
}

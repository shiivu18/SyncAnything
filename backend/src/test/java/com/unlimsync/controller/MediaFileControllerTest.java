package com.unlimsync.controller;

import com.unlimsync.model.MediaFile;
import com.unlimsync.service.telegram.TelegramStorageService;
import com.unlimsync.repository.MediaFileRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(MediaFileController.class)
@AutoConfigureMockMvc(addFilters = false)
class MediaFileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaFileRepository mediaFileRepository;

    @MockBean
    private TelegramStorageService telegramStorageService;

    @Test
    void testListAll_ReturnsEmptyList() throws Exception {
        Mockito.when(mediaFileRepository.findAll()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/v1/media/list"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void testUpload_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                MediaType.TEXT_PLAIN_VALUE, "hello world".getBytes());
        Mockito.when(telegramStorageService.uploadFile(any(), anyString()))
                .thenReturn("tg_file_id_123");
        MediaFile saved = new MediaFile();
        saved.setId(1L);
        saved.setTelegramFileId("tg_file_id_123");
        saved.setOriginalName("test.txt");
        saved.setChatId(null);
        saved.setUploadedAt(Instant.now());
        Mockito.when(mediaFileRepository.save(any(MediaFile.class))).thenReturn(saved);
        mockMvc.perform(multipart("/api/v1/media/upload")
                        .file(file)
                        .param("chatId", "")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telegramFileId").value("tg_file_id_123"))
                .andExpect(jsonPath("$.originalName").value("test.txt"));
    }
}

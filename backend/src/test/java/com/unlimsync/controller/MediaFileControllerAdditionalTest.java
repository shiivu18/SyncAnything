package com.unlimsync.controller;

import com.unlimsync.model.MediaFile;
import com.unlimsync.repository.MediaFileRepository;
import com.unlimsync.service.telegram.TelegramStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.io.FileWriter;
import java.time.Instant;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(MediaFileController.class)
@AutoConfigureMockMvc(addFilters = false)
class MediaFileControllerAdditionalTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaFileRepository mediaFileRepository;

    @MockBean
    private TelegramStorageService telegramStorageService;

    @Test
    void testPaginatedList() throws Exception {
        MediaFile file = new MediaFile();
        file.setId(1L);
        file.setTelegramFileId("tg123");
        file.setOriginalName("test.txt");
        file.setChatId(null);
        file.setUploadedAt(Instant.now());
        Page<MediaFile> page = new PageImpl<>(Collections.singletonList(file), PageRequest.of(0, 10), 1);
        Mockito.when(mediaFileRepository.findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class))).thenReturn(page);
        mockMvc.perform(get("/api/v1/media/list/paged").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void testDownloadById() throws Exception {
        // Prepare a MediaFile entry
        MediaFile file = new MediaFile();
        file.setId(2L);
        file.setTelegramFileId("tg456");
        file.setOriginalName("download.txt");
        file.setChatId(null);
        file.setUploadedAt(Instant.now());
        Mockito.when(mediaFileRepository.findById(eq(2L))).thenReturn(java.util.Optional.of(file));
        // Mock downloadFile to write a temporary file with known content
        Mockito.doAnswer(invocation -> {
            String destPath = invocation.getArgument(1);
            try (FileWriter writer = new FileWriter(destPath)) {
                writer.write("dummy content");
            }
            return null;
        }).when(telegramStorageService).downloadFile(eq("tg456"), anyString());

        mockMvc.perform(get("/api/v1/media/2/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(content().string("dummy content"));
    }
}

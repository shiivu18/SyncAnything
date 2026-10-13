package com.unlimsync.service.telegram;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.util.Map;

@Service
public class TelegramStorageServiceImpl implements TelegramStorageService {
    private final RestTemplate restTemplate = new RestTemplate();
    @org.springframework.beans.factory.annotation.Value("${TELEGRAM_BOT_TOKEN}")
    private String botToken;
    @org.springframework.beans.factory.annotation.Value("${TELEGRAM_CHAT_ID:}")
    private String defaultChatId;
    private String apiBase;

    @PostConstruct
    public void init() {
        this.apiBase = "https://api.telegram.org/bot" + botToken + "/";
    }

    @Override
    public String uploadFile(File file, String chatId) {
        // Telegram requires a chat_id – we use a private channel or user ID passed via env var
        String uploadUrl = apiBase + "sendDocument";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
        body.add("chat_id", chatId);
        body.add("document", new FileSystemResource(file));
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl, requestEntity, Map.class);
        Map result = (Map) response.getBody().get("result");
        Map document = (Map) result.get("document");
        return (String) document.get("file_id");
    }

    @Override
    public File downloadFile(String fileId, String destinationPath) {
        // First get the file path via getFile
        String getFileUrl = UriComponentsBuilder.fromHttpUrl(apiBase + "getFile")
                .queryParam("file_id", fileId)
                .toUriString();
        ResponseEntity<Map> resp = restTemplate.getForEntity(getFileUrl, Map.class);
        Map result = (Map) resp.getBody().get("result");
        String filePath = (String) result.get("file_path");
        String fileUrl = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;
        // Download the file bytes
        byte[] bytes = restTemplate.getForObject(fileUrl, byte[].class);
        File out = new File(destinationPath);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(out)) {
            fos.write(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    @Override
    public String getFileDownloadUrl(String fileId) {
        // Build direct download URL (requires prior getFile to resolve path)
        String getFileUrl = UriComponentsBuilder.fromHttpUrl(apiBase + "getFile")
                .queryParam("file_id", fileId)
                .toUriString();
        ResponseEntity<Map> resp = restTemplate.getForEntity(getFileUrl, Map.class);
        Map result = (Map) resp.getBody().get("result");
        String filePath = (String) result.get("file_path");
        return "https://api.telegram.org/file/bot" + botToken + "/" + filePath;
    }
}

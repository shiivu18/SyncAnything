package com.unlimsync.model;

import jakarta.persistence.*; // includes Index
import java.time.Instant;

@Entity
@Table(name = "media_files", indexes = @Index(name = "idx_telegram_file_id", columnList = "telegram_file_id"))
public class MediaFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_file_id", nullable = false, unique = true)
    private String telegramFileId;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "chat_id")
    private String chatId;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTelegramFileId() { return telegramFileId; }
    public void setTelegramFileId(String telegramFileId) { this.telegramFileId = telegramFileId; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}

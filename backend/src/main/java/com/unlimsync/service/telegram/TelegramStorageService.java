package com.unlimsync.service.telegram;

import java.io.File;

public interface TelegramStorageService {
    /**
     * Upload a file to Telegram and return the file_id reference.
     * The implementation will use the configured chat_id (environment variable).
     */
    String uploadFile(File file, String chatId);

    /**
     * Download a Telegram file (by file_id) to the given destination path.
     * Returns the downloaded File object.
     */
    File downloadFile(String fileId, String destinationPath);

    /**
     * Get a direct download URL for a Telegram file_id.
     */
    String getFileDownloadUrl(String fileId);
}

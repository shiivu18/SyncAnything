package com.unlimsync.controller;

import com.unlimsync.model.MediaFile;
import com.unlimsync.repository.MediaFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/media")
public class MediaFileController {

    private final MediaFileRepository mediaFileRepository;

    @Autowired
    public MediaFileController(MediaFileRepository mediaFileRepository) {
        this.mediaFileRepository = mediaFileRepository;
    }

    /** List all stored media entries */
    @GetMapping("/list")
    public ResponseEntity<List<MediaFile>> listAll() {
        List<MediaFile> all = mediaFileRepository.findAll();
        return ResponseEntity.ok(all);
    }

    /** Retrieve a single media entry by its database ID */
    @GetMapping("/{id}")
    public ResponseEntity<MediaFile> getById(@PathVariable Long id) {
        Optional<MediaFile> opt = mediaFileRepository.findById(id);
        return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Delete a media entry (also optionally delete from Telegram) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!mediaFileRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        mediaFileRepository.deleteById(id);
        // Note: actual deletion from Telegram would require a bot call; omitted for now.
        return ResponseEntity.noContent().build();
    }
}

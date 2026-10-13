package com.unlimsync.repository;

import com.unlimsync.model.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
    // Additional query methods can be defined here if needed.
}

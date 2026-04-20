package com.attirehub.filestorage.service;

import com.attirehub.filestorage.config.FileStorageProperties;
import com.attirehub.filestorage.dto.UploadThingUploadResponse;
import com.attirehub.shared.exception.FileStorageException;
import com.attirehub.shared.exception.InvalidFileException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    /** UploadThing URL path prefix; file key follows this (e.g. utfs.io/f/abc123). */
    private static final String UPLOADTHING_PATH_PREFIX = "/f/";

    private final FileStorageProperties fileStorageProperties;
    private final UploadThingScriptService uploadThingScriptService;

    @Override
    public String storeFile(MultipartFile file, String storageKey) {
        validateFile(file);

        UploadThingUploadResponse response = uploadThingScriptService.uploadFile(file, storageKey, "image");

        String url = response.getUrl();
        if (url == null || url.isBlank()) {
            url = "https://utfs.io/f/" + response.getFileKey();
        }

        log.debug("Uploaded file to UploadThing: url={}", url);
        return url;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty or not provided");
        }

        if (file.getSize() > fileStorageProperties.getMaxFileSize()) {
            throw new InvalidFileException(String.format(
                    "File size (%d bytes) exceeds maximum allowed size (%d bytes)",
                    file.getSize(), fileStorageProperties.getMaxFileSize()));
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new InvalidFileException("File content type is not determined");
        }

        boolean allowed = fileStorageProperties.getAllowedTypes().stream()
                .anyMatch(type -> {
                    if (type.endsWith("/*")) {
                        String prefix = type.substring(0, type.indexOf('/'));
                        return contentType.startsWith(prefix + "/");
                    }
                    return type.equalsIgnoreCase(contentType);
                });

        if (!allowed) {
            throw new InvalidFileException(String.format(
                    "File type '%s' is not allowed. Allowed types: %s",
                    contentType, fileStorageProperties.getAllowedTypes()));
        }
    }

    @Override
    public void deleteFileByUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        String fileKey = extractFileKeyFromUrl(imageUrl);
        if (fileKey == null || fileKey.isBlank()) {
            log.debug("Could not extract UploadThing file key from URL, skipping delete: {}", imageUrl);
            return;
        }
        try {
            var response = uploadThingScriptService.deleteFiles(List.of(fileKey));
            if (response == null || !response.isSuccess()) {
                String err = (response != null ? response.getError() : null);
                throw new FileStorageException("UploadThing delete failed"
                        + (err != null ? (": " + err) : ""));
            }
        } catch (Exception e) {
            // Fail fast so admin endpoints don't return "deleted" when UploadThing wasn't updated.
            log.warn("Failed to delete file from UploadThing (key={})", fileKey, e);
            if (e instanceof FileStorageException fse) {
                throw fse;
            }
            throw new FileStorageException("Failed to delete file from UploadThing (key=" + fileKey + ")", e);
        }
    }

    /**
     * Extracts UploadThing file key from URL (e.g. https://utfs.io/f/abc123 -> abc123).
     */
    private String extractFileKeyFromUrl(String url) {
        int idx = url.indexOf(UPLOADTHING_PATH_PREFIX);
        if (idx < 0) {
            return null;
        }
        int start = idx + UPLOADTHING_PATH_PREFIX.length();
        int end = url.indexOf('?', start);
        if (end < 0) {
            end = url.length();
        }
        return url.substring(start, end).trim();
    }
}

package com.attirehub.filestorage.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Uploads product/variant images to UploadThing and returns the public URL.
 * Images are stored only at variant level (variant.imageUrl in DB).
 */
public interface FileStorageService {

    /**
     * Validates the file and uploads to UploadThing. Returns the public URL to store in DB.
     *
     * @param file       the image file
     * @param storageKey e.g. "products/123" or "variants/456"
     * @return the UploadThing URL (e.g. https://utfs.io/f/...)
     */
    String storeFile(MultipartFile file, String storageKey);

    /**
     * Deletes the file from UploadThing by its stored URL.
     * Extracts the file key from URLs like https://utfs.io/f/&lt;key&gt;.
     * No-op if URL is null/blank or does not look like an UploadThing URL.
     *
     * @param imageUrl the variant or product image URL stored in DB
     */
    void deleteFileByUrl(String imageUrl);
}

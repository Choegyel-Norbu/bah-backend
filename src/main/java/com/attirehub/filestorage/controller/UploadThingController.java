package com.attirehub.filestorage.controller;

import com.attirehub.filestorage.dto.UploadThingDeleteResponse;
import com.attirehub.filestorage.dto.UploadThingUploadResponse;
import com.attirehub.filestorage.service.UploadThingScriptService;
import com.attirehub.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/uploadthing")
@RequiredArgsConstructor
public class UploadThingController {

    private final UploadThingScriptService uploadThingScriptService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UploadThingUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "field", defaultValue = "general") String field,
            @RequestParam(value = "fileType", defaultValue = "image") String fileType) {
        UploadThingUploadResponse response = uploadThingScriptService.uploadFile(file, field, fileType);
        return ResponseEntity.ok(ApiResponse.success("File uploaded successfully", response));
    }

    @DeleteMapping("/files")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UploadThingDeleteResponse>> deleteFiles(
            @RequestBody List<String> fileKeys) {
        UploadThingDeleteResponse response = uploadThingScriptService.deleteFiles(fileKeys);
        return ResponseEntity.ok(ApiResponse.success("Files deleted", response));
    }

    @DeleteMapping("/files/{fileKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UploadThingDeleteResponse>> deleteFile(
            @PathVariable String fileKey) {
        UploadThingDeleteResponse response = uploadThingScriptService.deleteFiles(List.of(fileKey));
        return ResponseEntity.ok(ApiResponse.success("File deleted", response));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("UploadThing service is available", "OK"));
    }
}

package com.attirehub.filestorage.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadThingUploadResponse {

    private boolean success;
    private String url;
    private String fileKey;
    private String fileName;
    private Long fileSize;
    private String error;
}

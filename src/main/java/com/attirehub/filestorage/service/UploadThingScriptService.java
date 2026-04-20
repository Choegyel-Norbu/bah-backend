package com.attirehub.filestorage.service;

import com.attirehub.filestorage.dto.UploadThingDeleteResponse;
import com.attirehub.filestorage.dto.UploadThingUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UploadThingScriptService {

    UploadThingUploadResponse uploadFile(MultipartFile file, String field, String fileType);

    UploadThingDeleteResponse deleteFiles(List<String> fileKeys);
}

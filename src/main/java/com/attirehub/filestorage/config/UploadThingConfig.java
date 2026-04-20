package com.attirehub.filestorage.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class UploadThingConfig {

    @Value("${uploadthing.token:}")
    private String token;

    @Value("${uploadthing.script.upload:scripts/uploadthing-upload.js}")
    private String uploadScript;

    @Value("${uploadthing.script.delete:scripts/uploadthing-delete.js}")
    private String deleteScript;

    @Value("${uploadthing.script.timeout:60}")
    private int scriptTimeout;

    public boolean isConfigured() {
        return token != null && !token.isBlank();
    }

    public void requireConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "UploadThing is not configured. Set UPLOADTHING_TOKEN environment variable.");
        }
    }
}

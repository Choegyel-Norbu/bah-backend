package com.attirehub.filestorage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.file-storage")
@Getter
@Setter
public class FileStorageProperties {

    private List<String> allowedTypes = List.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private long maxFileSize = 10485760L; // 10MB default
}

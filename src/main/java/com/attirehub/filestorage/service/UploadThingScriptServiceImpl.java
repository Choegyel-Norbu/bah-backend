package com.attirehub.filestorage.service;

import com.attirehub.filestorage.config.UploadThingConfig;
import com.attirehub.filestorage.dto.UploadThingDeleteResponse;
import com.attirehub.filestorage.dto.UploadThingUploadResponse;
import com.attirehub.shared.exception.FileStorageException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UploadThingScriptServiceImpl implements UploadThingScriptService {

    private static final Logger log = LoggerFactory.getLogger(UploadThingScriptServiceImpl.class);

    private final UploadThingConfig uploadThingConfig;
    private final ObjectMapper objectMapper;

    @Override
    public UploadThingUploadResponse uploadFile(MultipartFile file, String field, String fileType) {
        uploadThingConfig.requireConfigured();

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("ut-upload-", "-" + file.getOriginalFilename());
            file.transferTo(tempFile.toFile());

            List<String> command = new ArrayList<>();
            command.add("node");
            command.add(uploadThingConfig.getUploadScript());
            command.add(tempFile.toAbsolutePath().toString());
            command.add(field);
            command.add(fileType);
            command.add(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("UPLOADTHING_TOKEN", uploadThingConfig.getToken());
            pb.redirectErrorStream(false);

            log.debug("Running UploadThing upload script for file: {}", file.getOriginalFilename());
            Process process = pb.start();

            String stdout;
            String stderr;
            try (var stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 var stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                stdout = stdoutReader.lines().collect(Collectors.joining("\n"));
                stderr = stderrReader.lines().collect(Collectors.joining("\n"));
            }

            boolean completed = process.waitFor(uploadThingConfig.getScriptTimeout(), TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new FileStorageException("UploadThing upload script timed out");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("UploadThing upload script failed. Exit code: {}, stderr: {}", exitCode, stderr);
                throw new FileStorageException("UploadThing upload failed: " + stderr);
            }

            String jsonLine = extractJsonLine(stdout);
            UploadThingUploadResponse response = objectMapper.readValue(jsonLine, UploadThingUploadResponse.class);

            if (!response.isSuccess()) {
                throw new FileStorageException("UploadThing upload failed: " +
                        (response.getError() != null ? response.getError() : "Unknown error"));
            }

            log.info("Successfully uploaded file to UploadThing: fileKey={}", response.getFileKey());
            return response;

        } catch (FileStorageException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FileStorageException("Failed to upload file via UploadThing", e);
        } finally {
            deleteTempFile(tempFile);
        }
    }

    @Override
    public UploadThingDeleteResponse deleteFiles(List<String> fileKeys) {
        uploadThingConfig.requireConfigured();

        try {
            List<String> command = new ArrayList<>();
            command.add("node");
            command.add(uploadThingConfig.getDeleteScript());
            command.addAll(fileKeys);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("UPLOADTHING_TOKEN", uploadThingConfig.getToken());
            pb.redirectErrorStream(false);

            log.debug("Running UploadThing delete script for keys: {}", fileKeys);
            Process process = pb.start();

            String stdout;
            String stderr;
            try (var stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 var stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                stdout = stdoutReader.lines().collect(Collectors.joining("\n"));
                stderr = stderrReader.lines().collect(Collectors.joining("\n"));
            }

            boolean completed = process.waitFor(uploadThingConfig.getScriptTimeout(), TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new FileStorageException("UploadThing delete script timed out");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("UploadThing delete script failed. Exit code: {}, stderr: {}", exitCode, stderr);
                throw new FileStorageException("UploadThing delete failed: " + stderr);
            }

            String jsonLine = extractJsonLine(stdout);
            UploadThingDeleteResponse response = objectMapper.readValue(jsonLine, UploadThingDeleteResponse.class);

            if (!response.isSuccess()) {
                log.warn("UploadThing delete reported failure: {}", response.getError());
            } else {
                log.info("Successfully deleted files from UploadThing: {}", response.getDeletedFiles());
            }

            return response;

        } catch (FileStorageException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FileStorageException("Failed to delete files via UploadThing", e);
        }
    }

    private String extractJsonLine(String stdout) {
        String[] lines = stdout.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.startsWith("{") && line.endsWith("}")) {
                return line;
            }
        }
        throw new FileStorageException("No valid JSON response from UploadThing script. Output: " + stdout);
    }

    private void deleteTempFile(Path tempFile) {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", tempFile, e);
            }
        }
    }
}

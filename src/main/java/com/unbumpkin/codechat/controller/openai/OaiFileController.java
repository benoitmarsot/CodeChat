package com.unbumpkin.codechat.controller.openai;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.unbumpkin.codechat.domain.openai.OaiFile;
import com.unbumpkin.codechat.domain.openai.OaiFile.Purposes;
import com.unbumpkin.codechat.exception.ResourceNotFoundException;
import com.unbumpkin.codechat.repository.openai.OaiFileRepository;
import com.unbumpkin.codechat.service.openai.OaiFileService;
import com.unbumpkin.codechat.service.request.UploadDirRequest;
import com.unbumpkin.codechat.service.request.UploadFileRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;

@RestController
@RequestMapping("/api/v1/openai/files")
@Tag(name = "OpenAI File Controller", description = "Endpoints for managing OpenAI files")
public class OaiFileController {
    @Autowired
    private final OaiFileService oaiFileService;

    @Autowired
    private final OaiFileRepository oaiFileRepository;

    public OaiFileController(
        OaiFileService oaiFileService,
        OaiFileRepository oaiFileRepository
    ) {
        this.oaiFileService = oaiFileService;
        this.oaiFileRepository = oaiFileRepository;
    }

    @Operation(summary = "Simple get test")
    @GetMapping("/test")
    public ResponseEntity<String> test() throws IOException {
        return ResponseEntity.ok("All is good");
    }

    @Operation(summary = "Get my repo files")
    @GetMapping("/myrepo")
    public ResponseEntity<Iterable<OaiFile>> getMyRepoFiles(
        @RequestParam int projectId
    ) throws IOException {
        List<OaiFile> files = oaiFileRepository.listAllFiles(projectId);
        return ResponseEntity.ok(files);
    }

    @Operation(summary = "Get all files")
    @GetMapping()
    public ResponseEntity<Iterable<OaiFile>> getAllFiles() throws IOException {
        List<String> fileIds = oaiFileService.listFiles();
        List<OaiFile> files = oaiFileRepository.retrieveFiles(fileIds);
        return ResponseEntity.ok(files);
    }

    @Operation(summary = "Upload a directory")
    @PostMapping("/uploadDir")
    public ResponseEntity<Map<String, OaiFile>> uploadDir(
        @RequestBody UploadDirRequest request,
        @RequestParam int projectId
    ) throws IOException, SQLException {
        if (!new File(request.rootDir()).isDirectory()) {
            throw new IllegalArgumentException("Invalid directory path: " + request.rootDir());
        }

        Map<String, OaiFile> files = oaiFileService.uploadFiles(
            request.rootDir(),
            request.extension(),
            Purposes.valueOf(request.purpose().toLowerCase()),
            projectId
        );
        try {
            oaiFileRepository.storeOaiFiles(files.values(), projectId);
        } catch (DataAccessException e) {
            for (OaiFile file : files.values()) {
                oaiFileService.deleteFile(file.fileId());
            }
            throw e;
        }
        return ResponseEntity.ok(files);
    }

    @Operation(summary = "Upload a file")
    @PutMapping("/uploadFile")
    public ResponseEntity<OaiFile> uploadFile(
        @RequestHeader("Authorization") String authHeader,
        @RequestBody UploadFileRequest request,
        @RequestParam int projectId
    ) throws IOException {
        if (!new File(request.filepath()).exists()) {
            throw new IllegalArgumentException("Invalid file path: " + request.filepath());
        }

        OaiFile file = oaiFileService.uploadFile(
            request.filepath(),
            Purposes.valueOf(request.purpose().toLowerCase()),
            projectId
        );
        try {
            oaiFileRepository.storeOaiFile(file, projectId);
        } catch (IOException e) {
            oaiFileService.deleteFile(file.fileId());
            throw e;
        }
        return ResponseEntity.ok(file);
    }

    @Operation(summary = "Get a OaiFile")
    @GetMapping("/{fileId}")
    public ResponseEntity<OaiFile> getFile(
        @RequestHeader("Authorization") String authHeader,
        @PathVariable String fileId
    ) {
        OaiFile file = oaiFileRepository.retrieveFile(fileId);
        return ResponseEntity.ok(file);
    }

    @Operation(summary = "Delete a file")
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
        @RequestHeader("Authorization") String authHeader,
        @PathVariable String fileId
    ) throws IOException {
        oaiFileService.deleteFile(fileId);
        oaiFileRepository.deleteFile(fileId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete all files")
    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllFiles(
        @RequestHeader("Authorization") String authHeader
    ) throws IOException {
        List<String> fileIds = oaiFileRepository.deleteAll();
        for (String fileId : fileIds) {
            oaiFileService.deleteFile(fileId);
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get all files from a root directory")
    @GetMapping("rootDir")
    public ResponseEntity<List<OaiFile>> getFilesFromRootDir(
        @RequestHeader("Authorization") String authHeader,
        @RequestParam String rootDir,
        @RequestParam int projectId
    ) {
        if (!new File(rootDir).isDirectory()) {
            throw new IllegalArgumentException("Invalid directory path: " + rootDir);
        }

        List<OaiFile> files = oaiFileRepository.retrieveFiles(rootDir, projectId);
        return ResponseEntity.ok(files);
    }

    @Operation(summary = "Get a file info from OpenAI")
    @GetMapping("/fileinfo/{fileId}")
    public ResponseEntity<JsonNode> getFileInfo(
        @PathVariable String fileId
    ) throws IOException {
        JsonNode file = oaiFileService.retrieveFile(fileId);
        return ResponseEntity.ok(file);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
        IllegalArgumentException ex
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @Operation(summary = "Download a file to a specific path")
    @GetMapping("/downloadInto/{fileId}")
    public ResponseEntity<Void> downloadFile(
        @RequestHeader("Authorization") String authHeader,
        @PathVariable String fileId,
        @RequestParam String outPath
    ) throws IOException {
        if (!new File(outPath).exists()) {
            throw new IllegalArgumentException("Invalid file path: " + outPath);
        }
        if (!oaiFileRepository.fileExists(fileId)) {
            throw new ResourceNotFoundException("The file doesn't exist or the user doesn't have the right to it.");
        }
        oaiFileService.downloadFile(fileId, outPath);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(
        ResourceNotFoundException ex
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @Operation(summary = "Download a file")
    @GetMapping("/download/{fileId}")
    public ResponseEntity<byte[]> downloadFile(
        @PathVariable String fileId
    ) throws IOException {
        byte[] fileContent = oaiFileService.downloadFile(fileId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileId);
        return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
    }
}
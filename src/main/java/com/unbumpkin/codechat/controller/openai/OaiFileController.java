package com.unbumpkin.codechat.controller.openai;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.unbumpkin.codechat.domain.openai.OaiFile;
import com.unbumpkin.codechat.domain.openai.OaiFile.Purposes;
import com.unbumpkin.codechat.repository.openai.OaiFileRepository;
import com.unbumpkin.codechat.service.openai.OaiFileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;

import org.springframework.http.HttpHeaders;

import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping("/api/openai/files")
@Tag(name = "OpenAI File Controller", description = "Endpoints for managing OpenAI files")
//@SecurityRequirement(name = "bearerAuth")
public class OaiFileController {
    @Autowired
    private final OaiFileService oaiFileService;

    @Autowired
    private final OaiFileRepository oaiFileRepository;
    
    public OaiFileController(OaiFileService oaiFileService, OaiFileRepository oaiFileRepository) {
        this.oaiFileService = oaiFileService;
        this.oaiFileRepository = oaiFileRepository;
    }

    @Operation(summary = "Get all files")
    @GetMapping()
    public ResponseEntity<Iterable<OaiFile>> getAllFiles() {
        try {
            List<String> fileIds = oaiFileService.listFiles();
            List<OaiFile> files = oaiFileRepository.retrieveFiles(fileIds);
            return ResponseEntity.ok(files);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    record UploadDirRequest(String rootDir, String extension, String purpose) {}
    @Operation(summary = "Upload a directory")
    @PostMapping("/uploadDir")
    public ResponseEntity<Map<String,OaiFile>> uploadDir(
        @RequestBody UploadDirRequest request 
    ) {
        try {
            Map<String,OaiFile> files = oaiFileService.uploadFiles(
                request.rootDir(), 
                request.extension(), 
                Purposes.valueOf(request.purpose().toLowerCase())
            );
            try {
                oaiFileRepository.storeOaiFiles(files);
            } catch (IOException | SQLException e) {
                for (OaiFile file : files.values()) {
                    oaiFileService.deleteFile(file.fileId());
                }
                throw e;
            }
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    record UploadFileRequest(String filepath, String purpose) {}
    @Operation(summary = "Upload a file")
    @PutMapping("/uploadFile")
    public ResponseEntity<OaiFile> uploadFile(
        @RequestBody UploadFileRequest request
    ) {
        try {
            OaiFile file = oaiFileService.uploadFile(
                request.filepath(), 
                Purposes.valueOf(request.purpose().toLowerCase()));
            try {
                oaiFileRepository.storeOaiFile(file);
            } catch (IOException e) {
                oaiFileService.deleteFile(file.fileId());
                throw e;
            }
            return ResponseEntity.ok(file);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e);
        }
    }

    @Operation(summary = "get a OaiFile")
    @GetMapping("/{fileId}")
    public ResponseEntity<OaiFile> getFile(
        @PathVariable String fileId
    ) {
        try {
            OaiFile file = oaiFileRepository.retrieveFile(fileId);
            return ResponseEntity.ok(file);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @Operation(summary = "Delete a file")
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(String fileId) {
        try {
            oaiFileService.deleteFile(fileId);
            oaiFileRepository.deleteFile(fileId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @Operation(summary = "Delete all files")
    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllFiles() {
        try {
            List<String> fileIds = oaiFileService.cleanUpFiles();
            oaiFileRepository.deleteAllFiles();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @Operation(summary = "Get all files from a root directory")
    @GetMapping("rootDir")
    public ResponseEntity<List<OaiFile>> getFilesFromRootDir(
        @RequestParam String rootDir
    ) {
        try {
            List<OaiFile> files = oaiFileRepository.retrieveFiles(rootDir);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @Operation(summary = "Get a file info from OpenAI")
    @GetMapping("/fileinfo/{fileId}")
    public ResponseEntity<JsonNode> getFileInfo(
        @PathVariable String fileId) {
        try {
            JsonNode file = oaiFileService.retrieveFile(fileId);
            return ResponseEntity.ok(file);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @Operation(summary = "Download a file to a specific path")
    @GetMapping("/downloadInto/{fileId}")
    public ResponseEntity<Void> downloadFile(
        @PathVariable String fileId, 
        @RequestParam String outPath
    ) {
        try {
            oaiFileService.downloadFile(fileId, outPath);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @Operation(summary = "Download a file")
    @GetMapping("/download/{fileId}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileId) {
        try {
            byte[] fileContent = oaiFileService.downloadFile(fileId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileId);
            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    

    

}
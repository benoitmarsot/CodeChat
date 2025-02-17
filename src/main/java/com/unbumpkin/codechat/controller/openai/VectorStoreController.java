package com.unbumpkin.codechat.controller.openai;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbumpkin.codechat.domain.openai.VectorStore;
import com.unbumpkin.codechat.domain.openai.VectorStore.VectorStoreResponse;
import com.unbumpkin.codechat.repository.openai.VectorStoreRepository;
import com.unbumpkin.codechat.repository.openai.VectorStoreRepository.RepoVectorStoreResponse;
import com.unbumpkin.codechat.service.openai.VectorStoreFile;
import com.unbumpkin.codechat.service.openai.VectorStoreFileBatch;
import com.unbumpkin.codechat.service.openai.VectorStoreService;

@RestController
@RequestMapping("/api/v1/openai/vectorstores")
public class VectorStoreController {
    
    @Autowired
    private final VectorStoreService vectorStoreService;
    @Autowired
    private final VectorStoreRepository vectorStoreRepository;
    @Autowired
    private final ObjectMapper objectMapper;

    public VectorStoreController(VectorStoreService vectorStoreService, VectorStoreRepository vectorStoreRepository,ObjectMapper objectMapper) {
        this.vectorStoreService = vectorStoreService;
        this.vectorStoreRepository = vectorStoreRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a new VectorStore
     * @throws IOException, DataAccessException 
     */
    @PostMapping
    public ResponseEntity<VectorStore> createVectorStore(
        @RequestBody VectorStore request
    ) throws IOException, DataAccessException {
        String vsOaiId = vectorStoreService.createVectorStore(request);
        // Build the VectorStore instance to store in the repository.
        request.setOaiVsId(vsOaiId);
        try {
            vectorStoreRepository.storeVectorStore(request);
        } catch (DataAccessException e) {
            vectorStoreService.deleteVectorStore(vsOaiId);
            throw e;
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    private record FullVectorStoreResponse( 
        String id,
        String name,
        String Description,
        Instant created,
        long dayskeep,
        VectorStoreResponse openai_response
    ) { }
    /**
     * Retrieves a VectorStore based on its ID.
     */
    @GetMapping("/{vsid}")
    public ResponseEntity<FullVectorStoreResponse> getVectorStore(
        @PathVariable String vsid
    ) throws IOException {
        RepoVectorStoreResponse repoStore = vectorStoreRepository.getVectorStoreByOaiId(vsid);
        JsonNode openAIStore = vectorStoreService.retrieveVectorStore(vsid);
        // Deserialize openAIStore to VectorStoreResponse
        VectorStoreResponse vectorStoreResponse = objectMapper.treeToValue(openAIStore, VectorStoreResponse.class);
        FullVectorStoreResponse fullResponse = new FullVectorStoreResponse(
            vectorStoreResponse.id(),
            vectorStoreResponse.name(),
            repoStore.description(),
            repoStore.created(),
            repoStore.dayskeep(),
            vectorStoreResponse
        );
        return ResponseEntity.ok(fullResponse);
    }

    /**
     * Lists all VectorStores stored in the database.
     */
    @GetMapping
    public ResponseEntity<List<RepoVectorStoreResponse>> getAllVectorStores() {
        try {
            List<RepoVectorStoreResponse> stores = vectorStoreRepository.getAllVectorStores();
            return ResponseEntity.ok(stores);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Updates a VectorStore 
     * @throws IOException 
     */
    @PutMapping("/{vsid}")
    public ResponseEntity<Void> updateVectorStore(
        @PathVariable String vsid,
        @RequestBody VectorStore request
    ) throws IOException, DataAccessException {
        vectorStoreRepository.updateVectorStore(request);
        vectorStoreService.modifyVectorStore(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a VectorStore based on its ID.
     * @throws IOException
     */
    @DeleteMapping("/{vsid}")
    public ResponseEntity<Void> deleteVectorStore(
        @PathVariable String vsid
    ) throws IOException {
        vectorStoreService.deleteVectorStore(vsid);
        vectorStoreRepository.deleteVectorStore(vsid);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes all VectorStores 
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAllVectorStores() {
        vectorStoreService.cleanUpVectorStores();
        vectorStoreRepository.deleteAllVectorStores();
        return ResponseEntity.ok().build();
    }

    /**
     * Adds an association between a VectorStore and a single OaiFile.
     * @throws IOException
     */
    @PostMapping("/{vsid}/files/{fileid}")
    public ResponseEntity<Void> addFile(
        @PathVariable String vsOaiId, 
        @PathVariable String fOaiId
    ) throws IOException {
        if(vectorStoreRepository.vectorContainFile(vsOaiId,fOaiId)) {
            throw new IllegalArgumentException("VectorStore already contains this file");
        }
        VectorStoreFile vectorStoreFile = new VectorStoreFile(vsOaiId);        
        vectorStoreFile.createFile(fOaiId);
        vectorStoreRepository.addFile(vsOaiId, fOaiId);

        return ResponseEntity.ok().build();
    }

    /**
     * Adds associations between a VectorStore and multiple OaiFiles.
     * @throws IOException
     */
    @PostMapping("/{vsid}/files")
    public ResponseEntity<Void> addFiles(
        @PathVariable String vsOaiId, @RequestBody List<String> fileOaiIds
    ) throws IOException {
        List<String> existingFiles = vectorStoreRepository.vectorContainAny(vsOaiId, fileOaiIds);
        if(existingFiles.size() > 0) {
            throw new IllegalArgumentException("VectorStore already contains some these files: " + existingFiles);
        }
        VectorStoreFileBatch vsFileBatch = new VectorStoreFileBatch(vsOaiId);        
        vsFileBatch.createBatch(fileOaiIds);

        vectorStoreRepository.addFiles(vsOaiId, fileOaiIds);
        return ResponseEntity.ok().build();
    }

    /**
     * Removes an association between a VectorStore and an OaiFile.
     * @throws IOException
     */
    @DeleteMapping("/{vsid}/files/{fileid}")
    public ResponseEntity<Void> removeFile(
        @PathVariable String vsid, @PathVariable String fileid
    ) throws IOException {
        VectorStoreFile vectorStoreFile = new VectorStoreFile(vsid);        
        vectorStoreFile.deleteFile(fileid);

        vectorStoreRepository.removeFile(vsid, fileid);
        return ResponseEntity.ok().build();
    }

}
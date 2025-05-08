package com.unbumpkin.codechat.model.openai;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.unbumpkin.codechat.service.openai.CCProjectFileManager.Types;

public class VectorStore {

    private int vsid;
    private String oaiVsId;
    private int projectId;
    private String vsname;
    private List<String> fileIds;
    private String vsdesc;
    private Instant created;
    private Integer dayskeep;
    private ExpiresAfter expiresAfter;
    private ChunkingStrategy chunkingStrategy;
    private Map<String, String> metadata; // JSONB column as a String (adjust type as needed)
    private VectorStoreResponse vectorStoreResponse;
    private Types type;
    
    public record ExpiresAfter(String anchor, int days) {
        public ExpiresAfter(int days) {
            this("last_active_at", 0);
        }
        @JsonIgnore
        public boolean isEmpty() {
            return days == 0 ;
        }
        
    }

    public record Static(int max_chunk_size_tokens, int chunk_overlap_tokens) {
        @JsonIgnore
        public boolean isEmpty() {
            return max_chunk_size_tokens == 0 && chunk_overlap_tokens == 0;
        }
    }

    public record ChunkingStrategy(String type, @JsonProperty("static") Static staticProp) {
        public ChunkingStrategy(Static staticProp) {
            this("static", staticProp);
        }

        @JsonIgnore
        public boolean isEmpty() {
            return staticProp.isEmpty();
        }
    }

    // Default constructor
    public VectorStore() {
    }

    // Repository constructor
    public VectorStore(
        int vsId, String oaiVsId, int projectId, String vsname, 
        String vsdesc, Integer dayskeep, Types type
    ) {
        this.vsid = vsId;
        this.oaiVsId = oaiVsId;
        this.projectId = projectId;
        this.vsname = vsname;
        this.vsdesc = vsdesc;
        this.dayskeep = dayskeep;
        this.type = type;
    }

    // OpenAI service constructor
    // Parameterized constructor
    public VectorStore(
        String vsname, String vsdesc, List<String> fileIds, 
        ExpiresAfter expiresAfter, ChunkingStrategy chunkingStrategy, 
        Map<String, String> metadata
    ) {
        this.vsname = vsname;
        this.vsdesc = vsdesc;
        this.fileIds = fileIds;
        this.expiresAfter = expiresAfter;
        this.chunkingStrategy = chunkingStrategy;
        this.metadata = metadata;
    }
    public VectorStoreResponse getVectorStoreResponse() {
        return vectorStoreResponse;
    }
    public void setVectorStoreResponse(VectorStoreResponse vectorStoreResponse) {
        this.vectorStoreResponse = vectorStoreResponse;
    }

    public int getVsid() {
        return vsid;
    }

    public void setVsid(int vsid) {
        this.vsid = vsid;
    }
    public String getOaiVsid() {
        return oaiVsId;
    }

    public void setOaiVsId(String oaiVsId) {
        this.oaiVsId = oaiVsId;
    }
    public int getProjectId() {
        return projectId;
    }

    public String getVsname() {
        return vsname;
    }
    public void setVsname(String vsname) {
        this.vsname = vsname;
    }

    public String getVsdesc() {
        return vsdesc;
    }
    public void setVsdesc(String vsdesc) {
        this.vsdesc = vsdesc;
    }

    public List<String> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<String> fileIds) {
        this.fileIds = fileIds;
    }

    public ExpiresAfter getExpiresAfter() {
        return expiresAfter;
    }

    public void setExpiresAfter(ExpiresAfter expiresAfter) {
        this.expiresAfter = expiresAfter;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public ChunkingStrategy getChunkingStrategy() {
        return chunkingStrategy;
    }

    public void setChunkingStrategy(ChunkingStrategy chunkingStrategy) {
        this.chunkingStrategy = chunkingStrategy;
    }

    public Integer getDayskeep() {
        return dayskeep;
    }

    public void setDayskeep(Integer dayskeep) {
        this.dayskeep = dayskeep;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    public Types getType() {
        return type;
    }

}
package com.unbumpkin.codechat.ai.service;

import com.unbumpkin.codechat.ai.chunker.*;
import com.unbumpkin.codechat.ai.dto.Chunk;
import com.unbumpkin.codechat.ai.dto.EmbeddedChunk;
import com.unbumpkin.codechat.ai.embedder.EmbedderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.unbumpkin.codechat.ai.vectorstore.PgVectorRepository;
import com.unbumpkin.codechat.service.openai.CCProjectFileManager.Types;

import java.util.List;
import java.util.Map;

@Service
public class ChunkingPipeline {
    private final CodeChunker codeChunker;
    private final TextChunker textChunker;
    private final EmbedderService embeddingService;
    @Autowired
    PgVectorRepository pgVectorRepository;

    public ChunkingPipeline(
        CodeChunker codeChunker, TextChunker textChunker,
        EmbedderService embeddingService
    ) {
        this.codeChunker = codeChunker;
        this.textChunker = textChunker;
        this.embeddingService = embeddingService;
    }

    public void process(
        int projectId, Types chunkType, String content, String extension,Map<String,String> metadata
    )  throws Exception {
        List<Chunk> chunks = CodeChunker.isSupportedExtension(extension)
            ? codeChunker.chunk(content,extension,metadata) 
            : textChunker.chunk(content,metadata);
        List<EmbeddedChunk> embedded = embeddingService.embedChunks(chunks);
        pgVectorRepository.saveAll(projectId,chunkType,embedded);
    }
}
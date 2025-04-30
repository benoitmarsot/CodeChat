package com.unbumpkin.codechat.ai.vectorstore;

import java.util.List;

import com.unbumpkin.codechat.ai.dto.EmbeddedChunk;

import static com.unbumpkin.codechat.service.openai.CCProjectFileManager.Types;
 
public interface LocalVectorStore {
    void saveAll(int projectId, Types chunkType, List<EmbeddedChunk> chunks);
    List<EmbeddedChunk> search(int projectId, Types chunkType, float[] embedding, int topK);
    public boolean haveContent(int projectId, Types chunkType);
}
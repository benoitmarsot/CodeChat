package com.unbumpkin.codechat.ai.embedder;

import com.unbumpkin.codechat.ai.dto.Chunk;
import com.unbumpkin.codechat.ai.dto.EmbeddedChunk;
import com.unbumpkin.codechat.service.openai.OaiEmbeddingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OaiEmbedderService extends EmbedderService {
    static public final int MAX_TOKENS = 8192;

    @Autowired
    private OaiEmbeddingService oaiEmbeddingService;

    @Override
    public List<EmbeddedChunk> embedChunks(List<Chunk> chunks) throws Exception {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        
        List<EmbeddedChunk> embeddedChunks = new ArrayList<>();
        int totalTokenCt=0;
        int lastSubmitedIndex=0;
        List<String> chunkContents=new ArrayList<>();
        for(int i=0; i<chunks.size(); i++) {
            this.validateChunk(chunks.get(i));
            int nbTokens=getTokenCount(chunks.get(i).content());
            if(totalTokenCt+nbTokens>MAX_TOKENS) {
                if(chunkContents.size()==0) {
                    throw new IllegalArgumentException("Chunk is too large to embed");
                }
                List<float[]> vectors=oaiEmbeddingService.getOpenAIEmbedding(chunkContents);
                for(int j=0; j<chunkContents.size(); j++) {
                    embeddedChunks.add(new EmbeddedChunk(
                        chunkContents.get(j),
                        vectors.get(j),
                        chunks.get(lastSubmitedIndex+j).metadata()
                    ));
                }
                totalTokenCt=0;
                chunkContents.clear();
                lastSubmitedIndex=i;
            } 
            totalTokenCt+=nbTokens;
            chunkContents.add(chunks.get(i).content());

        }
        return embeddedChunks;
    }
    
    @Override
    public float[] embed(String text) throws Exception {
        // Embedding models (like OpenAI, HuggingFace, etc.) output vectors of floating-point numbers (float[] or double[]), not token IDs.
        // You tokenize text to get int[] tokens, feed those to the model, and the model returns a float[] embedding.
        float[] fVector=oaiEmbeddingService.getOpenAIEmbedding(text);

        return fVector;
    }

    /**
     * Slow embedding method for debugging purposes.
     * Same as the embedChunks method, but processes each chunk individually.
     * Not trying to send multiple chunks to the OpenAI API at once.
     *  
     * @param chunks List of chunks to be embedded.
     * @return List of EmbeddedChunk objects.
     * @throws Exception if an error occurs during the embedding process.
     */
    public List<EmbeddedChunk> embedChunksSlow(List<Chunk> chunks) throws Exception {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        
        List<EmbeddedChunk> embeddedChunks = new ArrayList<>();
        for(Chunk chunk : chunks) {
            this.validateChunk(chunk);
            embeddedChunks.add(new EmbeddedChunk(
                chunk.content(),
                embed(chunk.content()),
                chunk.metadata()
            ));

        }
        return embeddedChunks;
    }

}
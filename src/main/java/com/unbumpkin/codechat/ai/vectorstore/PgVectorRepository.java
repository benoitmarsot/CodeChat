package com.unbumpkin.codechat.ai.vectorstore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbumpkin.codechat.ai.dto.EmbeddedChunk;
import com.unbumpkin.codechat.service.openai.CCProjectFileManager.Types;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;

@Repository
public class PgVectorRepository implements LocalVectorStore {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    ObjectMapper mapper;

    @Override
    public void saveAll(int projectId, Types chunkType, List<EmbeddedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        StringBuilder sql = new StringBuilder("INSERT INTO chunk (projectid, chunktype, content, embedding, metadata) VALUES ");
        for (int i = 0; i < chunks.size(); i++) {
            sql.append("(?, ?, ?, ?::vector, ?::jsonb)");
            if (i < chunks.size() - 1) {
                sql.append(", ");
            }
        }
    
        Object[] params = new Object[chunks.size() * 5];
        int idx = 0;
        for (EmbeddedChunk chunk : chunks) {
            try {
                params[idx++] = projectId;
                params[idx++] = chunkType.toString();
                params[idx++] = chunk.content();
                // Use mapper to serialize embedding as JSON array string, then convert [] to {} for pgvector
                String embeddingJson = mapper.writeValueAsString(chunk.embedding());
                params[idx++] = embeddingJson;
                // Use mapper to serialize metadata as JSON string
                params[idx++] = mapper.writeValueAsString(chunk.metadata());
            } catch (Exception e) {
                throw new RuntimeException("Error serializing chunk for DB insert", e);
            }
        }
        try {
            jdbcTemplate.update(sql.toString(), params);
        } catch (Exception e) {
            System.err.println("Error saving chunks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<EmbeddedChunk> search(int projectId, Types chunkType, float[] embedding, int topK) {
        // Implement cosine similarity search with pgvector SQL
        String sql = "SELECT chunkid, content, metadata FROM chunk WHERE projectid = ? AND chunktype = ? ORDER BY embedding <-> ?::vector LIMIT ?";
        String embeddingJson = null;
        try {
            embeddingJson = mapper.writeValueAsString(embedding);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing embedding for DB search", e);
        }
        try {
            return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    try {
                        String content = rs.getString("content");
                        String metadataJson = rs.getString("metadata");
                        return new EmbeddedChunk(
                            content,
                            embedding,
                            mapper.readValue(metadataJson, new TypeReference<Map<String, String>>() {})
                        );
                    } catch (Exception e) {
                        throw new RuntimeException("Error deserializing chunk from DB", e);
                    }
                },
                projectId,
                chunkType.toString(),
                embeddingJson,
                topK
            );
        } catch (Exception e) {
            System.err.println("Error searching chunks: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
    @Override
    public boolean haveContent(int projectId, Types chunkType) {
        String sql = "select 1 from chunk where projectid = ? and chunktype = ? limit 1";
        List<Integer> result = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> rs.getInt(1),
            projectId,
            chunkType.toString()
        );
        return !result.isEmpty() && result.get(0) == 1;
    }


}
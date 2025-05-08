package com.unbumpkin.codechat.ai.vectorstore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbumpkin.codechat.ai.dto.EmbeddedChunk;
import com.unbumpkin.codechat.ai.dto.SearchChunkResult;
import com.unbumpkin.codechat.service.openai.CCProjectFileManager.Types;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import static java.lang.String.format;

import java.sql.Timestamp;


@Repository
public class PgVectorRepository implements LocalVectorStore {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    ObjectMapper mapper;

    /**
     * Saves a list of embedded chunks to the database for one document.
     */
    @Override
    public void saveAll(int projectId, Types chunkType, List<EmbeddedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        StringBuilder sql = new StringBuilder("INSERT INTO core.chunk (projectid, uri, authorid, chunktype, content, start, embedding, metadata, created_at) VALUES ");
        for (int i = 0; i < chunks.size(); i++) {
            sql.append("(?, ?, ?, ?, ?, ?, ?::vector, ?::jsonb, ?)");
            if (i < chunks.size() - 1) {
                sql.append(", ");
            }
        }
    
        Object[] params = new Object[chunks.size() * 9];
        int idx = 0;
        for (EmbeddedChunk chunk : chunks) {
            try {
                params[idx++] = projectId;
                params[idx++] = chunk.metadata().get("url");
                params[idx++] = chunk.metadata().get("author");
                params[idx++] = chunkType.toString();
                params[idx++] = chunk.content();
                params[idx++] = Integer.parseInt(chunk.metadata().get("start"));
                String embeddingJson = mapper.writeValueAsString(chunk.embedding());
                params[idx++] = embeddingJson;
                params[idx++] = mapper.writeValueAsString(chunk.metadata());
                params[idx++] = getTimestampFromIso8601(chunk.metadata().get("timestamp"));
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
    private static Timestamp getTimestampFromIso8601(String iso8601) {
        Instant instant = Instant.parse(iso8601);
        return Timestamp.from(instant);
    }

    @Override
    public List<SearchChunkResult> search(int projectId, Types chunkType, float[] embedding, Integer maxRows, Float distanceThreshold) {
        String maxRowCond="";
        if(maxRows!= null&& maxRows > 0) {
            maxRowCond = "LIMIT "+maxRows;
        }
        if(distanceThreshold== null || distanceThreshold < 0) {
            distanceThreshold=2f;
        }
        // Implement cosine similarity search with pgvector SQL
        String sql = format("""
            WITH emb AS (SELECT ?::vector AS v)
            SELECT chunkid, content, metadata, (embedding <-> emb.v) AS distance 
            FROM core.chunk, emb
            WHERE projectid = ? AND chunktype = ?
                AND (embedding <-> emb.v) <= ? 
            ORDER BY embedding <-> emb.v 
            %s;
            """,maxRowCond);
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
                        float distance = rs.getFloat("distance");
                        return new SearchChunkResult(
                            content,
                            distance,
                            mapper.readValue(metadataJson, new TypeReference<Map<String, String>>() {})
                        );
                    } catch (Exception e) {
                        throw new RuntimeException("Error deserializing chunk from DB", e);
                    }
                },
                embeddingJson, projectId, chunkType.toString(), distanceThreshold
            );
        } catch (Exception e) {
            System.err.println("Error searching chunks: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
    @Override
    public boolean haveContent(int projectId, Types chunkType) {
        String sql = "select 1 from core.chunk where projectid = ? and chunktype = ? limit 1";
        List<Integer> result = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> rs.getInt(1),
            projectId,
            chunkType.toString()
        );
        return !result.isEmpty() && result.get(0) == 1;
    }


}
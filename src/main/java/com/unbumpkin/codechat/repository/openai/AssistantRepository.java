package com.unbumpkin.codechat.repository.openai;

import java.sql.PreparedStatement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

import com.unbumpkin.codechat.model.openai.Assistant;
import com.unbumpkin.codechat.security.CustomAuthentication;
import com.unbumpkin.codechat.service.openai.AssistantBuilder.ReasoningEfforts;
import com.unbumpkin.codechat.service.openai.BaseOpenAIClient.Models;

@Repository
public class AssistantRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private CustomAuthentication getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CustomAuthentication) {
            return ((CustomAuthentication) authentication);
        }
        throw new IllegalStateException("No authenticated user found");
    }
    private final RowMapper<Assistant> rowMapper = (rs, rowNum) -> new Assistant(
        rs.getInt("aid"),
        rs.getString("oai_aid"),
        rs.getInt("projectid"),
        rs.getString("name"),
        rs.getString("description"),
        rs.getString("instruction"),
        ReasoningEfforts.valueOf(rs.getString("reasoningeffort")),
        Models.fromString(rs.getString("model")),
        rs.getFloat("temperature"),
        rs.getInt("maxresults"),
        rs.getInt("codevsid"),
        rs.getInt("markupvsid"),
        rs.getInt("configvsid"),
        rs.getInt("fullvsid"),
        rs.getTimestamp("created") != null ? rs.getTimestamp("created").toLocalDateTime() : null
    );
    
    /**
     * Add an assistant to the database.
     * @param assistant The assistant to add.
     */
    public int addAssistant(Assistant assistant) {
        String sql = """
            INSERT INTO assistant (
                oai_aid, projectid, name, description, instruction, reasoningeffort, 
                model, temperature, maxresults, codevsid, markupvsid, configvsid, fullvsid
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[] { "aid" });
            ps.setString(1, assistant.oaiAid());
            ps.setInt(2, assistant.projectid());
            ps.setString(3, assistant.name());
            ps.setString(4, assistant.description());
            ps.setString(5, assistant.instruction());
            ps.setString(6, assistant.reasoningEffort().toString());
            ps.setString(7, assistant.model().toString());
            ps.setFloat(8, assistant.temperature());
            ps.setInt(9, assistant.maxResults());
            ps.setInt(10, assistant.codevsid());
            ps.setInt(11, assistant.markupvsid());
            ps.setInt(12, assistant.configvsid());
            ps.setInt(13, assistant.fullvsid());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0 : key.intValue();
    }
    
    /**
     * Update an assistant.
     * @param assistant The assistant to update.
     */
    public void updateAssistant(Assistant assistant) {
        String sql = """
            UPDATE assistant
            SET oai_aid = ?, projectid = ?, name = ?, description = ?, instruction = ?, 
                reasoningeffort = ?, model = ?, temperature = ?, maxresults = ?,
                codevsid = ?, markupvsid = ?, configvsid = ?, fullvsid = ?
            WHERE aid = ?
        """;
        jdbcTemplate.update(
            sql, 
            assistant.oaiAid(),
            assistant.projectid(),
            assistant.name(), 
            assistant.description(),
            assistant.instruction(),
            assistant.reasoningEffort().toString(),
            assistant.model().toString(),
            assistant.temperature(),
            assistant.maxResults(),
            assistant.codevsid(),
            assistant.markupvsid(),
            assistant.configvsid(),
            assistant.fullvsid(),
            assistant.aid()
        );
    }
    /**
     * Retrieve an assistant by ID.
     * @param aid The assistant ID.
     * @return The assistant.
     */
    public Assistant getAssistantById(int aid) {
        String sql = """
            SELECT a.*
            FROM assistant a
            WHERE a.aid = ?
        """;
        return jdbcTemplate.queryForObject(sql, rowMapper, aid);
    }
    /*
     * Get the assistant by project ID.
     * @param projectId The project ID.
     * @return The assistant.
     */
    public Assistant getAssistantByProjectId(int projectId) {
        String sql = """
            SELECT a.*
            FROM assistant a
            WHERE a.projectid = ?
        """;
        return jdbcTemplate.queryForObject(sql, rowMapper, projectId);
    }

    /**
     * Retrieve all assistants.
     * @return A list of assistants.
     */
    public List<Assistant> getAllAssistants() {
        String sql = """
            SELECT a.*
            FROM assistant a
        """;
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Delete an assistant.
     * @param aid The assistant ID.
     */
    public void deleteAssistant(int aid) {
        String sql = """
            DELETE FROM assistant
            WHERE aid = ?
        """;
        jdbcTemplate.update(sql, aid);
    }

    public void deleteAll() {
        CustomAuthentication user = getCurrentUser();
        if (user == null || !user.isAdmin()) {
            throw new IllegalStateException("Only admins can delete all messages");
        }
        // Delete all records in the assistant table
        String deleteAssistantsSql = "DELETE FROM assistant";
        jdbcTemplate.update(deleteAssistantsSql);
    }
}
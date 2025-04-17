package com.unbumpkin.codechat.repository.openai;

import java.sql.PreparedStatement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

import com.unbumpkin.codechat.dto.openai.SocialAssistant;
import com.unbumpkin.codechat.security.CustomAuthentication;
import com.unbumpkin.codechat.service.openai.AssistantBuilder.ReasoningEfforts;
import com.unbumpkin.codechat.service.openai.BaseOpenAIClient.Models;

@Repository
public class SocialAssistantRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private CustomAuthentication getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CustomAuthentication) {
            return ((CustomAuthentication) authentication);
        }
        throw new IllegalStateException("No authenticated user found");
    }
    private final RowMapper<SocialAssistant> rowMapper = (rs, rowNum) -> new SocialAssistant(
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
        rs.getInt("vsid"),
        rs.getTimestamp("created") != null ? rs.getTimestamp("created").toLocalDateTime() : null
    );
    
    /**
     * Add an assistant to the database.
     * @param assistant The assistant to add.
     */
    public int addAssistant(SocialAssistant assistant) {
        String sql = """
            INSERT INTO socialassistant (
                oai_aid, projectid, name, description, instruction, reasoningeffort, 
                model, temperature, maxresults, vsid
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            ps.setInt(10, assistant.vsid());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0 : key.intValue();
    }
    
    /**
     * Update an assistant.
     * @param assistant The assistant to update.
     */
    public void updateAssistant(SocialAssistant assistant) {
        String sql = """
            UPDATE socialassistant
            SET oai_aid = ?, projectid = ?, name = ?, description = ?, instruction = ?, 
                reasoningeffort = ?, model = ?, temperature = ?, maxresults = ?,
                vsid = ?
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
            assistant.vsid(),
            assistant.aid()
        );
    }
    /**
     * Retrieve an assistant by ID.
     * @param aid The assistant ID.
     * @return The assistant.
     */
    public SocialAssistant getAssistantById(int aid) {
        String sql = """
            SELECT a.*
            FROM socialassistant a
            WHERE a.aid = ?
        """;
        return jdbcTemplate.queryForObject(sql, rowMapper, aid);
    }
    /*
     * Get the assistant by project ID.
     * @param projectId The project ID.
     * @return The assistant or null if not found.
     */
    public SocialAssistant getAssistantByProjectId(int projectId) {
        String sql = """
            SELECT a.*
            FROM socialassistant a
            WHERE a.projectid = ?
        """;
        try {
            return jdbcTemplate.queryForObject(sql, rowMapper, projectId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Retrieve all assistants.
     * @return A list of assistants.
     */
    public List<SocialAssistant> getAllAssistants() {
        String sql = """
            SELECT a.*
            FROM socialassistant a
        """;
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Delete an assistant.
     * @param aid The assistant ID.
     */
    public void deleteAssistant(int aid) {
        String sql = """
            DELETE FROM socialassistant
            WHERE aid = ?
        """;
        jdbcTemplate.update(sql, aid);
    }

    public void deleteAll() {
        CustomAuthentication user = getCurrentUser();
        if (user == null || !user.isAdmin()) {
            throw new IllegalStateException("Only admins can delete all social assistants!");
        }
        // Delete all records in the assistant table
        String deleteAssistantsSql = "DELETE FROM socialassistant";
        jdbcTemplate.update(deleteAssistantsSql);
    }
}
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
        rs.getString("name"),
        rs.getString("description"),
        rs.getInt("projectid"),
        rs.getInt("codevsid"),
        rs.getInt("markupvsid"),
        rs.getInt("configvsid"),
        rs.getInt("fullvsid")
    );

    /**
     * Add an assistant to the database.
     * @param assistant The assistant to add.
     */
    public int addAssistant(Assistant assistant) {
        String sql = """
            INSERT INTO core.assistant (oai_aid, name, description, projectid, codevsid, markupvsid, configvsid, fullvsid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[] { "aid" });
            ps.setString(1, assistant.oaiAid());
            ps.setString(2, assistant.name());
            ps.setString(3, assistant.description());
            ps.setInt(4, assistant.projectid());
            ps.setInt(5, assistant.codevsid());
            ps.setInt(6, assistant.markupvsid());
            ps.setInt(7, assistant.configvsid());
            ps.setInt(8, assistant.fullvsid());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key==null?0:key.intValue();
    }

    /**
     * Retrieve an assistant by ID.
     * @param aid The assistant ID.
     * @return The assistant.
     */
    public Assistant getAssistantById(int aid) {
        String sql = """
            SELECT a.*
            FROM core.assistant a
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
            FROM core.assistant a
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
            FROM core.assistant a
        """;
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Update an assistant.
     * @param assistant The assistant to update.
     */
    public void updateAssistant(Assistant assistant) {
        String sql = """
            UPDATE core.assistant
            SET oai_aid = ?, name = ?, description = ?, codevsid = ?, markupvsid = ?, configvsid = ?, fullvsid = ?
            WHERE aid = ?
        """;
        jdbcTemplate.update(sql, assistant.oaiAid(), assistant.name(), assistant.description(), assistant.codevsid(), assistant.markupvsid(), assistant.configvsid(), assistant.fullvsid(), assistant.aid());
    }

    /**
     * Delete an assistant.
     * @param aid The assistant ID.
     */
    public void deleteAssistant(int aid) {
        String sql = """
            DELETE FROM core.assistant
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
        String deleteAssistantsSql = "DELETE FROM core.assistant";
        jdbcTemplate.update(deleteAssistantsSql);
    }
}
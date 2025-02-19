package com.unbumpkin.codechat.repository.openai;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import com.unbumpkin.codechat.domain.openai.Assistant;

@Repository
public class AssistantRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Assistant> rowMapper = (rs, rowNum) -> new Assistant(
        rs.getInt("aid"),
        rs.getString("oai_aid"),
        rs.getString("name"),
        rs.getString("description"),
        rs.getInt("codevsid"),
        rs.getInt("markupvsid"),
        rs.getInt("configvsid"),
        rs.getInt("fullvsid")
    );

    /**
     * Add an assistant to the database.
     * @param assistant The assistant to add.
     */
    public void addAssistant(Assistant assistant) {
        String sql = """
            INSERT INTO core.assistant (aid, oai_aid, name, description, codevsid, markupvsid, configvsid, fullvsid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(sql, assistant.aid(), assistant.oaiAid(), assistant.name(), assistant.description(), assistant.codevsid(), assistant.markupvsid(), assistant.configvsid(), assistant.fullvsid());
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
}
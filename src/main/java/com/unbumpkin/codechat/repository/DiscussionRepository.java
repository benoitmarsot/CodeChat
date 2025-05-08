package com.unbumpkin.codechat.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.unbumpkin.codechat.dto.openai.AssistantTypes;
import com.unbumpkin.codechat.dto.request.DiscussionUpdateRequest;
import com.unbumpkin.codechat.model.Discussion;
import com.unbumpkin.codechat.security.CurrentUserProvider;

@Repository
public class DiscussionRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CurrentUserProvider currentUserProvider;

    private final RowMapper<Discussion> rowMapper = (rs, rowNum) -> new Discussion(
        rs.getInt("did"),
        rs.getInt("projectid"),
        rs.getString("name"),
        rs.getString("description"),
        rs.getBoolean("isfavorite"),
        AssistantTypes.valueOf(rs.getString("assistanttype")),
        rs.getTimestamp("created")
    );

    /**
     * Add a discussion to the database and return the generated ID.
     * @param discussion The discussion to add.
     * @return The generated discussion ID.
     */
    public Discussion addDiscussion(Discussion discussion) {
        String sql = """
            INSERT INTO core.discussion (projectid, name, description, assistanttype)
            SELECT ?, ?, ?, ?
            FROM core.project p
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE p.projectid = ? AND (p.authorid = ? OR sp.userid = ?)
            RETURNING did, projectid, name, description, isfavorite, assistanttype, created
            """;
    
        int userId = currentUserProvider.getCurrentUser().getUserId();
        KeyHolder keyHolder = new GeneratedKeyHolder();
    
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, discussion.projectId());
            ps.setString(2, discussion.name());
            ps.setString(3, discussion.description());
            ps.setString(4, discussion.assistantType().toString());
            ps.setInt(5, discussion.projectId());
            ps.setInt(6, userId);
            ps.setInt(7, userId);
            return ps;
        }, keyHolder);
    
        // Extract the generated keys
        Map<String, Object> keys = Objects.requireNonNull(keyHolder.getKeys(), "Failed to retrieve generated key for discussion");
        int newId = (int) keys.get("did");
        Timestamp created = (Timestamp) keys.get("created");
        
        return new Discussion(newId, discussion.projectId(), discussion.name(), discussion.description(),
                discussion.isFavorite(), discussion.assistantType(), created);
    }

    /**
     * Retrieve a discussion by ID.
     * @param did The discussion ID.
     * @return The discussion.
     */
    public Discussion getDiscussionById(int did) {
        String sql = """
            SELECT d.*
            FROM core.discussion d
            JOIN core.project p ON d.projectid = p.projectid
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE d.did = ? AND (p.authorid = ? OR sp.userid = ?)
        """;
        int userId = currentUserProvider.getCurrentUser().getUserId();
        return jdbcTemplate.queryForObject(sql, rowMapper, did, userId, userId);
    }

    /**
     * Retrieve all discussions by project ID.
     * @param projectId The project ID.
     * @return A list of discussions.
     */
    public List<Discussion> getAllDiscussionsByProjectId(int projectId) {
        String sql = """
                SELECT d.*
                FROM core.discussion d
                JOIN core.project p ON d.projectid = p.projectid
                LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
                WHERE d.projectid = ? AND (p.authorid = ? OR sp.userid = ?)
                order by d.created desc
        """;
        int userId = currentUserProvider.getCurrentUser().getUserId();
        List<Discussion> discussions ;
        try {
            discussions = jdbcTemplate.query(sql, rowMapper, projectId, userId, userId);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return discussions;
    }

    /**
     * Update a discussion.
     * @param discussion The discussion to update.
     * @return The updated discussion.
     */
    public Discussion updateDiscussion(DiscussionUpdateRequest updateRequest) {
        String sql = """
            UPDATE core.discussion d
            SET name = ?, description = ?, isfavorite = ?, assistanttype = ?
            FROM core.project p
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE d.did = ? AND d.projectid = p.projectid AND (p.authorid = ? OR sp.userid = ?)
            RETURNING d.did, d.projectid, d.name, d.description, d.isfavorite, d.assistanttype, d.created
        """;
        int userId = currentUserProvider.getCurrentUser().getUserId();
        return jdbcTemplate.queryForObject(
            sql, rowMapper, updateRequest.name(), updateRequest.description(), 
            updateRequest.isFavorite(), updateRequest.assistantType().toString(), 
            updateRequest.did(), userId, userId
        );
    }

    /**
     * Delete a discussion.
     * @param did The discussion ID.
     */
    public void deleteDiscussion(int did) {
        String sql = """
            WITH auth_check AS (
                SELECT 1 FROM core.discussion d
                JOIN core.project p ON d.projectid = p.projectid
                LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
                WHERE d.did = ? AND (p.authorid = ? OR sp.userid = ?)
            )
            DELETE FROM core.message WHERE did = ? AND EXISTS (SELECT 1 FROM core.auth_check);
            
            WITH auth_check AS (
                SELECT 1 FROM core.discussion d
                JOIN core.project p ON d.projectid = p.projectid
                LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
                WHERE d.did = ? AND (p.authorid = ? OR sp.userid = ?)
            )
            DELETE FROM core.discussion WHERE did = ? AND EXISTS (SELECT 1 FROM core.auth_check);
        """;
        int userId = currentUserProvider.getCurrentUser().getUserId();
        jdbcTemplate.update(sql, did, userId, userId, did, did, userId, userId, did);
    }

    public void deleteAll() {
        if (!currentUserProvider.getCurrentUser().isAdmin()) {
            throw new IllegalStateException("Only admins can delete all messages");
        }

        // Delete all records in the discussion table
        String deleteDiscussionsSql = "DELETE FROM core.discussion";
        jdbcTemplate.update(deleteDiscussionsSql);
    }
    
}
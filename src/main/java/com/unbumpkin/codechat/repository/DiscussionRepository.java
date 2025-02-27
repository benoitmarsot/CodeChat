package com.unbumpkin.codechat.repository;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.unbumpkin.codechat.security.CustomAuthentication;
import com.unbumpkin.codechat.domain.Discussion;

@Repository
public class DiscussionRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Discussion> rowMapper = (rs, rowNum) -> new Discussion(
        rs.getInt("did"),
        rs.getInt("projectid"),
        rs.getString("name"),
        rs.getString("description")
    );

    private int getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CustomAuthentication) {
            return ((CustomAuthentication) authentication).getUserId();
        }
        throw new IllegalStateException("No authenticated user found");
    }

    /**
     * Add a discussion to the database.
     * @param discussion The discussion to add.
     */
    public void addDiscussion(Discussion discussion) {
        String sql = """
            INSERT INTO core.discussion (did, projectid, name, description)
            SELECT ?, ?, ?, ?
            FROM core.project p
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE p.projectid = ? AND (p.authorid = ? OR sp.userid = ?)
        """;
        int userId = getCurrentUserId();
        jdbcTemplate.update(sql, discussion.did(), discussion.projectId(), discussion.name(), discussion.description(), discussion.projectId(), userId, userId);
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
        int userId = getCurrentUserId();
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
        """;
        int userId = getCurrentUserId();
        return jdbcTemplate.query(sql, rowMapper, projectId, userId, userId);
    }

    /**
     * Update a discussion.
     * @param discussion The discussion to update.
     */
    public void updateDiscussion(Discussion discussion) {
        String sql = """
            UPDATE core.discussion d
            SET name = ?, description = ?
            FROM core.project p
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE d.did = ? AND d.projectid = p.projectid AND (p.authorid = ? OR sp.userid = ?)
        """;
        int userId = getCurrentUserId();
        jdbcTemplate.update(sql, discussion.name(), discussion.description(), discussion.did(), userId, userId);
    }

    /**
     * Delete a discussion.
     * @param did The discussion ID.
     */
    public void deleteDiscussion(int did) {
        String sql = """
            DELETE FROM core.discussion d
            USING core.project p
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE d.did = ? AND d.projectid = p.projectid AND (p.authorid = ? OR sp.userid = ?)
        """;
        int userId = getCurrentUserId();
        jdbcTemplate.update(sql, did, userId, userId);
    }
    private CustomAuthentication getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CustomAuthentication) {
            return ((CustomAuthentication) authentication);
        }
        throw new IllegalStateException("No authenticated user found");
    }

    public void deleteAll() {
        CustomAuthentication currentUser = getCurrentUser();
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new IllegalStateException("Only admins can delete all messages");
        }

        // Delete all records in the discussion table
        String deleteDiscussionsSql = "DELETE FROM core.discussion";
        jdbcTemplate.update(deleteDiscussionsSql);
    }
}
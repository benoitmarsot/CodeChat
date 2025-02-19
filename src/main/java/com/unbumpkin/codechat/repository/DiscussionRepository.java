package com.unbumpkin.codechat.repository;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
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

    /**
     * Add a discussion to the database.
     * @param discussion The discussion to add.
     * @param userId The user ID.
     */
    public void addDiscussion(Discussion discussion, int userId) {
        String sql = """
            INSERT INTO core.discussion (did, projectid, name, description)
            SELECT ?, ?, ?, ?
            FROM core.project p
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE p.projectid = ? AND (p.authorid = ? OR sp.userid = ?)
        """;
        jdbcTemplate.update(sql, discussion.did(), discussion.projectId(), discussion.name(), discussion.description(), discussion.projectId(), userId, userId);
    }

    /**
     * Retrieve a discussion by ID.
     * @param did The discussion ID.
     * @param userId The user ID.
     * @return The discussion.
     */
    public Discussion getDiscussionById(int did, int userId) {
        String sql = """
            SELECT d.*
            FROM core.discussion d
            JOIN core.project p ON d.projectid = p.projectid
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE d.did = ? AND (p.authorid = ? OR sp.userid = ?)
        """;
        return jdbcTemplate.queryForObject(sql, rowMapper, did, userId, userId);
    }

    /**
     * Retrieve all discussions by project ID.
     * @param projectId The project ID.
     * @param userId The user ID.
     * @return A list of discussions.
     */
    public List<Discussion> getAllDiscussionsByProjectId(int projectId, int userId) {
        String sql = """
            SELECT d.*
            FROM core.discussion d
            JOIN core.project p ON d.projectid = p.projectid
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE d.projectid = ? AND (p.authorid = ? OR sp.userid = ?)
        """;
        return jdbcTemplate.query(sql, rowMapper, projectId, userId, userId);
    }

    /**
     * Update a discussion.
     * @param discussion The discussion to update.
     * @param userId The user ID.
     */
    public void updateDiscussion(Discussion discussion, int userId) {
        String sql = """
            UPDATE core.discussion d
            SET name = ?, description = ?
            FROM core.project p
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE d.did = ? AND d.projectid = p.projectid AND (p.authorid = ? OR sp.userid = ?)
        """;
        jdbcTemplate.update(sql, discussion.name(), discussion.description(), discussion.did(), userId, userId);
    }

    /**
     * Delete a discussion.
     * @param did The discussion ID.
     * @param userId The user ID.
     */
    public void deleteDiscussion(int did, int userId) {
        String sql = """
            DELETE FROM core.discussion d
            USING core.project p
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE d.did = ? AND d.projectid = p.projectid AND (p.authorid = ? OR sp.userid = ?)
        """;
        jdbcTemplate.update(sql, did, userId, userId);
    }
}
package com.unbumpkin.codechat.repository;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.unbumpkin.codechat.security.CustomAuthentication;
import com.unbumpkin.codechat.domain.Project;

@Repository
public class ProjectRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Project> rowMapper = (rs, rowNum) -> new Project(
        rs.getInt("projectid"),
        rs.getString("name"),
        rs.getString("description"),
        rs.getInt("authorid"),
        rs.getInt("aid")
    );

    private int getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CustomAuthentication) {
            return ((CustomAuthentication) authentication).getUserId();
        }
        throw new IllegalStateException("No authenticated user found");
    }

    /**
     * Add a project to the database.
     * @param project The project to add.
     */
    public void addProject(Project project) {
        String sql = "INSERT INTO core.project (projectid, name, description, authorid, aid) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, project.projectId(), project.name(), project.description(), 
            getCurrentUserId(), project.assistantId());
    }

    /**
     * Retrieve a project by ID.
     * @param projectId The project ID.
     * @return The project.
     */
    public Project getProjectById(int projectId) {
        String sql = """
            SELECT p.*
            FROM core.project p
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE p.projectid = ? AND (p.authorid = ? OR sp.userid = ?)
        """;
        int userId = getCurrentUserId();
        return jdbcTemplate.queryForObject(sql, rowMapper, projectId, userId, userId);
    }

    /**
     * Retrieve all projects by user ID.
     * @return A list of projects.
     */
    public List<Project> getAllProjects() {
        String sql = """
            SELECT p.*
            FROM core.project p
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE p.authorid = ? OR sp.userid = ?
        """;
        int userId = getCurrentUserId();
        return jdbcTemplate.query(sql, rowMapper, userId, userId);
    }

    /**
     * Update a project.
     * @param project The project to update.
     */
    public void updateProject(Project project) {
        String sql = """
            UPDATE core.project
            SET name = ?, description = ?, aid = ?
            WHERE projectid = ? AND (authorid = ? OR EXISTS (
                SELECT 1 FROM core.sharedproject
                WHERE projectid = ? AND userid = ?
            ))
        """;
        int userId = getCurrentUserId();
        jdbcTemplate.update(sql, project.name(), project.description(), project.assistantId(), 
            project.projectId(), userId, project.projectId(), userId);
    }

    /**
     * Delete a project.
     * @param projectId The project ID.
     */
    public void deleteProject(int projectId) {
        String sql = """
            DELETE FROM core.project
            WHERE projectid = ? AND (authorid = ? OR EXISTS (
                SELECT 1 FROM core.sharedproject
                WHERE projectid = ? AND userid = ?
            ))
        """;
        int userId = getCurrentUserId();
        jdbcTemplate.update(sql, projectId, userId, projectId, userId);
    }

    /**
     * Retrieve all users with access to a specific project.
     * @param projectId The project ID.
     * @return A list of user IDs.
     */
    public List<Integer> getUsersWithAccess(int projectId) {
        String sql = """
            SELECT sp.userid
            FROM core.sharedproject sp
            JOIN core.project p ON sp.projectid = p.projectid
            WHERE sp.projectid = ? AND (p.authorid = ? OR sp.userid = ?)
        """;
        int userId = getCurrentUserId();
        return jdbcTemplate.queryForList(sql, Integer.class, projectId, userId, userId);
    }

    /**
     * Grant a user access to a project.
     * @param projectId The project ID.
     * @param targetUserId The target user ID.
     */
    public void grantUserAccessToProject(int projectId, int targetUserId) {
        String sql = """
            INSERT INTO core.sharedproject (projectid, userid)
            SELECT ?, ?
            FROM core.project p
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE p.projectid = ? AND (p.authorid = ? OR sp.userid = ?)
        """;
        int currentUserId = getCurrentUserId();
        jdbcTemplate.update(sql, projectId, targetUserId, projectId, currentUserId, currentUserId);
    }

    /**
     * Revoke a user's access to a project.
     * @param projectId The project ID.
     * @param targetUserId The target user ID.
     */
    public void revokeUserAccessFromProject(int projectId, int targetUserId) {
        String sql = """
            DELETE FROM core.sharedproject
            WHERE projectid = ? AND userid = ? AND EXISTS (
                SELECT 1 FROM core.project p
                LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
                WHERE p.projectid = ? AND (p.authorid = ? OR sp.userid = ?)
            )
        """;
        int currentUserId = getCurrentUserId();
        jdbcTemplate.update(sql, projectId, targetUserId, projectId, currentUserId, currentUserId);
    }
}
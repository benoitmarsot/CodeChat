package com.unbumpkin.codechat.repository;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
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

    /**
     * Add a project to the database.
     * @param project The project to add.
     * @param authorId The author ID.
     */
    public void addProject(Project project, int authorId) {
        String sql = "INSERT INTO core.project (projectid, name, description, authorid, aid) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, project.projectId(), project.name(), project.description(), authorId, project.assistantId());
    }

    /**
     * Retrieve a project by ID.
     * @param projectId The project ID.
     * @param userId The user ID.
     * @return The project.
     */
    public Project getProjectById(int projectId, int userId) {
        String sql = """
            SELECT p.*
            FROM core.project p
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE p.projectid = ? AND (p.authorid = ? OR sp.userid = ?)
        """;
        return jdbcTemplate.queryForObject(sql, rowMapper, projectId, userId, userId);
    }

    /**
     * Retrieve all projects by user ID.
     * @param userId The user ID.
     * @return A list of projects.
     */
    public List<Project> getAllProjects(int userId) {
        String sql = """
            SELECT p.*
            FROM core.project p
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE p.authorid = ? OR sp.userid = ?
        """;
        return jdbcTemplate.query(sql, rowMapper, userId, userId);
    }

    /**
     * Update a project.
     * @param project The project to update.
     * @param userId The user ID.
     */
    public void updateProject(Project project, int userId) {
        String sql = """
            UPDATE core.project
            SET name = ?, description = ?, authorid = ?, aid = ?
            WHERE projectid = ? AND (authorid = ? OR EXISTS (
                SELECT 1 FROM core.sharedproject
                WHERE projectid = ? AND userid = ?
            ))
        """;
        jdbcTemplate.update(sql, project.name(), project.description(), project.authorId(), project.assistantId(), project.projectId(), userId, project.projectId(), userId);
    }

    /**
     * Delete a project.
     * @param projectId The project ID.
     * @param userId The user ID.
     */
    public void deleteProject(int projectId, int userId) {
        String sql = """
            DELETE FROM core.project
            WHERE projectid = ? AND (authorid = ? OR EXISTS (
                SELECT 1 FROM core.sharedproject
                WHERE projectid = ? AND userid = ?
            ))
        """;
        jdbcTemplate.update(sql, projectId, userId, projectId, userId);
    }

    /**
     * Retrieve all users with access to a specific project.
     * @param projectId The project ID.
     * @param userId The user ID.
     * @return A list of user IDs.
     */
    public List<Integer> getUsersWithAccess(int projectId, int userId) {
        String sql = """
            SELECT sp.userid
            FROM core.sharedproject sp
            JOIN core.project p ON sp.projectid = p.projectid
            WHERE sp.projectid = ? AND (p.authorid = ? OR sp.userid = ?)
        """;
        return jdbcTemplate.queryForList(sql, Integer.class, projectId, userId, userId);
    }

    /**
     * Grant a user access to a project.
     * @param projectId The project ID.
     * @param userId The user ID.
     * @param authorId The author ID.
     */
    public void grantUserAccessToProject(int projectId, int userId, int authorId) {
        String sql = """
            INSERT INTO core.sharedproject (projectid, userid)
            SELECT ?, ?
            FROM core.project p
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE p.projectid = ? AND (p.authorid = ? OR sp.userid = ?)
        """;
        jdbcTemplate.update(sql, projectId, userId, projectId, authorId, authorId);
    }

    /**
     * Revoke a user's access to a project.
     * @param projectId The project ID.
     * @param userId The user ID.
     * @param authorId The author ID.
     */
    public void revokeUserAccessFromProject(int projectId, int userId, int authorId) {
        String sql = """
            DELETE FROM core.sharedproject
            WHERE projectid = ? AND userid = ? AND EXISTS (
                SELECT 1 FROM core.project p
                LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
                WHERE p.projectid = ? AND (p.authorid = ? OR sp.userid = ?)
            )
        """;
        jdbcTemplate.update(sql, projectId, userId, projectId, authorId, authorId);
    }
}
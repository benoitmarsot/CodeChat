package com.unbumpkin.codechat.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbumpkin.codechat.dto.response.ProjectWithResource;
import com.unbumpkin.codechat.model.Project;
import com.unbumpkin.codechat.security.CustomAuthentication;

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
        /**
     * Add a project to the database and return the generated project ID.
     * @param project The project to add.
     * @return The generated project ID.
     */
    public int addProject(String name, String description) {
        String sql = "INSERT INTO core.project (name, description, authorid) VALUES (?, ?, ?) RETURNING projectid";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setInt(3, getCurrentUserId());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key==null ? 0 : key.intValue();
    }

    /**
     * Retrieve a project by ID with associated resources.
     * @param projectId The project ID.
     * @return The project with resource information.
     */
    public ProjectWithResource getProjectWithResById(int projectId) {
        String sql = """
            SELECT 
                p.projectid, p.name, p.description, p.authorid, 
                a.aid, a.model, a.description as assistant_description,
                COALESCE(json_agg(pr.uri ) FILTER (WHERE pr.uri IS NOT NULL), '[]'::json) as resources
            FROM core.project p
            INNER JOIN core.assistant a ON a.projectid = p.projectid
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            LEFT JOIN core.projectresource pr ON p.projectid = pr.projectid
            WHERE p.isdeleted=false AND p.projectid = ? AND (p.authorid = ? OR sp.userid = ?)
            GROUP BY p.projectid, p.name, p.description, p.authorid, a.aid, a.model, a.description
            """;
        
        int userId = getCurrentUserId();
        
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                // Parse the JSON array of resources to a Java List
                List<String> resourceUris = new ArrayList<>();
                String resourcesJson = rs.getString("resources");
                if (resourcesJson != null && !resourcesJson.equals("[]")) {
                    try {
                        JsonNode resourcesNode = new ObjectMapper().readTree(resourcesJson);
                        if (resourcesNode.isArray()) {
                            for (JsonNode node : resourcesNode) {
                                resourceUris.add(node.asText());
                            }
                        }
                    } catch (JsonProcessingException e) {
                        // Handle JSON parsing error
                        e.printStackTrace();
                    }
                }
                
                return new ProjectWithResource(
                    rs.getInt("projectid"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("authorid"),
                    rs.getInt("aid"),
                    rs.getString("model"),
                    rs.getString("assistant_description"),
                    resourceUris
                );
            }, projectId, userId, userId);
        } catch (EmptyResultDataAccessException e) {
            return null; // Project not found or user doesn't have access
        }
    }
    /**
     * Retrieve a project by ID with associated resources.
     * @param projectId The project ID.
     * @return The project with resource information.
     */
    public Project getProjectById(int projectId) {
        String sql = """
            SELECT 
                p.projectid, p.name, p.description, p.authorid, 
                a.aid
            FROM core.project p
            INNER JOIN core.assistant a ON a.projectid = p.projectid
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE p.isdeleted=false AND p.projectid = ? AND (p.authorid = ? OR sp.userid = ?)
            """;
        
        int userId = getCurrentUserId();
        
        try {
            return jdbcTemplate.queryForObject(sql, rowMapper, projectId, userId, userId);
        } catch (EmptyResultDataAccessException e) {
            return null; // Project not found or user doesn't have access
        }
    }

    /**
     * Retrieve all projects by user ID.
     * @return A list of projects.
     */
    public List<Project> getAllProjects() {
        String sql = """
            SELECT p.projectid, p.name, p.description, p.authorid, a.aid
            FROM core.project p
                INNER JOIN core.Assistant a ON a.projectid = p.projectid
                LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE p.isdeleted=false and (p.authorid = ? OR sp.userid = ?)
        """;
        int userId = getCurrentUserId();
        try {
            List<Project> projects = jdbcTemplate.query(sql, rowMapper, userId, userId);
            return projects;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Update a project.
     * @param project The project to update.
     */
    public void updateProject(Project project) {
        String sql = """
            UPDATE core.project
            SET name = ?, description = ?
            WHERE isdeleted=false and projectid = ? AND (authorid = ? OR EXISTS (
                SELECT 1 FROM core.sharedproject
                WHERE projectid = ? AND userid = ?
            ))
        """;
        int userId = getCurrentUserId();
        jdbcTemplate.update(sql, project.name(), project.description(),  
            project.projectId(), userId, project.projectId(), userId);
    }

    /**
     * Delete a project.
     * @param projectId The project ID.
     */
    public void deleteProject(int projectId) {
        String sql = """
            DELETE FROM core.project
            WHERE projectid = ? AND authorid = ?
        """;
        try {
            int userId = getCurrentUserId();
            jdbcTemplate.update(sql, projectId, userId, projectId, userId);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Mark project for deletion
     * @param projectId
     * @return
     */
    public void markForDeletion(int projectId) {
        String sql = """
            UPDATE core.project
            SET isdeleted = true
            WHERE projectid = ? AND authorid = ?
        """;
        try {
            int userId = getCurrentUserId();
            jdbcTemplate.update(sql, projectId, userId);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
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
            WHERE p.isdeleted=false and sp.projectid = ? AND (p.authorid = ? OR sp.userid = ?)
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

        // Delete all records in the sharedproject table
        String deleteSharedProjectsSql = "DELETE FROM core.sharedproject";
        jdbcTemplate.update(deleteSharedProjectsSql);

        // Delete all records in the discussion table
        String deleteDiscussionsSql = "DELETE FROM core.discussion";
        jdbcTemplate.update(deleteDiscussionsSql);

        // Delete all records in the project table
        String deleteProjectsSql = "DELETE FROM core.project";
        jdbcTemplate.update(deleteProjectsSql);

        // Delete all records in usersecret table
        String deleteSecretsSql = "DELETE FROM core.usersecret";
        jdbcTemplate.update(deleteSecretsSql);
    }
}
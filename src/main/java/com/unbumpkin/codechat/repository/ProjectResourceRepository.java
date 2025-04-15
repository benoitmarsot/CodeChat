package com.unbumpkin.codechat.repository;

import com.unbumpkin.codechat.model.ProjectResource;
import com.unbumpkin.codechat.model.UserSecret;
import com.unbumpkin.codechat.model.ProjectResource.ResTypes;
import com.unbumpkin.codechat.model.UserSecret.Labels;
import com.unbumpkin.codechat.security.CustomAuthentication;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ProjectResourceRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final String encryptionKey; 
    
    public ProjectResourceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        if( System.getenv("USER_SECRET_KEY")!=null) {
            this.encryptionKey = System.getenv("USER_SECRET_KEY");
        } else {
            // Only for development - use a proper key management solution in production
            this.encryptionKey = "dev-key-1234567890abcdefghijklmn";
            System.out.println("WARNING: Using default encryption key. Set USER_SECRET_KEY for production.");
        }
    }

    private int getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CustomAuthentication) {
            return ((CustomAuthentication) authentication).getUserId();
        }
        throw new IllegalStateException("No authenticated user found");
    }
    
    public ProjectResource createResource(int projectId, String uri, ResTypes resType, Map<Labels, UserSecret> secrets) {
        // Insert the project resource
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO projectresource (projectid, uri, restype) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, projectId);
            ps.setString(2, uri);
            ps.setString(3, resType.toString());
            return ps;
        }, keyHolder);
        
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys == null) {
            throw new IllegalStateException("Failed to retrieve generated key for project resource");
        }
        int prId = (int) keys.get("prid");
        
        // Insert any associated secrets
        if (secrets != null && !secrets.isEmpty()) {
            for (UserSecret secret : secrets.values()) {
                addSecret(prId, secret.label(), secret.value());
            }
        }
        
        return new ProjectResource(prId, projectId, uri, resType, secrets != null ? secrets : new HashMap<>());
    }
    
    public ProjectResource updateResource(int prId, String uri, ResTypes resType) {
        jdbcTemplate.update(
            "UPDATE projectresource SET uri = ?, restype=? WHERE prid = ?",
            uri, resType.toString(), prId
        );
        
        // Get the updated resource with its secrets
        return getResource(prId);
    }
    
    public void deleteResource(int prId) {
        // Delete associated secrets first (cascading delete should handle this, but being explicit)
        jdbcTemplate.update("DELETE FROM usersecret WHERE prid = ?", prId);
        
        // Delete the resource
        jdbcTemplate.update("DELETE FROM projectresource WHERE prid = ?", prId);
    }
    /**
     * Get all resources uri for a project
     * @param projectId
     * @return
     */
    public List<String> getProjectResourcesUri(int projectId) {
        return jdbcTemplate.query(
            "SELECT pr.uri FROM projectresource pr WHERE pr.projectid = ?",
            (rs, rowNum) -> rs.getString("uri"),
            projectId
        );
    }
    /**
     * Get the ID of a resource by its URI
     * @param projectId The project ID
     * @param uri The resource URI
     * @return The resource ID, or null if not found
     */
    public Integer getResourceId(int projectId, String uri) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT prid FROM projectresource WHERE projectid = ? AND uri = ?",
                Integer.class,
                projectId, uri
            );
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Get all resources for a project
     * @param projectId The project ID
     * @return A list of ProjectResource objects
     */
    public List<ProjectResource> getResources(int projectId) {
        // First get all resources
        List<ProjectResource> resources = jdbcTemplate.query(
            "SELECT prid, projectid, uri, restype FROM projectresource WHERE projectid = ?",
            (rs, rowNum) -> new ProjectResource(
                rs.getInt("prid"),
                rs.getInt("projectid"),
                rs.getString("uri"),
                ResTypes.valueOf(rs.getString("restype")),
                new HashMap<>()
            ),
            projectId
        );
        
        // For each resource, fetch its secrets
        for (int i = 0; i < resources.size(); i++) {
            ProjectResource resource = resources.get(i);
            
            // Get the secrets for this resource
            List<UserSecret> secretsList = jdbcTemplate.query(
                "SELECT userid, label, value FROM usersecret WHERE prid = ?",
                (rs, rowNum) -> new UserSecret(
                    rs.getInt("userid"),
                    Labels.valueOf(rs.getString("label")),
                    decryptValue(rs.getString("value"))
                ),
                resource.prId()
            );
            
            // Convert the list to a map
            Map<Labels, UserSecret> secretsMap = new HashMap<>();
            for (UserSecret secret : secretsList) {
                secretsMap.put(secret.label(), secret);
            }
            
            // Create a new ProjectResource with the secrets map
            resources.set(i, new ProjectResource(
                resource.prId(),
                resource.projectId(),
                resource.uri(),
                resource.resType(),
                secretsMap
            ));
        }
        
        return resources;
    }
    
    public ProjectResource getResource(int prId) {
        // Get the project resource
        ProjectResource resource = jdbcTemplate.queryForObject(
            "SELECT prid, projectid, uri FROM projectresource WHERE prid = ?",
            (rs, rowNum) -> new ProjectResource(
                rs.getInt("prid"),
                rs.getInt("projectid"),
                rs.getString("uri"),
                ResTypes.valueOf(rs.getString("restype")),
                new HashMap<>()
            ),
            prId
        );
        
        if (resource != null) {
            // Get the secrets for this resource
            List<UserSecret> secretsList = jdbcTemplate.query(
                "SELECT userid, label, value FROM usersecret WHERE prid = ?",
                (rs, rowNum) -> new UserSecret(
                    rs.getInt("userid"),
                    Labels.valueOf(rs.getString("label")),
                    decryptValue(rs.getString("value"))
                ),
                prId
            );
            
            // Convert the list to a map
            Map<Labels, UserSecret> secretsMap = new HashMap<>();
            for (UserSecret secret : secretsList) {
                secretsMap.put(secret.label(), secret);
            }
            
            // Create a new Projectresource with the secrets map
            return new ProjectResource(
                resource.prId(),
                resource.projectId(),
                resource.uri(),
                resource.resType(),
                secretsMap
            );
        }
        
        return null;
    }
    
    public void updateSecret(int prId, Labels label, String value) {
        if(value == null) {
            deleteSecret(prId, label);
            return;
        }
        int userId = getCurrentUserId();
        jdbcTemplate.update(
            "UPDATE usersecret SET value = ? WHERE prid = ? AND userid = ? AND label = ?",
            encryptValue(value), prId, userId, label.toString()
        );
    }
    
    public void deleteSecret(int prId, Labels label) {
        jdbcTemplate.update(
            "DELETE FROM usersecret WHERE prid = ? AND label = ?",
            prId, label.toString()
        );
    }
    
    public void addSecret(int prId, Labels label, String value) {
        if(value == null) {
            return;
        }
        int userId = getCurrentUserId();
        jdbcTemplate.update(
            "INSERT INTO usersecret (userid, prid, label, value) VALUES (?, ?, ?, ?) " +
            "ON CONFLICT (userid, prid, label) DO UPDATE SET value = EXCLUDED.value",
            userId, prId, label.toString(), encryptValue(value)
        );
    }

    public void removeProjectResourceSecrets(int prId) {
        jdbcTemplate.update(
            "DELETE FROM usersecret WHERE prid = ?",
            prId
        );
    }
    
    // Helper methods for encryption and decryption
    // TODO: Consider using a more secure approach than having key in ./zshrc
    //       aws KMS? (maybe Linus)
    
    private String encryptValue(String value) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting value", e);
        }
    }
    
    private String decryptValue(String encryptedValue) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decodedValue = Base64.getDecoder().decode(encryptedValue);
            byte[] decryptedBytes = cipher.doFinal(decodedValue);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting value", e);
        }
    }

}
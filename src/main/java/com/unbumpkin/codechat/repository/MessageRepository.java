package com.unbumpkin.codechat.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.unbumpkin.codechat.security.CustomAuthentication;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbumpkin.codechat.dto.request.MessageCreateRequest;
import com.unbumpkin.codechat.dto.social.SocialReferences;
import com.unbumpkin.codechat.model.Message;

@Repository
public class MessageRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    private final RowMapper<Message> rowMapper = (rs, rowNum) -> {
        SocialReferences socialreferences = null;
        try {
            socialreferences=rs.getString("socialreference") == null ? null :
                objectMapper.readValue(rs.getString("socialreference"), SocialReferences.class);
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
            return new Message(
                rs.getInt("msgid"),
                rs.getInt("did"),
                rs.getString("role"),
                rs.getInt("authorid"),
                rs.getString("message"),
                socialreferences,
                rs.getTimestamp("created")
            );
    };

    private int getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CustomAuthentication) {
            return ((CustomAuthentication) authentication).getUserId();
        }
        throw new IllegalStateException("No authenticated user found");
    }

    private CustomAuthentication getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CustomAuthentication) {
            return ((CustomAuthentication) authentication);
        }
        throw new IllegalStateException("No authenticated user found");
    }

    /**
     * Add a message to the database.
     * @param IssueComment The message to add.
     * @throws JsonProcessingException 
     * @throws DataAccessException 
     */
    public Message addMessage( MessageCreateRequest createRequest) throws DataAccessException, JsonProcessingException {
        String sql = """
            INSERT INTO core.message (did, role, authorid, message, socialreference)
            SELECT ?, ?, ?, ?, ?::jsonb
            WHERE EXISTS (
                SELECT 1
                FROM core.project p
                JOIN core.discussion d ON p.projectid = d.projectid
                LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
                WHERE d.did = ? AND (sp.userid = ? OR p.authorid = ?)
            )
            RETURNING msgid, did, role, authorid, message, socialreference, created
        """;
        int userId = getCurrentUserId();
        //( int did, String role, int authorid, String message) {
        return jdbcTemplate.queryForObject(
            sql, rowMapper, createRequest.did(), createRequest.role(),  userId, createRequest.message(), 
            createRequest.socialAnswer()!=null?objectMapper.writeValueAsString(createRequest.socialAnswer()):null,  // Assuming this is a JSONB field
            createRequest.did(), userId, userId
        );
    }

    /**
     * Retrieve a message by ID.
     * @param msgid The message ID.
     * @return The message.
     */
    public Message getMessageById(int msgid) {
        String sql = """
            SELECT m.*
            FROM core.message m
            JOIN core.discussion d ON m.did = d.did
            JOIN core.project p ON d.projectid = p.projectid
            LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE m.msgid = ? AND (sp.userid = ? OR p.authorid = ?)
        """;
        return jdbcTemplate.queryForObject(sql, rowMapper, msgid, getCurrentUserId(), getCurrentUserId());
    }

    /**
     * Retrieve all messages by discussion ID.
     * @param did The discussion ID.
     * @return A list of messages.
     */
    public List<Message> getAllMessagesByDiscussionId(int did) {
        String sql = """
            SELECT m.*
            FROM core.message m
                JOIN core.discussion d ON m.did = d.did
                JOIN core.project p ON d.projectid = p.projectid
                LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
            WHERE m.did = ? AND (sp.userid = ? OR p.authorid = ?)
            order by m.msgid asc
        """;
        return jdbcTemplate.query(sql, rowMapper, did, getCurrentUserId(), getCurrentUserId());
    }

    /**
     * Update a message.
     * @param message The message to update.
     */
    public void updateMessage(Message message) {
        String sql = """
            UPDATE core.message m
            SET role = ?, authorid = ?, message = ?
            WHERE m.msgid = ? AND EXISTS (
                SELECT 1
                FROM core.project p
                JOIN core.discussion d ON p.projectid = d.projectid
                LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
                WHERE d.did = ? AND (sp.userid = ? OR p.authorid = ?)
            )
        """;
        int userId = getCurrentUserId();
        jdbcTemplate.update(sql, message.role(), message.authorid(), message.message(), message.msgid(), message.discussionId(), userId, userId);
    }

    /**
     * Delete a message.
     * @param msgid The message ID.
     */
    public void deleteMessage(int msgid) {
        String sql = """
            DELETE FROM core.message
            WHERE msgid = ? AND EXISTS (
                SELECT 1
                FROM core.project p
                JOIN core.discussion d ON p.projectid = d.projectid
                LEFT JOIN core.sharedproject sp ON p.projectid = sp.projectid
                WHERE d.did = message.did AND (sp.userid = ? OR p.authorid = ?)
            )
        """;
        jdbcTemplate.update(sql, msgid, getCurrentUserId(), getCurrentUserId());
    }

    /**
     * Delete all messages.
     */
    public void deleteAll() {
        CustomAuthentication user = getCurrentUser();
        if (user == null || !user.isAdmin()) {
            throw new IllegalStateException("Only admins can delete all messages");
        }
        String sql = "DELETE FROM core.message";
        jdbcTemplate.update(sql);
    }
}
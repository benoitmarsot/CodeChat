package com.unbumpkin.codechat.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.unbumpkin.codechat.security.CustomAuthentication;

import com.unbumpkin.codechat.domain.Message;

@Repository
public class MessageRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Message> rowMapper = (rs, rowNum) -> new Message(
        rs.getInt("msgid"),
        rs.getInt("did"),
        rs.getString("role"),
        rs.getInt("authorid"),
        rs.getString("message")
    );

    private int getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CustomAuthentication) {
            return ((CustomAuthentication) authentication).getUserId();
        }
        throw new IllegalStateException("No authenticated user found");
    }

    /**
     * Add a message to the database.
     * @param message The message to add.
     */
    public void addMessage(int did, Message message) {
        String sql = """
            INSERT INTO core.message (msgid, did, role, authorid, message)
            SELECT ?, ?, ?, ?, ?
            WHERE EXISTS (
                SELECT 1
                FROM core.sharedproject sp
                JOIN core.discussion d ON sp.projectid = d.projectid
                WHERE d.did = ? AND sp.userid = ?
            )
        """;
        int userId = getCurrentUserId();
        jdbcTemplate.update(sql, message.msgid(), message.discussionId(), message.role(), message.authorid(), message.message(), message.discussionId(), userId);
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
            JOIN core.sharedproject sp ON d.projectid = sp.projectid
            WHERE m.msgid = ? AND sp.userid = ?
        """;
        return jdbcTemplate.queryForObject(sql, rowMapper, msgid, getCurrentUserId());
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
            JOIN core.sharedproject sp ON d.projectid = sp.projectid
            WHERE m.did = ? AND sp.userid = ?
        """;
        return jdbcTemplate.query(sql, rowMapper, did, getCurrentUserId());
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
                FROM core.sharedproject sp
                JOIN core.discussion d ON sp.projectid = d.projectid
                WHERE d.did = ? AND sp.userid = ?
            )
        """;
        int userId = getCurrentUserId();
        jdbcTemplate.update(sql, message.role(), message.authorid(), message.message(), message.msgid(), message.discussionId(), userId);
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
                FROM core.sharedproject sp
                JOIN core.discussion d ON sp.projectid = d.projectid
                WHERE d.did = core.message.did AND sp.userid = ?
            )
        """;
        jdbcTemplate.update(sql, msgid, getCurrentUserId());
    }
}
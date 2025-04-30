package com.unbumpkin.codechat.repository.openai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

import com.unbumpkin.codechat.dto.request.AddOaiThreadRequest;
import com.unbumpkin.codechat.model.openai.OaiThread;
import com.unbumpkin.codechat.security.CustomAuthentication;
import com.unbumpkin.codechat.service.openai.CCProjectFileManager.Types;

@Repository
public class OaiThreadRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<OaiThread> rowMapper = (rs, rowNum) -> new OaiThread(
        rs.getInt("threadid"),
        rs.getString("oai_threadid"),
        rs.getObject("vsid", Integer.class),
        rs.getInt("did"),
        rs.getString("type")
    );

    /**
     * Add a thread to the database.
     * @param request The thread to add.
     */
    public void addThread(AddOaiThreadRequest request) {
        try {
            String sql = """
                INSERT INTO core.thread ( oai_threadid, vsid, did, type)
                VALUES (?, ?, ?, ?)
                """;
            System.out.println("request.oaiThreadId(): " + request.oaiThreadId());
            jdbcTemplate.update(sql, request.oaiThreadId(),  request.vsid(), request.did(), request.type());
        } catch (Exception e) {
            System.out.println("exception in addThread: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieve a thread by ID.
     * @param threadid The thread ID.
     * @return The thread.
     */
    public OaiThread getThreadById(int threadid) {
        String sql = """
            SELECT t.*
            FROM core.thread t
            WHERE t.threadid = ?
            """;
        return jdbcTemplate.queryForObject(sql, rowMapper, threadid);
    }

    /**
     * Retrieve all threads by discussion ID.
     * @param discussionId The discussion ID.
     * @return A list of threads.
     */
    public Map<Types,OaiThread> getAllThreadsByDiscussionId(int discussionId) {
        String sql = """
            SELECT t.*
            FROM core.thread t
            WHERE t.did = ?
            """;
        List<OaiThread> threadList= jdbcTemplate.query(sql, rowMapper, discussionId);
        Map<Types,OaiThread> threadMap = new HashMap<>();
        for (OaiThread thread : threadList) {
            threadMap.put(Types.valueOf(thread.type()), thread);
        }
        return threadMap;
    }

    /**
     * Update a thread.
     * @param thread The thread to update.
     */
    public void updateThread(OaiThread thread) {
        String sql = """
            UPDATE core.thread
            SET oai_threadid = ?, vsid = ?, did = ?, type = ?
            WHERE threadid = ?
        """;
        jdbcTemplate.update(sql, thread.oaiThreadId(), thread.vsid(), thread.did(), thread.type(), thread.threadid());
    }

    /**
     * Delete a thread.
     * @param threadid The thread ID.
     */
    public void deleteThread(int threadid) {
        String sql = """
            DELETE FROM core.thread
            WHERE threadid = ?
        """;
        jdbcTemplate.update(sql, threadid);
    }
    

    private CustomAuthentication getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CustomAuthentication) {
            return ((CustomAuthentication) authentication);
        }
        throw new IllegalStateException("No authenticated user found");
    }

    /**
     * Delete all threads.
     */
    public void deleteAll() {
        CustomAuthentication currentUser = getCurrentUser();
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new IllegalStateException("Only admins can delete all messages");
        }
        // Delete all records in the thread table
        String deleteThreadsSql = "DELETE FROM core.thread";
        jdbcTemplate.update(deleteThreadsSql);
    }
}
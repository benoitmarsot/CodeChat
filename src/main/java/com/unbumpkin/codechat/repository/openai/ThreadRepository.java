package com.unbumpkin.codechat.repository.openai;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import com.unbumpkin.codechat.domain.openai.Thread;
import com.unbumpkin.codechat.security.CustomAuthentication;

@Repository
public class ThreadRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Thread> rowMapper = (rs, rowNum) -> new Thread(
        rs.getInt("threadid"),
        rs.getString("oai_threadid"),
        rs.getObject("vsid", Integer.class),
        rs.getInt("discussionId"),
        rs.getString("type")
    );

    /**
     * Add a thread to the database.
     * @param thread The thread to add.
     */
    public void addThread(Thread thread) {
        String sql = """
            INSERT INTO core.thread (threadid, oai_threadid, vsid, did, type)
            VALUES (?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(sql, thread.threadid(), thread.oaiThreadId(), thread.vsid(), thread.discussionId(), thread.type());
    }

    /**
     * Retrieve a thread by ID.
     * @param threadid The thread ID.
     * @return The thread.
     */
    public Thread getThreadById(int threadid) {
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
    public List<Thread> getAllThreadsByDiscussionId(int discussionId) {
        String sql = """
            SELECT t.*
            FROM core.thread t
            WHERE t.did = ?
        """;
        return jdbcTemplate.query(sql, rowMapper, discussionId);
    }

    /**
     * Update a thread.
     * @param thread The thread to update.
     */
    public void updateThread(Thread thread) {
        String sql = """
            UPDATE core.thread
            SET oai_threadid = ?, vsid = ?, did = ?, type = ?
            WHERE threadid = ?
        """;
        jdbcTemplate.update(sql, thread.oaiThreadId(), thread.vsid(), thread.discussionId(), thread.type(), thread.threadid());
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
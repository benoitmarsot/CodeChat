package com.unbumpkin.codechat.repository.openai;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbumpkin.codechat.domain.openai.OaiFile;
import com.unbumpkin.codechat.domain.openai.OaiFile.Purposes;
import com.unbumpkin.codechat.security.CustomAuthentication;


/**
 * Repository for OaiFile objects.
 */
@Repository
public class OaiFileRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private int getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CustomAuthentication) {
            return ((CustomAuthentication) authentication).getUserId();
        }
        throw new IllegalStateException("No authenticated user found");
    }
    
    /**
     * Uploads a file to the database.
     * @param file: The file to be uploaded.
     * @throws JsonProcessingException
     */
    public void storeOaiFile(OaiFile file) throws JsonProcessingException, DataAccessException {
        ObjectMapper mapper=new ObjectMapper();
        String json=mapper.writeValueAsString(file);

        String sql = "call core.createoaifile(?::json,?)";
        jdbcTemplate.update(sql, json, getCurrentUserId());
    }

    /**
     * Saves the uploaded files to the database.
     * @param files A map of file names to OaiFile objects to be uploaded.
     * @return The number of files uploaded.
     * @throws DataAccessException
     */
    @Transactional
    public long storeOaiFiles(Collection<OaiFile> files) throws DataAccessException {
        if (files == null || files.isEmpty()) {
            return 0;
        }
        String csvData = getFilesCSV(files);
        
        Long result= jdbcTemplate.execute((ConnectionCallback<Long>) connection -> {
            try {
                CopyManager copyManager = new CopyManager(connection.unwrap(BaseConnection.class));
                String sql = "COPY core.oaifile (userid, oai_f_id, file_name, rootdir, filepath, purpose, linecount) "
                          + "FROM STDIN WITH (FORMAT csv, HEADER true, NULL 'null')";
                try (InputStream inputStream = new ByteArrayInputStream(csvData.getBytes())) {
                    return copyManager.copyIn(sql, inputStream);
                }
            } catch (IOException | SQLException e) {
                throw new DataAccessException("Failed to copy data: " + e.getMessage(), e) {};
            }
        });
        return result==null?0:result;
    }

    /**
     * Delete files from the database.
     * @param fileIds: The list of file ids to be deleted.
     */
    public void deleteFiles(List<String> fileIds) {
        String sql = "DELETE FROM core.oaifile WHERE oai_f_id = ANY(?) and userid = ?";
        jdbcTemplate.update(sql, (Object) fileIds.toArray(new String[0]), getCurrentUserId());
    }
    /**
     * Check if a file exists in the database.
     * @param fileId: The Oai file id to be checked.
     * @return
     */
    public boolean fileExists(String fileId) {
        String sql = "SELECT COUNT(*) FROM core.oaifile WHERE oai_f_id = ? and userid = ?";
        return Objects.requireNonNullElse(
            jdbcTemplate.queryForObject(sql, Integer.class, fileId, getCurrentUserId()),0
         ) > 0;
    }
    /**
     * Count the number of files in the database.
     * @return the number of files in the database.
     */
    public int countFiles() {
        String sql = "SELECT COUNT(*) FROM core.oaifile WHERE userid = ?";
        return Objects.requireNonNullElse(jdbcTemplate.queryForObject(sql, Integer.class, getCurrentUserId()), 0);
    }
    /**
     * delete a file from the database.
     * @param fileId: The file id to be deleted.
     */
    public void deleteFile(String fileId) {
        String sql = "DELETE FROM core.oaifile WHERE oai_f_id = ? and userid = ?";
        jdbcTemplate.update(sql, fileId, getCurrentUserId());
    }

    /**
     * Delete all files from the database.
     */
    public List<String> deleteAllFiles() {
        int userId = getCurrentUserId();
        List<String> fileIds = jdbcTemplate.queryForList
            ("SELECT oai_f_id FROM core.oaifile where userid = ?", String.class, 
            userId
        );
        String sql = "DELETE FROM core.oaifile where userid=?";
        jdbcTemplate.update(sql,userId);
        return fileIds;

    }

    /**
     * return a list of OaiFiles corresponding to the List of fileids.
     * @param fileIds: The list of file ids to be retrieved.
     * @return a list of OaiFiles.
     */
    public List<OaiFile> retrieveFiles(List<String> fileIds) {
        String sql = "SELECT * FROM core.oaifile WHERE oai_f_id = ANY(?) and userid = ?";
        return jdbcTemplate.query(
            sql, 
            ps -> {
                ps.setArray(1, ps.getConnection().createArrayOf("text", fileIds.toArray()));
                ps.setInt(2, getCurrentUserId());
            },
            (rs, rowNum) -> OaiFileFrom(rs)
        );
    }

    /**
     * return a list of OaiFiles corresponding to the root directory.
     * @param rootDir: The root directory to be retrieved.
     * @return a list of OaiFiles.
     */
    public List<OaiFile> retrieveFiles(String rootDir) {
        String sql = "SELECT * FROM core.oaifile where rootdir = ? and userid = ?";
        return jdbcTemplate.query(sql, ps -> {
            ps.setString(1, rootDir);
            ps.setInt(2, getCurrentUserId());
        }, (rs, rowNum) -> {
            return OaiFileFrom(rs);
        });
    }


    /**
     * return the list of all OaiFiles in the database.
     * @return a list of OaiFiles.
     */
    public List<OaiFile> listAllFiles() {
        
        String sql = "SELECT * FROM core.oaifile where userid = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            return OaiFileFrom(rs);
        }, getCurrentUserId());
    }

    /**
     * get the OaiFile corresponding to the fileid.
     * @param fileId: The file id to be retrieved.
     * @return the OaiFile corresponding to the fileid.
     */
    public OaiFile retrieveFile(String fileId) {
        String sql = "SELECT * FROM core.oaifile WHERE oai_f_id = ? and userid = ?";
        return jdbcTemplate.queryForObject(
            sql, 
            (rs, rowNum) -> OaiFileFrom(rs),
            fileId, getCurrentUserId()
        );
    }
    private OaiFile OaiFileFrom(ResultSet rs) throws SQLException {
        return new OaiFile(
            rs.getInt("fid"),
            rs.getInt("userid"),
            rs.getString("oai_f_id"),
            rs.getString("file_name"),
            rs.getString("rootdir"),
            rs.getString("filepath"),
            Purposes.valueOf(rs.getString("purpose").toLowerCase()),
            rs.getInt("linecount")
        );
    }
    /**
     * Converts the files map to a CSV format string with double quotes around each field.
     * @param files A map of file names to OaiFile objects.
     * @return A CSV format string.
     */
    private String getFilesCSV(Collection<OaiFile> files) {
        int userId = getCurrentUserId();
        StringBuilder sb = new StringBuilder();
        sb.append("userid,fileid,filename,rootdir,filepath,purpose,linecount\n");
        for (OaiFile file : files) {
            sb.append(String.valueOf(userId)).append(",");
            sb.append(quoteAndEscape(file.fileId())).append(",");
            sb.append(quoteAndEscape(file.fileName())).append(",");
            sb.append(quoteAndEscape(file.rootdir())).append(",");
            sb.append(quoteAndEscape(file.filePath())).append(",");
            sb.append(quoteAndEscape(file.purpose().toString())).append(",");
            sb.append(file.linecount()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Quotes the input string with double quotes and escapes any double quotes in the input.
     * @param input The input string.
     * @return The quoted and escaped string.
     */
    private static String quoteAndEscape(String input) {
        if (input == null) {
            return "null";
        }
        return "\"" + input.replace("\"", "\\\"") + "\"";
    }

    
}

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
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbumpkin.codechat.model.openai.OaiFile;
import com.unbumpkin.codechat.model.openai.OaiFile.Purposes;
import com.unbumpkin.codechat.security.CurrentUserProvider;

/**
 * Repository for OaiFile objects.
 */
@Repository
public class OaiFileRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CurrentUserProvider currentUserProvider;

    /**
     * Uploads a file to the database.
     * @param file: The file to be uploaded.
     * @param projectId: The project ID to which the file belongs.
     * @throws JsonProcessingException
     */
    public void storeOaiFile(OaiFile file, int prId) throws JsonProcessingException, DataAccessException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(file);

        String sql = "call core.createoaifile(?::json,?)";
        jdbcTemplate.update(sql, json, prId);
    }
    public List<OaiFile> getOaiFileByPath(String path, int projectId) {
        String sql = "SELECT o.* FROM core.oaifile o " +
                     "JOIN core.projectresource pr ON o.prid = pr.prid " +
                     "WHERE o.filepath = ? AND pr.projectid = ?";
        try {
            return jdbcTemplate.query(
                sql, 
                (rs, rowNum) -> OaiFileFrom(rs), 
                path, 
                projectId
            );
        } catch (org.springframework.dao.DataAccessException e) {
            return List.of(); // Return empty list instead of null
        }
    }

    /**
     * Saves the uploaded files to the database.
     * @param files A collection of OaiFile objects to be uploaded.
     * @param projectId The project ID to which the files belong.
     * @return The number of files uploaded.
     * @throws DataAccessException
     */
    @Transactional
    public long storeOaiFiles(Collection<OaiFile> files, int prId) throws DataAccessException {
        if (files == null || files.isEmpty()) {
            return 0;
        }
        String csvData = getFilesCSV(files, prId);

        Long result = jdbcTemplate.execute((ConnectionCallback<Long>) connection -> {
            try {
                CopyManager copyManager = new CopyManager(connection.unwrap(BaseConnection.class));
                String sql = "COPY core.oaifile (prid, oai_f_id, file_name, rootdir, filepath, purpose, linecount) "
                           + "FROM STDIN WITH (FORMAT csv, HEADER true, NULL 'null')";
                try (InputStream inputStream = new ByteArrayInputStream(csvData.getBytes())) {
                    return copyManager.copyIn(sql, inputStream);
                }
            } catch (IOException | SQLException e) {
                throw new DataAccessException("Failed to copy data: " + e.getMessage(), e) {};
            }
        });
        return result == null ? 0 : result;
    }

    /**
     * Delete files from the database.
     * @param fileIds The list of file IDs to be deleted.
     */
    public void deleteFiles(List<String> fileIds) {
        String sql = "DELETE FROM core.oaifile WHERE oai_f_id = ANY(?)";
        jdbcTemplate.update(sql, (Object) fileIds.toArray(new String[0]));
    }

    /**
     * Check if a file exists in the database.
     * @param fileId The Oai file ID to be checked.
     * @return True if the file exists, false otherwise.
     */
    public boolean fileExists(String fileId) {
        String sql = "SELECT COUNT(*) FROM core.oaifile WHERE oai_f_id = ?";
        return Objects.requireNonNullElse(
            jdbcTemplate.queryForObject(sql, Integer.class, fileId), 0
        ) > 0;
    }

    /**
     * Count the number of files in the database.
     * @return The number of files in the database.
     */
    public int countFiles() {
        String sql = "SELECT COUNT(*) FROM core.oaifile";
        return Objects.requireNonNullElse(jdbcTemplate.queryForObject(sql, Integer.class), 0);
    }

    /**
     * Delete a file from the database.
     * @param fileId The file ID to be deleted.
     */
    public void deleteFile(String fileId) {
        String sql = "DELETE FROM core.oaifile WHERE oai_f_id = ?";
        jdbcTemplate.update(sql, fileId);
    }

    /**
     * Delete all files from the database.
     * @return A list of deleted file IDs.
     */
    public List<String> deleteAll() {
        if (!currentUserProvider.getCurrentUser().isAdmin()) {
            throw new IllegalStateException("Only admins can delete all messages");
        }

        List<String> fileIds = jdbcTemplate.queryForList(
            "SELECT oai_f_id FROM core.oaifile", String.class
        );
        String sql = "DELETE FROM core.oaifile";
        jdbcTemplate.update(sql);
        return fileIds;
    }

    /**
     * Return a list of OaiFiles corresponding to the list of file IDs.
     * @param fileIds The list of file IDs to be retrieved.
     * @return A list of OaiFiles.
     */
    public List<OaiFile> retrieveFiles(String[] fileIds) {
        String sql = "SELECT * FROM core.oaifile WHERE oai_f_id = ANY(?)";
        return jdbcTemplate.query(
            sql,
            ps -> ps.setArray(1, ps.getConnection().createArrayOf("text", fileIds)),
            (rs, rowNum) -> OaiFileFrom(rs)
        );
    }

    /**
     * Return a list of OaiFiles corresponding to the root directory.
     * @param rootDir The root directory to be retrieved.
     * @param projectId The project ID to which the files belong.
     * @return A list of OaiFiles.
     */
    public List<OaiFile> retrieveFiles(String rootDir, int projectId) {
        String sql = "SELECT o.* FROM core.oaifile o " +
            "JOIN core.projectresource pr ON o.prid = pr.prid " +
            "WHERE o.rootdir = ? AND pr.projectid = ?";
        return jdbcTemplate.query(sql, ps -> {
            ps.setString(1, rootDir);
            ps.setInt(2, projectId);
        }, (rs, rowNum) -> OaiFileFrom(rs));
    }

    /**
     * Return the list of all OaiFiles in the database.
     * @param projectId The project ID to which the files belong.
     * @return A list of OaiFiles.
     */
    public List<OaiFile> listAllFiles(int projectId) {
        String sql = "SELECT o.* FROM core.oaifile o " +
            "JOIN core.projectresource pr ON o.prid = pr.prid " +
            "WHERE pr.projectid = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> OaiFileFrom(rs), projectId);
    }

    /**
     * Get the OaiFile corresponding to the file ID.
     * @param fileId The file ID to be retrieved.
     * @return The OaiFile corresponding to the file ID.
     */
    public OaiFile retrieveFile(String fileId) {
        String sql = "SELECT * FROM core.oaifile WHERE oai_f_id = ?";
        return jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> OaiFileFrom(rs),
            fileId
        );
    }

    private OaiFile OaiFileFrom(ResultSet rs) throws SQLException {
        return new OaiFile(
            rs.getInt("fid"),
            rs.getInt("prid"),
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
     * @param files A collection of OaiFile objects.
     * @param projectId The project ID to which the files belong.
     * @return A CSV format string.
     */
    private String getFilesCSV(Collection<OaiFile> files, int prId) {
        StringBuilder sb = new StringBuilder();
        sb.append("prid,fileid,filename,rootdir,filepath,purpose,linecount\n");
        for (OaiFile file : files) {
            sb.append(prId).append(",");
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
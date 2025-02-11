package com.unbumpkin.codechat.repository.openai;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbumpkin.codechat.domain.openai.OaiFile;
import com.unbumpkin.codechat.domain.openai.OaiFile.Purposes;

@Repository
public class OaiFileRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Uploads a file to the database.
     * @param file: The file to be uploaded.
     * @throws JsonProcessingException
     */
    public void storeOaiFile(OaiFile file) throws JsonProcessingException, DataAccessException {
        ObjectMapper mapper=new ObjectMapper();
        String json=mapper.writeValueAsString(file);

        String sql = "call core.createoaifile(?::json)";
        jdbcTemplate.update(sql, json);
    }

    /**
     * Saves the uploaded files to the database.
     * @param files A map of file names to OaiFile objects to be uploaded.
     * @return The number of files uploaded.
     * @throws SQLException If there is an error with the SQL operation.
     * @throws IOException  If there is an error with the file operation.
     */
    public int storeOaiFiles(Map<String, OaiFile> files) throws SQLException, IOException {
        // Convert the files map to CSV format
        String csvData = getFilesCSV(files);

        // Get the connection from JdbcTemplate
        try (Connection con = jdbcTemplate.getDataSource().getConnection()) {
            String sql = "COPY core.oaifile (fileid,filename,rootdir,filepath,purpose) FROM STDIN WITH (FORMAT csv, HEADER true, NULL 'null')";
            CopyManager copyManager = new CopyManager(con.unwrap(BaseConnection.class));
            InputStream inputStream = new ByteArrayInputStream(csvData.getBytes());
            copyManager.copyIn(sql, inputStream);
        }

        return files.size();
    }
    /**
     * Delete files from the database.
     * @param fileIds: The list of file ids to be deleted.
     */
    public void deleteFiles(List<String> fileIds) {
        String sql = "DELETE FROM core.oaifile WHERE fileid = ANY(?)";
        jdbcTemplate.update(sql, (Object) fileIds.toArray(new String[0]));
    }

    /**
     * delete a file from the database.
     * @param fileId: The file id to be deleted.
     */
    public void deleteFile(String fileId) {
        String sql = "DELETE FROM core.oaifile WHERE fileid = ?";
        jdbcTemplate.update(sql, fileId);
    }

    /**
     * Delete all files from the database.
     */
    public void deleteAllFiles() {
        String sql = "DELETE FROM core.oaifile";
        jdbcTemplate.update(sql);
    }

    /**
     * return a list of OaiFiles corresponding to the List of fileids.
     * @param fileIds: The list of file ids to be retrieved.
     * @return a list of OaiFiles.
     */
    public List<OaiFile> retrieveFiles(List<String> fileIds) {
        String sql = "SELECT * FROM core.oaifile WHERE fileid = ANY(?)";
        return jdbcTemplate.query(sql, new Object[]{fileIds.toArray(new String[0])}, (rs, rowNum) -> {
            return new OaiFile(
                rs.getString("fileid"),
                rs.getString("filename"),
                rs.getString("rootdir"),
                rs.getString("filepath"),
                Purposes.valueOf(rs.getString("purpose").toLowerCase())
            );
        });
    }

    /**
     * return a list of OaiFiles corresponding to the root directory.
     * @param rootDir: The root directory to be retrieved.
     * @return a list of OaiFiles.
     */
    public List<OaiFile> retrieveFiles(String rootDir) {
        String sql = "SELECT * FROM core.oaifile where rootdir = ?";
        return jdbcTemplate.query(sql, new Object[]{rootDir}, (rs, rowNum) -> {
            return new OaiFile(
                rs.getString("fileid"),
                rs.getString("filename"),
                rs.getString("rootdir"),
                rs.getString("filepath"),
                Purposes.valueOf(rs.getString("purpose").toLowerCase())
            );
        });
    }

    /**
     * return the list of all OaiFiles in the database.
     * @return a list of OaiFiles.
     */
    public List<OaiFile> listAllFiles() {
        String sql = "SELECT * FROM core.oaifile";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            return new OaiFile(
                rs.getString("fileid"),
                rs.getString("filename"),
                rs.getString("rootdir"),
                rs.getString("filepath"),
                OaiFile.Purposes.valueOf(rs.getString("purpose"))
            );
        });
    }

    /**
     * get the OaiFile corresponding to the fileid.
     * @param fileId: The file id to be retrieved.
     * @return the OaiFile corresponding to the fileid.
     */
    public OaiFile retrieveFile(String fileId) {
        String sql = "SELECT * FROM core.oaifile WHERE fileid = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{fileId}, (rs, rowNum) -> {
            return new OaiFile(
                rs.getString("fileid"),
                rs.getString("filename"),
                rs.getString("rootdir"),
                rs.getString("filepath"),
                OaiFile.Purposes.valueOf(rs.getString("purpose"))
            );
        });
    }
    /**
     * Converts the files map to a CSV format string with double quotes around each field.
     * @param files A map of file names to OaiFile objects.
     * @return A CSV format string.
     */
    private String getFilesCSV(Map<String, OaiFile> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("fileid,filename,rootdir,filepath,purpose\n");
        for (OaiFile file : files.values()) {
            sb.append(quoteAndEscape(file.fileId())).append(",");
            sb.append(quoteAndEscape(file.fileName())).append(",");
            sb.append(quoteAndEscape(file.rootdir())).append(",");
            sb.append(quoteAndEscape(file.filePath())).append(",");
            sb.append(quoteAndEscape(file.purpose().toString())).append("\n");
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

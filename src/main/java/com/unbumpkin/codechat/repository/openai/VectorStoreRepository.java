package com.unbumpkin.codechat.repository.openai;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbumpkin.codechat.model.openai.VectorStore;
import com.unbumpkin.codechat.security.CustomAuthentication;
import com.unbumpkin.codechat.service.openai.ProjectFileCategorizer.Types;

@Repository
public class VectorStoreRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired 
    ObjectMapper objectMapper;

    public record RepoVectorStoreResponse( 
        int id,
        String vsid,
        String name,
        String description,
        Instant created,
        long dayskeep,
        Types type
    ) { }

    private CustomAuthentication getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CustomAuthentication) {
            return ((CustomAuthentication) authentication);
        }
        throw new IllegalStateException("No authenticated user found");
    }


    private final RowMapper<RepoVectorStoreResponse> rowMapper = (rs, rowNum) -> {
        RepoVectorStoreResponse vectorStore = new RepoVectorStoreResponse(
            rs.getInt("vsid"),
            rs.getString("oai_vs_id"),
            rs.getString("vs_name"),
            rs.getString("vs_desc"),
            rs.getTimestamp("created").toInstant(),
            rs.getInt("dayskeep"),
            Types.valueOf(rs.getString("type"))
        );
        return vectorStore;
    };


    /**
     * Uploads a VectorStore to the database.
     * @param vStore: The VectorStore to be uploaded.
     * @throws JsonProcessingException
     * @throws SQLException 
     */
    public int storeVectorStore(VectorStore vStore) throws DataAccessException, JsonProcessingException {
        String sql = "INSERT INTO core.vectorstore (oai_vs_id, vs_name, vs_desc, dayskeep, type) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[] { "vsid" });
            ps.setString(1, vStore.getOaiVsid());
            ps.setString(2, vStore.getVsname());
            ps.setString(3, vStore.getVsdesc());
            if(vStore.getDayskeep() == null) {
                ps.setNull(4, java.sql.Types.INTEGER);
            } else {
                ps.setInt(4, vStore.getDayskeep());
            }
            ps.setString(5, vStore.getType().name());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key==null?0:key.intValue();
    }

    /**
     * Retrieves a VectorStore from the database by its ID.
     * @param vsid: The ID of the VectorStore to retrieve.
     * @return The VectorStore with the specified ID.
     */
    public RepoVectorStoreResponse getVectorStoreByOaiId(String oaiVsId) {
        String sql = "SELECT * FROM core.vectorstore WHERE oai_vs_id = ?";
        return jdbcTemplate.queryForObject(sql, rowMapper, oaiVsId);
    }

    /**
     * Retrieves all VectorStores from the database.
     * @return A list of all VectorStores.
     */
    public List<RepoVectorStoreResponse> getAllVectorStores() {
        String sql = "SELECT * FROM core.vectorstore";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Updates a VectorStore in the database.
     * @param vStore: The VectorStore to update.
     * @throws JsonProcessingException
     */
    public void updateVectorStore(VectorStore vStore) throws DataAccessException, JsonProcessingException {
        String sql = "UPDATE core.vectorstore SET vs_name = ?, vs_desc = ?, dayskeep = ? WHERE oai_vs_id = ?";
        jdbcTemplate.update(sql, vStore.getVsname(), vStore.getVsdesc(), vStore.getDayskeep(), vStore.getOaiVsid());
    }

    /**
     * Deletes a VectorStore from the database by its ID.
     * @param vsid: The ID of the VectorStore to delete.
     */
    public void deleteVectorStore(String oaiVsId) {
        String sql = "DELETE FROM core.vectorstore WHERE oai_vs_id = ?";
        jdbcTemplate.update(sql, oaiVsId);
    }

    /**
     * Deletes all VectorStores from the database.
     */
    public void deleteAllVectorStores() {
        String sql = "DELETE FROM core.vectorstore";
        jdbcTemplate.update(sql);
    }

    /**
     * Adds an association between a VectorStore and a single OaiFile.
     * @param vsid: The VectorStore ID.
     * @param fileid: The associated File ID.
     */
    public void addFile(String vsOaiId, String fOaiId) {
        String sql = "INSERT INTO core.vectorstore_oaifile (vsid, fid) " +
                        "SELECT v.vsid, f.fid " +
                        "FROM core.vectorstore v " +
                            "JOIN core.oaifile f ON f.oai_f_id = ? " +
                        "WHERE v.oai_vs_id = ?";
        jdbcTemplate.update(sql, fOaiId, vsOaiId);
    }

    /**
     * Adds associations between a VectorStore and multiple OaiFiles.
     * @param vsid: The VectorStore ID.
     * @param fileids: A list of file IDs to associate.
     */
    public void addFiles(int vsid, List<Integer> fileids) {
        String sql = "INSERT INTO core.vectorstore_oaifile (vsid, fid) VALUES (?, ?)";
        fileids.forEach(fileid -> jdbcTemplate.update(sql, vsid, fileid));
    }
    /**
     * Adds associations between a VectorStore and multiple OaiFiles.
     * @param vsOaiId: The VectorStore open ai ID.
     * @param fileOaiIds: A list of OaiFile IDs to associate.
     */
    public void addFiles(String vsOaiId, Collection<String> fileOaids) {
        String sql = "INSERT INTO core.vectorstore_oaifile (vsid, fid)" +
                        "SELECT v.vsid, f.fid " +
                        "FROM core.vectorstore v " +
                            "JOIN core.oaifile f ON f.oai_f_id = ? " +
                        "WHERE v.oai_vs_id = ?";
        fileOaids.forEach(fileid -> jdbcTemplate.update(sql,fileid, vsOaiId));
    }

    /**
     * Removes an association between a VectorStore and an OaiFile.
     * @param vsid: The VectorStore ID.
     * @param fileid: The OaiFile ID to disassociate.
     */
    public void removeFile(int vsid, int fileid) {
        String sql = "DELETE FROM core.vectorstore_oaifile WHERE vsid = ? AND fid = ?";
        jdbcTemplate.update(sql, vsid, fileid);
    }
    /**
     * Removes an association between a VectorStore and an OaiFile using OpenAI IDs.
     * @param vsOaiId: The VectorStore OpenAI ID
     * @param fileOaiId: The OaiFile OpenAI ID to disassociate
     */
    public void removeFile(String vsOaiId, String fileOaiId) {
        String sql = "DELETE FROM core.vectorstore_oaifile " +
                    "WHERE vsid = (SELECT vsid FROM core.vectorstore WHERE oai_vs_id = ?) " +
                    "AND fid = (SELECT fid FROM core.oaifile WHERE oai_f_id = ?)";
        jdbcTemplate.update(sql, vsOaiId, fileOaiId);
    }

    /**
     * Checks if a VectorStore exists in the database by its ID.
     * @param vsid: The ID of the VectorStore to check.
     * @return true if the VectorStore exists, false otherwise.
     */
    public boolean vectorStoreExists(String oaiVsId) {
        String sql = "SELECT COUNT(*) FROM core.vectorstore WHERE oai_vs_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, oaiVsId);
        return count != null && count > 0;
    }
    /**
     * Checks if a VectorStore contains an association with an OaiFile.
     * @param vsid: The VectorStore ID.
     * @param fileid: The OaiFile ID.
     * @return tru if the file is associated with the VectorStore, false otherwise.
     */
    public boolean vectorContainFile(int vsid, String oaiFileId) {
        String sql = "SELECT COUNT(*) FROM core.vectorstore_oaifile vf "
                        +"inner join core.oaifile f on f.fid = vf.fid "
                    +"WHERE vf.vsid = ? AND f.oai_f_id = ?";
    
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, vsid, oaiFileId);
        return count != null && count > 0;
    }
    /**
     * Checks if a VectorStore contains an association with an OaiFile.
     * @param vsOaiId: The VectorStore openai ID.
     * @param fileid: The OaiFile ID.
     * @return tru if the file is associated with the VectorStore, false otherwise.
     */
    public boolean vectorContainFile(String vsOaiId, String oaiFileId) {
        String sql = "SELECT COUNT(*) FROM core.vectorstore_oaifile vf "
                    +"	inner join core.oaifile f on f.fid = vf.fid "
                    +"	inner join core.vectorstore v on v.vsid = vf.vsid "
                    +"WHERE v.oai_vs_id = ? AND f.oai_f_id = ?";

    
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, vsOaiId, oaiFileId);
        return count != null && count > 0;
    }

    public List<String> vectorContainAny(int vsid, List<String> fileids) {
        String sql = "SELECT f.oai_f_id FROM core.vectorstore_oaifile vf "
                        +"inner join core.oaifile f on f.fid = vf.fid "
                    +"WHERE vf.vsid = ? AND f.oai_f_id = ANY(?)";

        List<String> result = jdbcTemplate.queryForList(
            sql, String.class, 
            vsid, 
            fileids.toArray(new String[0])
        );
        return result;
    }
    public List<String> vectorContainAny(String vsOaiId, List<String> oaiFileIds) {
        String sql = "SELECT f.oai_f_id FROM core.vectorstore_oaifile vf "
                        +"inner join core.oaifile f on f.fid = vf.fid "
                        +"inner join core.vectorstore v on v.vsid = vf.vsid "
                    +"WHERE v.oai_vs_id = ? AND f.oai_f_id = ANY(?)";

        List<String> result = jdbcTemplate.queryForList(
            sql, String.class, 
            vsOaiId, 
            oaiFileIds.toArray(new String[0])
        );
        return result;
    }
    public List<String> findVectorStoreFiles(int vsid) {
        String sql = "SELECT f.oai_f_id FROM core.vectorstore_oaifile vf "
                        +"inner join core.oaifile f on f.fid = vf.fid "
                    +"WHERE vf.vsid = ?";
        return jdbcTemplate.queryForList(sql, String.class, vsid);
    }
    public List<String> findVectorStoreFiles(String vsOaiId) {
        String sql = "SELECT f.oai_f_id FROM core.vectorstore_oaifile vf "
                        + "INNER JOIN core.oaifile f ON f.fid = vf.fid "
                        + "INNER JOIN core.vectorstore v ON v.vsid = vf.vsid "
                    + "WHERE v.oai_vs_id = ?";
        return jdbcTemplate.queryForList(sql, String.class, vsOaiId);
    }
    public void deleteAll() {
        CustomAuthentication user = getCurrentUser();
        if (user == null || !user.isAdmin()) {
            throw new IllegalStateException("Only admins can delete all messages");
        }

        // Delete all associations in the vectorstore_oaifile table
        String deleteAssociationsSql = "DELETE FROM core.vectorstore_oaifile";
        jdbcTemplate.update(deleteAssociationsSql);

        // Delete all records in the vectorstore table
        String deleteVectorStoresSql = "DELETE FROM core.vectorstore";
        jdbcTemplate.update(deleteVectorStoresSql);
    }
}
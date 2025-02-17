package com.unbumpkin.codechat.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbumpkin.codechat.domain.User;
import com.unbumpkin.codechat.repository.UserRepository;

@Repository
public class UserRepository  {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;
    public UserRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public User create(User user) {
        String jsonUser;
        try {
            jsonUser = objectMapper.writeValueAsString(user);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting user to JSON", e);
        }
    
        String sql = "call core.createuser(?::json)";
        List<Integer> userIds = jdbcTemplate.query(
            sql,
            userIdRowMapper,
            jsonUser
        );
        return new User(userIds.get(0), user.name(), user.email(), user.password(), user.role());
    }
    
    public User findById(int id) {
        String sql = "SELECT * FROM core.user WHERE userid = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, id);
        return users.isEmpty() ? null : users.get(0);
    }
    
    public User findByEmail(String email) {
        String sql = "SELECT * FROM core.user WHERE email = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, email);
        return users.isEmpty() ? null : users.get(0);
    }

    public void deleteById(int id) {
        String sql = "DELETE FROM core.user WHERE userid = ?";
        jdbcTemplate.update(sql, id);
    }

    public void deleteByEmail(String email) {
        String sql = "DELETE FROM core.user WHERE email = ?";
        jdbcTemplate.update(sql, email);
    }

    public void deleteAll() {
        String sql = "DELETE FROM core.user";
        jdbcTemplate.update(sql);
    }

    public boolean existsById(int id) {
        String sql = "SELECT COUNT(1) FROM core.user WHERE userid = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(1) FROM core.user WHERE email = ?";
        // Use queryForObject with SqlParameterValue or simply pass the parameter
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    public Iterable<User> findAll() {
        String sql = "SELECT * FROM core.user";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    public int count() {
        String sql = "SELECT COUNT(1) FROM core.user";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Find the first user id in the database
     * Usefull for integration testing
     * @return the first user id in the database or 0 if no user is found
     */
    public int findFirstUserId() {
        String sql = "SELECT userid as out_userid FROM core.user LIMIT 1";
        Integer id= jdbcTemplate.queryForObject(sql, userIdRowMapper);
        return id != null ? id : 0;
    }
    private final RowMapper<Integer> userIdRowMapper = new RowMapper<Integer>() {
        @Override
        public Integer mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            return rs.getInt("out_userid");
        }
    };

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> new User(
        rs.getInt("userid"),
        rs.getString("name"),
        rs.getString("email"),
        rs.getString("password"),
        User.Role.valueOf(rs.getString("role").toUpperCase())
    );

}
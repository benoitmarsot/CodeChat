package com.unbumpkin.codechat.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbumpkin.codechat.domain.User;
import com.unbumpkin.codechat.repository.UserRepository;

@Repository
public class UserRepository  {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Optional<User> create(User user) {
        ObjectMapper mapper = new ObjectMapper();
        String jsonUser;
        try {
            jsonUser = mapper.writeValueAsString(user);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting user to JSON", e);
        }

        String sql = "call core.createuser(?::json)";
        List<Integer> userIds = jdbcTemplate.query(
            sql,
            new Object[]{jsonUser},
            userIdRowMapper
        );
        return userIds.isEmpty() ? 
            Optional.empty() : 
            Optional.of(new User(userIds.get(0), user.name(), user.email(), user.password(), user.role()));
    }

    public Optional<User> findById(int id) {
        String sql = "SELECT * FROM core.user WHERE id = ?";
        List<User> users = jdbcTemplate.query(sql, new Object[]{id}, userRowMapper);
        return users.stream().findFirst();
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM core.user WHERE email = ?";
        List<User> users = jdbcTemplate.query(sql, new Object[]{email}, userRowMapper);
        return users.stream().findFirst();
    }

    public void deleteById(int id) {
        String sql = "DELETE FROM core.user WHERE id = ?";
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
        String sql = "SELECT COUNT(1) FROM core.user WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, new Object[]{id}, Integer.class);
        return count != null && count > 0;
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(1) FROM core.user WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, new Object[]{email}, Integer.class);
        return count != null && count > 0;
    }

    public Iterable<User> findAll() {
        String sql = "SELECT * FROM core.user";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    public int count() {
        String sql = "SELECT COUNT(1) FROM core.user";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
    private final RowMapper<Integer> userIdRowMapper = new RowMapper<Integer>() {
        @Override
        public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getInt("userid");
        }
    };

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> new User(
        rs.getInt("id"),
        rs.getString("name"),
        rs.getString("email"),
        rs.getString("password"),
        User.Role.valueOf(rs.getString("role").toUpperCase())
    );

}
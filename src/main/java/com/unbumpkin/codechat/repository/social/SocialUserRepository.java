package com.unbumpkin.codechat.repository.social;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.unbumpkin.codechat.dto.social.SocialUser;
import com.unbumpkin.codechat.security.CurrentUserProvider;

@Repository
public class SocialUserRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CurrentUserProvider currentUserProvider;

    private final RowMapper<SocialUser> rowMapper = new RowMapper<SocialUser>() {
        @Override
        public SocialUser mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SocialUser(
                rs.getString("userid"),
                rs.getString("fname"),
                rs.getString("email")
            );
        }
    };
    public SocialUser addSocialUser(SocialUser socialUser, int prId) {
        String sql = "INSERT INTO core.socialuser (userid, prid, fname, email) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, socialUser.userId(), prId, socialUser.fullName(), socialUser.email());
        return socialUser;
    }
    public void addMissingSocialUsers(Collection<SocialUser> socialUsers, int prId) {
        // Assuming there's a unique constraint on (userid, prid)
        // If not, you should add: ALTER TABLE socialuser ADD CONSTRAINT unique_social_user UNIQUE (userid, prid);
        
        String sql = """
            INSERT INTO core.socialuser (userid, prid, fname, email)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (userid, prid) DO NOTHING
            """;
        
        for (SocialUser socialUser : socialUsers) {
            jdbcTemplate.update(sql, 
                socialUser.userId(), 
                prId, 
                socialUser.fullName(), 
                socialUser.email() // This might be null if we made email nullable
            );
        }
    }
    public SocialUser getSocialUserByUserId(String userid) {
        String sql = "SELECT * FROM core.socialuser WHERE userid = ?";
        return jdbcTemplate.queryForObject(sql, rowMapper, userid);
    }

    public List<SocialUser> getAllSocialUsers(int prId) {
        String sql = "SELECT * FROM core.socialuser WHERE prid = ?";
        return jdbcTemplate.query(sql, rowMapper, prId);
    }

    public int deleteSocialUser(String userid) {
        String sql = "DELETE FROM core.socialuser WHERE userid = ?";
        return jdbcTemplate.update(sql, userid);
    }
    public int deleteAll(int prId) {
        String sql = "DELETE FROM core.socialuser WHERE prid = ?";
        return jdbcTemplate.update(sql, prId);
    }
    public int deleteAll() {
        // Check if current user is admin
        if (!currentUserProvider.getCurrentUser().isAdmin()) {
            throw new IllegalStateException("Only administrators can delete all social users");
        }
        
        String sql = "DELETE FROM core.socialuser";
        return jdbcTemplate.update(sql);
    }


}

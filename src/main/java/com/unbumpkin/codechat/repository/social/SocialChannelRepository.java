package com.unbumpkin.codechat.repository.social;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

import com.unbumpkin.codechat.dto.social.SocialChannel;
import com.unbumpkin.codechat.security.CustomAuthentication;

@Repository
public class SocialChannelRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<SocialChannel> rowMapper = new RowMapper<SocialChannel>() {
        @Override
        public SocialChannel mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SocialChannel(
                rs.getString("channelid"),
                rs.getString("channelname"),
                rs.getString("lastmessagets")
            );
        }
    };

    public SocialChannel addSocialChannel(SocialChannel channel, int prId) {
        String sql = "INSERT INTO socialchannel (channelid, prid, channelname, lastmessagets) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, channel.channelId(), prId, channel.channelName(), channel.lastMessageTs());
        return channel;
    }

    public void addMissingSocialChannels(List<SocialChannel> channels, int prId) {
        // Assuming there's a unique constraint on (channelid, prid)
        // If not, you should add: ALTER TABLE socialchannel ADD CONSTRAINT unique_channel UNIQUE (channelid, prid);
        
        String sql = """
            INSERT INTO socialchannel (channelid, prid, channelname, lastmessagets)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (channelid, prid) DO NOTHING
            """;
        
        for (SocialChannel channel : channels) {
            jdbcTemplate.update(sql,
                channel.channelId(),
                prId,
                channel.channelName(),
                channel.lastMessageTs()
            );
        }
    }
    
    public SocialChannel getSocialChannelById(String channelId, int prId) {
        String sql = "SELECT * FROM socialchannel WHERE channelid = ? AND prid = ?";
        return jdbcTemplate.queryForObject(sql, rowMapper, channelId, prId);
    }

    public List<SocialChannel> getAllSocialChannels(int prId) {
        String sql = "SELECT * FROM socialchannel WHERE prid = ?";
        return jdbcTemplate.query(sql, rowMapper, prId);
    }

    public int deleteSocialChannel(String channelId, int prId) {
        String sql = "DELETE FROM socialchannel WHERE channelid = ? AND prid = ?";
        return jdbcTemplate.update(sql, channelId, prId);
    }
    public void deleteAll() {
        CustomAuthentication currentUser = getCurrentUser();
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new IllegalStateException("Only admins can delete all social channels");
        }
        String sql = "DELETE FROM socialchannel";
        jdbcTemplate.update(sql);
    }
    
    private CustomAuthentication getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CustomAuthentication) {
            return (CustomAuthentication) authentication;
        }
        throw new IllegalStateException("No authenticated user found");
    }
}
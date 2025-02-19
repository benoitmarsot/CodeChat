package com.unbumpkin.codechat.config;

import com.unbumpkin.codechat.repository.UserRepository;
import com.unbumpkin.codechat.security.CustomAuthentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@Configuration
public class TestSecurityConfig {

    @Autowired
    UserRepository userRepository;
    @Bean
    public CustomAuthentication setUpSecurityContext() {
        int testUserId = userRepository.findFirstUserId();
        UserDetails userDetails = User.withUsername("testuser").password("").roles("USER").build();
        CustomAuthentication authentication = new CustomAuthentication(userDetails, null, userDetails.getAuthorities(), testUserId);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }
}
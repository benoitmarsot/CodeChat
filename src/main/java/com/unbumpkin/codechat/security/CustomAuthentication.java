package com.unbumpkin.codechat.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class CustomAuthentication extends UsernamePasswordAuthenticationToken {
    private final int userId;

    public CustomAuthentication(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities, int userId) {
        super(principal, credentials, authorities);
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }
}
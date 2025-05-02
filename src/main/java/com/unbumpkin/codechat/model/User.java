package com.unbumpkin.codechat.model;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record User(
    int userid, 
    String name, 
    String email,
    String password,
    Timestamp created,
    Role role
) implements UserDetails {
    public User(int userid, String name, String email, String password, Role role) {
        this(userid, name, email, password, null, role);
    }
    public static final int nbTrialDays=30;
    
    public enum Role {
        FREE, USER, ADMIN
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return (!Role.FREE.equals(role) 
            || (Role.FREE.equals(role) && trialDaysLeft() > 0));
    }

    public Timestamp getCreated() {
        return created;
    }
    public Timestamp getTrialEnd() {
        if(created == null) {
            return null;
        }
        return new Timestamp(created.getTime() + nbTrialDays * 24 * 60 * 60 * 1000);
    }
    public int trialDaysLeft() {
        if(created == null) {
            return 0;
        }
        if(Role.FREE.equals(role)) {
            int total=nbTrialDays - (int) ((System.currentTimeMillis() - created.getTime()) / (1000 * 60 * 60 * 24));
            return total>0 ? total : 0;
        }
        return 0;
    }
    public boolean isTrial() {
        return Role.FREE.equals(role);
    }


    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
    @Override
    public String getPassword() {
        return password;
    }

    public int userid() {
        return userid;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

}
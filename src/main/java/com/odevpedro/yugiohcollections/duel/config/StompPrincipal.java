package com.odevpedro.yugiohcollections.duel.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.Collection;
import java.util.List;

public class StompPrincipal implements Principal {

    private final String userId;
    private final String username;
    private final String role;

    public StompPrincipal(String userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    @Override
    public String getName() {
        return username;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
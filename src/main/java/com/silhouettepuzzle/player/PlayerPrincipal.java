package com.silhouettepuzzle.player;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The logged-in player as kept in the security context and the session.
 * The password hash is dropped once login is done.
 */
public final class PlayerPrincipal implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String username;
    private String passwordHash;

    public PlayerPrincipal(Player player) {
        this.id = player.id();
        this.username = player.username();
        this.passwordHash = player.passwordHash();
    }

    public String getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_PLAYER"));
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}

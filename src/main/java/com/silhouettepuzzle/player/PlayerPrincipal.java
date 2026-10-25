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
 */
public final class PlayerPrincipal implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_PLAYER"));

    private final String id;
    private final String username;
    private String passwordHash;

    /**
     * Creates the principal of a stored player.
     *
     * @param player the player
     */
    public PlayerPrincipal(Player player) {
        this.id = player.id();
        this.username = player.username();
        this.passwordHash = player.passwordHash();
    }

    /**
     * Returns the player identifier.
     *
     * @return the identifier
     */
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
        return AUTHORITIES;
    }

    /**
     * Drops the password hash once authentication is done, so it does not
     * stay in the session.
     */
    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}

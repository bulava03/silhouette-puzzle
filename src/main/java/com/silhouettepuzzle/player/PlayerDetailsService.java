package com.silhouettepuzzle.player;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads players for login; the username is matched ignoring case.
 */
@Service
public class PlayerDetailsService implements UserDetailsService {

    private final PlayerRepository players;

    /**
     * Creates the service.
     *
     * @param players player storage
     */
    public PlayerDetailsService(PlayerRepository players) {
        this.players = players;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return players.findByUsernameKey(Player.keyOf(username))
                .map(PlayerPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("No player " + username));
    }
}

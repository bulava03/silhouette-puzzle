package com.silhouettepuzzle.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.silhouettepuzzle.entity.Player;
import com.silhouettepuzzle.repository.PlayerRepository;

/**
 * Loads players for login, matching the username regardless of case.
 */
@Service
public class PlayerDetailsService implements UserDetailsService {

    private final PlayerRepository players;

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

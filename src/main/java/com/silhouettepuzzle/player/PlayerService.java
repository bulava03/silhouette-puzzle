package com.silhouettepuzzle.player;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.silhouettepuzzle.ApiException;

/**
 * Registers players.
 */
@Service
public class PlayerService {

    /** BCrypt only looks at the first 72 bytes of a password. */
    private static final int MAX_PASSWORD_BYTES = 72;

    private final PlayerRepository players;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public PlayerService(PlayerRepository players, PasswordEncoder encoder, Clock clock) {
        this.players = players;
        this.encoder = encoder;
        this.clock = clock;
    }

    public Player register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw ApiException.badRequest("Password must be at most 72 bytes in UTF-8");
        }
        String key = Player.keyOf(request.username());
        if (players.existsByUsernameKey(key)) {
            throw taken(request.username());
        }
        try {
            return players.save(new Player(null, request.username(), key, encoder.encode(request.password()),
                    clock.instant(), List.of()));
        } catch (DuplicateKeyException e) {
            // Someone registered the same name between the check and the insert.
            throw taken(request.username());
        }
    }

    private static ApiException taken(String username) {
        return ApiException.conflict("Username is taken: " + username);
    }
}

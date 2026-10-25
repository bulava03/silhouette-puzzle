package com.silhouettepuzzle.player;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponseException;

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

    /**
     * Creates the service.
     *
     * @param players player storage
     * @param encoder password encoder
     * @param clock   source of the registration time
     */
    public PlayerService(PlayerRepository players, PasswordEncoder encoder, Clock clock) {
        this.players = players;
        this.encoder = encoder;
        this.clock = clock;
    }

    /**
     * Registers a player.
     *
     * @param request username and password, already validated for format
     * @return the new player
     */
    public Player register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST, ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST, "Password must be at most 72 bytes in UTF-8"), null);
        }
        String key = Player.keyOf(request.username());
        if (players.existsByUsernameKey(key)) {
            throw new UsernameTakenException(request.username());
        }
        Player player = new Player(null, request.username(), key, encoder.encode(request.password()), clock.instant(),
                List.of());
        try {
            return players.save(player);
        } catch (DuplicateKeyException e) {
            // Someone registered the same name between the check and the insert.
            throw new UsernameTakenException(request.username());
        }
    }
}

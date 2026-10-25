package com.silhouettepuzzle.player;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.silhouettepuzzle.attempt.LeaderboardService;
import com.silhouettepuzzle.attempt.ProgressEntry;

import jakarta.validation.Valid;

/**
 * Registration and the current player's data. Login and logout are handled
 * by Spring Security at {@code /login} and {@code /logout}.
 */
@RestController
public class PlayerController {

    private final PlayerService players;
    private final LeaderboardService leaderboards;

    /**
     * Creates the controller.
     *
     * @param players      registration logic
     * @param leaderboards results queries
     */
    public PlayerController(PlayerService players, LeaderboardService leaderboards) {
        this.players = players;
        this.leaderboards = leaderboards;
    }

    /**
     * Registers a player.
     *
     * @param request username and password
     * @return 201 with the new player
     */
    @PostMapping("/api/players")
    public ResponseEntity<PlayerView> register(@Valid @RequestBody RegisterRequest request) {
        Player player = players.register(request);
        return ResponseEntity.created(URI.create("/api/players/" + player.id()))
                .body(new PlayerView(player.id(), player.username()));
    }

    /**
     * Returns the logged-in player.
     *
     * @param me the logged-in player
     * @return the player
     */
    @GetMapping("/api/me")
    public PlayerView me(@AuthenticationPrincipal PlayerPrincipal me) {
        return new PlayerView(me.getId(), me.getUsername());
    }

    /**
     * Returns the logged-in player's personal bests per solved level.
     *
     * @param me the logged-in player
     * @return progress per level
     */
    @GetMapping("/api/me/progress")
    public List<ProgressEntry> progress(@AuthenticationPrincipal PlayerPrincipal me) {
        return leaderboards.progressOf(me.getId());
    }
}

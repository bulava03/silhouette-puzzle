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
import com.silhouettepuzzle.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Registration and the current player's data. Login and logout are handled
 * by Spring Security at {@code /login} and {@code /logout}.
 */
@RestController
@Tag(name = "Players", description = "Registration and the logged-in player")
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
    @Operation(summary = "Register", description = "Usernames are unique regardless of case. "
            + "Log in afterwards through POST /login.")
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
    @Operation(summary = "Get the logged-in player", security = @SecurityRequirement(name = OpenApiConfig.SESSION))
    public PlayerView me(@Parameter(hidden = true) @AuthenticationPrincipal PlayerPrincipal me) {
        return new PlayerView(me.getId(), me.getUsername());
    }

    /**
     * Returns the logged-in player's personal bests per solved level.
     *
     * @param me the logged-in player
     * @return progress per level
     */
    @GetMapping("/api/me/progress")
    @Operation(summary = "Get personal bests per solved level",
            security = @SecurityRequirement(name = OpenApiConfig.SESSION))
    public List<ProgressEntry> progress(@Parameter(hidden = true) @AuthenticationPrincipal PlayerPrincipal me) {
        return leaderboards.progressOf(me.getId());
    }
}

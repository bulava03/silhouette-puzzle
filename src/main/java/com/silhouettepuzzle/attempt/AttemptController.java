package com.silhouettepuzzle.attempt;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.silhouettepuzzle.player.PlayerPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Starting attempts, submitting solutions and level leaderboards.
 */
@RestController
@Tag(name = "Attempts", description = "Timed attempts, solution checking and leaderboards")
public class AttemptController {

    /** Most entries a leaderboard request may ask for. */
    static final int MAX_LEADERBOARD = 100;

    private final AttemptService attempts;
    private final LeaderboardService leaderboards;

    /**
     * Creates the controller.
     *
     * @param attempts     attempt logic
     * @param leaderboards results queries
     */
    public AttemptController(AttemptService attempts, LeaderboardService leaderboards) {
        this.attempts = attempts;
        this.leaderboards = leaderboards;
    }

    /**
     * Starts a timed attempt at a level.
     *
     * @param levelId the level to play
     * @param player  the logged-in player, or null when anonymous
     * @return 201 with the attempt
     */
    @PostMapping("/api/levels/{levelId}/attempts")
    @Operation(summary = "Start a timed attempt",
            description = "The server records the start time. Attempts of logged-in players count for "
                    + "leaderboards; anonymous attempts can be played but are not ranked.")
    public ResponseEntity<StartedAttempt> start(@PathVariable String levelId,
                                                @Parameter(hidden = true)
                                                @AuthenticationPrincipal PlayerPrincipal player) {
        Attempt attempt = attempts.start(levelId, player);
        return ResponseEntity.created(URI.create("/api/attempts/" + attempt.id())).body(StartedAttempt.of(attempt));
    }

    /**
     * Checks a solution for an attempt.
     *
     * @param attemptId the attempt
     * @param player    the logged-in player, or null when anonymous
     * @param request   placements and move count
     * @return the verdict
     */
    @PostMapping("/api/attempts/{attemptId}/submit")
    @Operation(summary = "Submit a solution",
            description = "One placement per level piece, in level order: position of the piece origin, "
                    + "rotation clockwise in 45-degree steps, and whether the piece is mirrored. "
                    + "Accepted when overlap and mismatch both stay within the tolerance; "
                    + "a rejected submission keeps the attempt open.")
    public SubmitResult submit(@PathVariable String attemptId,
                               @Parameter(hidden = true) @AuthenticationPrincipal PlayerPrincipal player,
                               @Valid @RequestBody SubmitRequest request) {
        return attempts.submit(attemptId, player, request.placements(), request.moves());
    }

    /**
     * Returns the best result of each player on a level.
     *
     * @param levelId the level
     * @param limit   how many places to return, 1 to 100
     * @return leaderboard entries, best first
     */
    @GetMapping("/api/levels/{levelId}/leaderboard")
    @Operation(summary = "Get a level leaderboard",
            description = "Best run of each player, ranked by time, then moves, then who solved first.")
    public List<LeaderboardEntry> leaderboard(@PathVariable String levelId,
                                              @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_LEADERBOARD)
                                              int limit) {
        return leaderboards.leaderboard(levelId, limit);
    }
}

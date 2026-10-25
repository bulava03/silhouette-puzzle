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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Starting attempts, submitting solutions and level leaderboards.
 */
@RestController
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
    public ResponseEntity<StartedAttempt> start(@PathVariable String levelId,
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
    public SubmitResult submit(@PathVariable String attemptId, @AuthenticationPrincipal PlayerPrincipal player,
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
    public List<LeaderboardEntry> leaderboard(@PathVariable String levelId,
                                              @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_LEADERBOARD)
                                              int limit) {
        return leaderboards.leaderboard(levelId, limit);
    }
}

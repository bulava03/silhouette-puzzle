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

    private final AttemptService attempts;
    private final LeaderboardService leaderboards;

    public AttemptController(AttemptService attempts, LeaderboardService leaderboards) {
        this.attempts = attempts;
        this.leaderboards = leaderboards;
    }

    @PostMapping("/api/levels/{levelId}/attempts")
    @Operation(summary = "Start a timed attempt",
            description = "Logged-in players' attempts count for leaderboards; anonymous ones are not ranked.")
    public ResponseEntity<Attempt> start(@PathVariable String levelId,
                                         @Parameter(hidden = true) @AuthenticationPrincipal PlayerPrincipal player) {
        Attempt attempt = attempts.start(levelId, player);
        return ResponseEntity.created(URI.create("/api/attempts/" + attempt.id())).body(attempt);
    }

    @PostMapping("/api/attempts/{attemptId}/submit")
    @Operation(summary = "Submit a solution",
            description = "One placement per piece, in level order: position of the piece origin, rotation "
                    + "clockwise in 45-degree steps, and whether it is mirrored. Accepted when overlap and "
                    + "mismatch both stay within 1% of the silhouette. An attempt can be submitted for a day.")
    public SubmitResult submit(@PathVariable String attemptId,
                               @Parameter(hidden = true) @AuthenticationPrincipal PlayerPrincipal player,
                               @Valid @RequestBody SubmitRequest request) {
        return attempts.submit(attemptId, player, request.placements(), request.moves());
    }

    @GetMapping("/api/levels/{levelId}/leaderboard")
    @Operation(summary = "Get a level leaderboard",
            description = "Best run of each player, ranked by time, then moves, then who solved first.")
    public List<LeaderboardEntry> leaderboard(@PathVariable String levelId,
                                              @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return leaderboards.leaderboard(levelId, limit);
    }
}

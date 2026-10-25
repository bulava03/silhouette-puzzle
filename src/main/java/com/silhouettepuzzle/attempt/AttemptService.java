package com.silhouettepuzzle.attempt;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.silhouettepuzzle.ApiException;
import com.silhouettepuzzle.geometry.CheckResult;
import com.silhouettepuzzle.geometry.Placement;
import com.silhouettepuzzle.geometry.SolutionChecker;
import com.silhouettepuzzle.level.Level;
import com.silhouettepuzzle.level.LevelRepository;
import com.silhouettepuzzle.player.PlayerBests;
import com.silhouettepuzzle.player.PlayerPrincipal;

/**
 * Starts attempts and checks submitted solutions. Time is measured on the
 * server only: the client never reports how long it took.
 */
@Service
public class AttemptService {

    private final AttemptRepository attempts;
    private final LevelRepository levels;
    private final MongoTemplate mongo;
    private final SolutionChecker checker;
    private final PlayerBests bests;
    private final Clock clock;
    private final Duration lifetime;

    public AttemptService(AttemptRepository attempts, LevelRepository levels, MongoTemplate mongo,
                          SolutionChecker checker, PlayerBests bests, Clock clock,
                          @Value("${app.attempts.lifetime}") Duration lifetime) {
        this.attempts = attempts;
        this.levels = levels;
        this.mongo = mongo;
        this.checker = checker;
        this.bests = bests;
        this.clock = clock;
        this.lifetime = lifetime;
    }

    public Attempt start(String levelId, PlayerPrincipal player) {
        if (!levels.existsById(levelId)) {
            throw Level.notFound(levelId);
        }
        return attempts.save(Attempt.start(levelId, player, clock.instant()));
    }

    /**
     * Checks a solution. A rejected one keeps the attempt open until its
     * lifetime runs out; an accepted one closes it and goes to the player's bests.
     */
    public SubmitResult submit(String attemptId, PlayerPrincipal player, List<Placement> placements, int moves) {
        // Someone else's attempt is reported as missing, so ids cannot be probed.
        Attempt attempt = attempts.findById(attemptId)
                .filter(found -> found.playableBy(player))
                .orElseThrow(() -> ApiException.notFound("Attempt not found: " + attemptId));
        if (attempt.solvedAt() != null) {
            throw alreadySolved(attemptId);
        }
        Instant now = clock.instant();
        if (Duration.between(attempt.startedAt(), now).compareTo(lifetime) > 0) {
            throw ApiException.gone("Attempt " + attemptId + " has expired; start the level again");
        }
        Level level = levels.findById(attempt.levelId()).orElseThrow(() -> Level.notFound(attempt.levelId()));
        if (placements.size() != level.pieces().size()) {
            throw ApiException.badRequest("Level " + level.id() + " has " + level.pieces().size()
                    + " pieces, got " + placements.size() + " placements");
        }

        CheckResult result = checker.check(level.silhouette(), level.pieceOutlines(), placements);
        if (!result.solved()) {
            return SubmitResult.of(result, null, null);
        }
        long timeMs = Duration.between(attempt.startedAt(), now).toMillis();
        // Only an attempt that is still unsolved gets marked, so of concurrent
        // correct submissions exactly one counts.
        Update solved = new Update().set("solvedAt", now).set("timeMs", timeMs).set("moves", moves);
        if (mongo.updateFirst(query(where("_id").is(attemptId).and("solvedAt").isNull()), solved, Attempt.class)
                .getModifiedCount() == 0) {
            throw alreadySolved(attemptId);
        }
        if (attempt.playerId() != null) {
            bests.record(attempt.playerId(), level.id(), timeMs, moves, now);
        }
        return SubmitResult.of(result, timeMs, moves);
    }

    private static ApiException alreadySolved(String attemptId) {
        return ApiException.conflict("Attempt already solved: " + attemptId);
    }
}

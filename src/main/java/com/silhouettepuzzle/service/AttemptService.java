package com.silhouettepuzzle.service;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.silhouettepuzzle.ApiException;
import com.silhouettepuzzle.dto.SubmitRequest;
import com.silhouettepuzzle.dto.SubmitResult;
import com.silhouettepuzzle.entity.Attempt;
import com.silhouettepuzzle.entity.Level;
import com.silhouettepuzzle.geometry.PuzzleGeometry;
import com.silhouettepuzzle.repository.AttemptRepository;
import com.silhouettepuzzle.security.PlayerPrincipal;

/**
 * Timed attempts and the checking of pictures. Time is measured on the
 * server only, and only a logged-in player's attempt is timed and counts.
 */
@Service
public class AttemptService {

    private final AttemptRepository attempts;
    private final LevelService levels;
    private final PlayerService players;
    private final PuzzleGeometry geometry;
    private final MongoTemplate mongo;
    private final Clock clock;
    private final Duration lifetime;

    public AttemptService(AttemptRepository attempts, LevelService levels, PlayerService players,
                          PuzzleGeometry geometry, MongoTemplate mongo, Clock clock,
                          @Value("${app.attempts.lifetime}") Duration lifetime) {
        this.attempts = attempts;
        this.levels = levels;
        this.players = players;
        this.geometry = geometry;
        this.mongo = mongo;
        this.clock = clock;
        this.lifetime = lifetime;
    }

    public Attempt start(Level level, PlayerPrincipal player) {
        return attempts.save(new Attempt(null, level.id(), player.getId(), clock.instant(), null, null));
    }

    /**
     * Checks a picture. With a logged-in player's attempt, a solved picture
     * closes the attempt, which can then count for the leaderboard.
     */
    public SubmitResult submit(String levelId, SubmitRequest request, PlayerPrincipal player) {
        Level level = levels.get(levelId);
        Attempt attempt = player == null || request.attemptId() == null ? null : open(request.attemptId(), level, player);
        if (request.placements().size() != level.pieces().size()) {
            throw ApiException.badRequest("Level " + level.id() + " has " + level.pieces().size()
                    + " pieces, got " + request.placements().size() + " placements");
        }
        PuzzleGeometry.Check check = geometry.check(level.silhouette(), level.pieceOutlines(), request.placements());
        double off = check.overlap() + check.mismatch();
        if (!check.solved() || attempt == null) {
            return new SubmitResult(check.solved(), off, null, null);
        }
        Instant now = clock.instant();
        long timeMs = Duration.between(attempt.startedAt(), now).toMillis();
        // Only a still unsolved attempt gets marked, so of concurrent correct
        // submissions exactly one counts.
        if (mongo.updateFirst(query(where("_id").is(attempt.id()).and("solvedAt").isNull()),
                new Update().set("solvedAt", now).set("timeMs", timeMs), Attempt.class).getModifiedCount() == 0) {
            throw alreadySolved(attempt.id());
        }
        players.recordBest(player.getId(), new Attempt(attempt.id(), level.id(), player.getId(),
                attempt.startedAt(), now, timeMs));
        Attempt best = players.find(player.getId()).flatMap(p -> p.bestOn(level.id())).orElseThrow();
        return new SubmitResult(true, off, timeMs, players.rankOf(best));
    }

    /** The player's open attempt at the level; someone else's is reported as missing. */
    private Attempt open(String attemptId, Level level, PlayerPrincipal player) {
        Attempt attempt = attempts.findById(attemptId)
                .filter(found -> found.playerId().equals(player.getId()) && found.levelId().equals(level.id()))
                .orElseThrow(() -> ApiException.notFound("Attempt not found: " + attemptId));
        if (attempt.solvedAt() != null) {
            throw alreadySolved(attemptId);
        }
        if (Duration.between(attempt.startedAt(), clock.instant()).compareTo(lifetime) > 0) {
            throw ApiException.gone("Attempt " + attemptId + " has expired; start the level again");
        }
        return attempt;
    }

    private static ApiException alreadySolved(String attemptId) {
        return ApiException.conflict("Attempt already solved: " + attemptId);
    }
}

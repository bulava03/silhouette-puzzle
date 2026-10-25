package com.silhouettepuzzle.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A logged-in player's try at a level, timed by the server. Open attempts
 * are temporary documents that MongoDB removes two days after they start;
 * a player's best solved attempt per level is kept inside the player.
 */
@Document("attempts")
public record Attempt(
        @Id String id,
        String levelId,
        String playerId,
        Instant startedAt,
        Instant solvedAt,
        Long timeMs) {

    /** Ranking order: faster first, then whoever solved first. */
    public boolean beats(Attempt other) {
        return timeMs < other.timeMs || timeMs.equals(other.timeMs) && solvedAt.isBefore(other.solvedAt);
    }
}

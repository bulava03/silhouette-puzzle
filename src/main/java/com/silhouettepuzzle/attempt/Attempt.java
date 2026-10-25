package com.silhouettepuzzle.attempt;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One try at a level, timed by the server.
 *
 * @param id        attempt identifier, assigned by MongoDB
 * @param levelId   the level being played
 * @param startedAt when the server started the attempt
 * @param solvedAt  when the server accepted a solution; null while unsolved
 * @param timeMs    time from start to the accepted solution; null while unsolved
 * @param moves     moves reported by the client with the accepted solution
 */
@Document("attempts")
public record Attempt(
        @Id String id,
        String levelId,
        Instant startedAt,
        Instant solvedAt,
        Long timeMs,
        Integer moves) {

    /**
     * Creates a new, unsolved attempt.
     *
     * @param levelId   the level being played
     * @param startedAt start time
     * @return the attempt, not yet stored
     */
    public static Attempt start(String levelId, Instant startedAt) {
        return new Attempt(null, levelId, startedAt, null, null, null);
    }

    /**
     * Tells whether a solution has been accepted.
     *
     * @return true once solved
     */
    public boolean solved() {
        return solvedAt != null;
    }
}

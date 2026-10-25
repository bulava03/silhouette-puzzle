package com.silhouettepuzzle.attempt;

import java.time.Instant;

/**
 * A freshly started attempt as returned to the client.
 *
 * @param id        attempt identifier, used to submit the solution
 * @param levelId   the level being played
 * @param startedAt server start time
 */
public record StartedAttempt(String id, String levelId, Instant startedAt) {

    /**
     * Creates the response for a stored attempt.
     *
     * @param attempt the attempt
     * @return the response
     */
    public static StartedAttempt of(Attempt attempt) {
        return new StartedAttempt(attempt.id(), attempt.levelId(), attempt.startedAt());
    }
}

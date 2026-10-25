package com.silhouettepuzzle.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The verdict on a picture. {@code off} is how much of it misses the
 * silhouette or overlaps, as a fraction of the silhouette area. A solved
 * timed attempt also carries the server-measured time and the player's
 * place on the leaderboard.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubmitResult(boolean solved, double off, Long timeMs, Integer rank) {
}

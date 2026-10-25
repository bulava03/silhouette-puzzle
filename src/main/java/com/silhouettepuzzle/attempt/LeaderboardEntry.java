package com.silhouettepuzzle.attempt;

import java.time.Instant;

/**
 * A player's best run on a level, with its place on the leaderboard.
 */
public record LeaderboardEntry(int rank, String username, long timeMs, int moves, Instant solvedAt) {
}

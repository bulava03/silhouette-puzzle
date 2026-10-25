package com.silhouettepuzzle.attempt;

import java.time.Instant;

/**
 * A player's best result on a level.
 *
 * @param rank     place on the leaderboard, starting at 1
 * @param username the player
 * @param timeMs   server-measured time
 * @param moves    moves of that run
 * @param solvedAt when that run was solved
 */
public record LeaderboardEntry(int rank, String username, long timeMs, int moves, Instant solvedAt) {
}

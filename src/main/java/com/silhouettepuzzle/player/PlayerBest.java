package com.silhouettepuzzle.player;

import java.time.Instant;

/**
 * A player's best run on one level, kept inside the player's document.
 *
 * @param levelId  the level
 * @param timeMs   server-measured time of the best run
 * @param moves    moves of the best run
 * @param solvedAt when the best run was solved
 * @param solves   how many times the player has solved the level
 */
public record PlayerBest(String levelId, long timeMs, int moves, Instant solvedAt, int solves) {
}

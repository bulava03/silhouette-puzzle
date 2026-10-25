package com.silhouettepuzzle.attempt;

/**
 * A player's best run on one level.
 *
 * @param levelId    the level
 * @param solves     how many times the player solved it
 * @param bestTimeMs time of the best run
 * @param moves      moves of the best run
 */
public record ProgressEntry(String levelId, int solves, long bestTimeMs, int moves) {
}

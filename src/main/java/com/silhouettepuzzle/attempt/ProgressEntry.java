package com.silhouettepuzzle.attempt;

/**
 * A player's personal bests on one level. The best time and the fewest
 * moves may come from different runs.
 *
 * @param levelId     the level
 * @param solves      how many times the player solved it
 * @param bestTimeMs  fastest time
 * @param fewestMoves fewest moves
 */
public record ProgressEntry(String levelId, int solves, long bestTimeMs, int fewestMoves) {
}

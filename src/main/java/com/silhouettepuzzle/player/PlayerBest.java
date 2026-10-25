package com.silhouettepuzzle.player;

import java.time.Instant;

/**
 * A player's best run on one level and how many times they solved it.
 */
public record PlayerBest(String levelId, long timeMs, int moves, Instant solvedAt, int solves) {
}

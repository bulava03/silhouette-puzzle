package com.silhouettepuzzle.geometry;

/**
 * Outcome of checking a solution. Both measures are fractions of the
 * silhouette area, so they do not depend on the scale of a level.
 *
 * @param solved         whether the solution is accepted
 * @param overlapRatio   area where pieces lie on top of each other
 * @param mismatchRatio  area covered by pieces but not the silhouette, plus
 *                       area of the silhouette left uncovered
 */
public record CheckResult(boolean solved, double overlapRatio, double mismatchRatio) {
}

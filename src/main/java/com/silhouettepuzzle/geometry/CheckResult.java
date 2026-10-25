package com.silhouettepuzzle.geometry;

/**
 * Outcome of checking a solution. The overlap between pieces and the
 * mismatch with the silhouette are fractions of the silhouette area.
 */
public record CheckResult(boolean solved, double overlapRatio, double mismatchRatio) {
}

package com.silhouettepuzzle.geometry;

/**
 * A point in board coordinates. The board uses screen orientation, as SVG
 * does: x grows to the right and y grows downwards.
 *
 * @param x horizontal coordinate
 * @param y vertical coordinate, growing downwards
 */
public record Point(double x, double y) {

    /**
     * Rejects coordinates that are not finite numbers.
     */
    public Point {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Point coordinates must be finite: (" + x + ", " + y + ")");
        }
    }
}

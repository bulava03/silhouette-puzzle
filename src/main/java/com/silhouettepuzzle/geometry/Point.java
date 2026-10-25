package com.silhouettepuzzle.geometry;

/**
 * A point on the board. The board uses screen orientation, as SVG does:
 * x grows to the right and y grows downwards.
 */
public record Point(double x, double y) {

    public Point {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Point coordinates must be finite: (" + x + ", " + y + ")");
        }
    }
}

package com.silhouettepuzzle.geometry;

import java.util.List;

/**
 * The classic seven-piece tangram assembled into a 4 x 4 square, used as a
 * known-good solution in tests. Each piece is described around its own
 * origin, so the placements exercise translation, every kind of rotation
 * (including 45 degrees) and the mirrored parallelogram.
 */
final class Tangram {

    static final double H = Math.sqrt(2) / 2;

    static final Outline LARGE_TRIANGLE = Outline.of(-2, -2, 2, -2, 0, 0);
    static final Outline MEDIUM_TRIANGLE = Outline.of(-2 * H, 0, 2 * H, 0, 0, 2 * H);
    static final Outline SMALL_TRIANGLE = Outline.of(-1, -1, 1, -1, 0, 0);
    static final Outline SQUARE = Outline.of(-H, -H, H, -H, H, H, -H, H);
    /** Stored mirrored: it only fits the square when flipped back. */
    static final Outline PARALLELOGRAM = Outline.of(0, 0, -1, -1, -1, 1, 0, 2);

    static final List<Outline> SILHOUETTE = List.of(Outline.of(0, 0, 4, 0, 4, 4, 0, 4));

    static final List<Outline> PIECES = List.of(
            LARGE_TRIANGLE, LARGE_TRIANGLE, MEDIUM_TRIANGLE,
            SMALL_TRIANGLE, SMALL_TRIANGLE, SQUARE, PARALLELOGRAM);

    static final List<Placement> SOLUTION = List.of(
            new Placement(2, 2, 0, false),
            new Placement(2, 2, 270, false),
            new Placement(3, 3, 315, false),
            new Placement(2, 2, 90, false),
            new Placement(1, 3, 180, false),
            new Placement(2, 3, 45, false),
            new Placement(3, 1, 0, true));

    private Tangram() {
    }
}

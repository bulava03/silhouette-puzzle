package com.silhouettepuzzle.geometry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

class GeometriesTest {

    /** A 2 x 1 rectangle to the right of the origin. */
    private static final Outline BAR = Outline.of(0, 0, 2, 0, 2, 1, 0, 1);

    @Test
    void movesThePieceOriginToThePlacementPosition() {
        Geometry placed = Geometries.place(BAR, new Placement(5, 7, 0, false));

        assertThat(placed.equalsExact(polygon(5, 7, 7, 7, 7, 8, 5, 8), 1e-9)).isTrue();
    }

    @Test
    void rotatesClockwiseOnScreen() {
        // y points down: turning clockwise by 90 degrees takes "right" to "down".
        Geometry placed = Geometries.place(BAR, new Placement(0, 0, 90, false));

        assertThat(placed.equalsExact(polygon(0, 0, 0, 2, -1, 2, -1, 0), 1e-9)).isTrue();
    }

    @Test
    void rotatesByFortyFiveDegrees() {
        Geometry placed = Geometries.place(Outline.of(0, 0, 1, 0, 0, 1), new Placement(0, 0, 45, false));
        double h = Math.sqrt(2) / 2;

        assertThat(placed.equalsExact(polygon(0, 0, h, h, -h, h), 1e-9)).isTrue();
    }

    @Test
    void flipsBeforeRotating() {
        // Mirrored to the left of the origin, then turned "left" to "up".
        Geometry placed = Geometries.place(BAR, new Placement(0, 0, 90, true));

        assertThat(placed.equalsExact(polygon(0, 0, 0, -2, -1, -2, -1, 0), 1e-9)).isTrue();
    }

    private static Geometry polygon(double... coordinates) {
        return Geometries.toPolygon(Outline.of(coordinates).vertices());
    }
}

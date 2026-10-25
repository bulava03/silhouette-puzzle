package com.silhouettepuzzle.geometry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

class GeometryTest {

    /** A 2 x 1 bar to the right of the origin. */
    private static final Outline BAR = Outline.of(0, 0, 2, 0, 2, 1, 0, 1);

    @Test
    void placesPiecesAsSvgDoes() {
        double h = Math.sqrt(2) / 2;
        // Moved, then turned clockwise on screen (y points down: "right" becomes "down"),
        // by 45 degrees, and mirrored before turning.
        assertPlaced(BAR, new Placement(5, 7, 0, false), 5, 7, 7, 7, 7, 8, 5, 8);
        assertPlaced(BAR, new Placement(0, 0, 90, false), 0, 0, 0, 2, -1, 2, -1, 0);
        assertPlaced(Outline.of(0, 0, 1, 0, 0, 1), new Placement(0, 0, 45, false), 0, 0, h, h, -h, h);
        assertPlaced(BAR, new Placement(0, 0, 90, true), 0, 0, 0, -2, -1, -2, -1, 0);
    }

    @Test
    void acceptsOnlyValidShapesAndWholeStepRotations() {
        assertThat(Outline.of(0, 0, 4, 0, 4, 3, 0, 3).area()).isEqualTo(12);
        for (double[] invalid : new double[][] {
                {0, 0, 1, 1},                // two vertices
                {0, 0, 2, 2, 2, 0, 0, 2},    // a self-intersecting bow tie
                {0, 0, 1, 1, 2, 2},          // no area
                {0, 0, 1, 0, 1},             // odd number of coordinates
                {0, 0, Double.NaN, 0, 1, 1}}) {
            assertThatThrownBy(() -> Outline.of(invalid)).isInstanceOf(IllegalArgumentException.class);
        }
        for (int rotation = -45; rotation <= 405; rotation += 15) {
            int angle = rotation;
            if (angle >= 0 && angle < 360 && angle % 45 == 0) {
                assertThat(new Placement(0, 0, angle, false).rotation()).isEqualTo(angle);
            } else {
                assertThatThrownBy(() -> new Placement(0, 0, angle, false))
                        .as("rotation %d", angle).isInstanceOf(IllegalArgumentException.class);
            }
        }
        assertThatThrownBy(() -> new Placement(Double.NaN, 0, 0, false)).isInstanceOf(IllegalArgumentException.class);
    }

    private static void assertPlaced(Outline piece, Placement placement, double... expected) {
        Geometry placed = Geometries.place(piece, placement);
        assertThat(placed.equalsExact(Geometries.toPolygon(Outline.of(expected).vertices()), 1e-9))
                .as("%s", placement).isTrue();
    }
}

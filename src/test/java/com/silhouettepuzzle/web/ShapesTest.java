package com.silhouettepuzzle.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.silhouettepuzzle.geometry.Outline;
import com.silhouettepuzzle.geometry.Placement;

class ShapesTest {

    @Test
    void formatsPointsWithoutTrailingZeros() {
        assertThat(Shapes.points(Outline.of(0, 0, 2.5, 0, 2.5, 1.25))).isEqualTo("0,0 2.5,0 2.5,1.25");
    }

    @Test
    void framesOutlinesWithAMargin() {
        assertThat(Shapes.viewBox(List.of(Outline.of(1, 2, 4, 2, 4, 3)), 0.5)).isEqualTo("0.5 1.5 4 2");
    }

    @Test
    void placesLikeTheServerChecks() {
        // A bar right of the origin, mirrored to the left, then turned "left" to "up".
        Outline bar = Outline.of(0, 0, 2, 0, 2, 1, 0, 1);

        Outline placed = Shapes.placed(bar, new Placement(10, 10, 90, true));

        assertThat(Shapes.points(placed)).isEqualTo("10,10 10,8 9,8 9,10");
    }
}

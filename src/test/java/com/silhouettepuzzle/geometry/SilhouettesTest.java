package com.silhouettepuzzle.geometry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class SilhouettesTest {

    private static final Outline UNIT = Outline.of(0, 0, 1, 0, 1, 1, 0, 1);

    @Test
    void assemblesTheTangramIntoAPlainSquare() {
        // Vertices where pieces meet along a straight edge are dropped and
        // float noise from the 45-degree rotations is rounded away.
        assertThat(Silhouettes.assemble(Tangram.PIECES, Tangram.SOLUTION)).singleElement()
                .satisfies(square -> assertThat(square.vertices()).containsExactlyInAnyOrder(
                        new Point(0, 0), new Point(4, 0), new Point(4, 4), new Point(0, 4)));
        // Pieces touching only at a corner stay separate parts.
        assertThat(Silhouettes.assemble(List.of(UNIT, UNIT), List.of(at(0, 0), at(1, 1)))).hasSize(2);
    }

    @Test
    void rejectsOverlapsAndHoles() {
        assertThatThrownBy(() -> Silhouettes.assemble(List.of(UNIT, UNIT), List.of(at(0, 0), at(0.5, 0))))
                .hasMessageContaining("overlap");
        // A 3 x 3 frame: bars on top and bottom, unit squares left and right in the middle row.
        Outline bar = Outline.of(0, 0, 3, 0, 3, 1, 0, 1);
        assertThatThrownBy(() -> Silhouettes.assemble(List.of(bar, bar, UNIT, UNIT),
                List.of(at(0, 0), at(0, 2), at(0, 1), at(2, 1))))
                .hasMessageContaining("hole");
    }

    private static Placement at(double x, double y) {
        return new Placement(x, y, 0, false);
    }
}

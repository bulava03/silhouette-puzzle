package com.silhouettepuzzle.geometry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;

class SilhouettesTest {

    private static final Outline UNIT = Outline.of(0, 0, 1, 0, 1, 1, 0, 1);
    private static final Placement ORIGIN = new Placement(0, 0, 0, false);

    @Test
    void assemblesTheTangramSquareIntoFourCorners() {
        List<Outline> silhouette = Silhouettes.assemble(Tangram.PIECES, Tangram.SOLUTION);

        // Vertices where pieces meet along a straight edge are dropped,
        // and float noise from the 45-degree rotations is rounded away.
        assertThat(silhouette).singleElement().satisfies(square -> {
            assertThat(square.vertices()).hasSize(4);
            assertThat(square.vertices()).containsExactlyInAnyOrder(
                    new Point(0, 0), new Point(4, 0), new Point(4, 4), new Point(0, 4));
        });
    }

    @Test
    void keepsPartsThatOnlyTouchAtACorner() {
        List<Outline> silhouette = Silhouettes.assemble(List.of(UNIT, UNIT),
                List.of(ORIGIN, new Placement(1, 1, 0, false)));

        assertThat(silhouette).hasSize(2);
        assertThat(silhouette).allSatisfy(part -> assertThat(part.area()).isCloseTo(1, within(1e-9)));
    }

    @Test
    void rejectsOverlappingPieces() {
        assertThatThrownBy(() -> Silhouettes.assemble(List.of(UNIT, UNIT),
                List.of(ORIGIN, new Placement(0.5, 0, 0, false))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    void rejectsASilhouetteWithAHole() {
        Outline bar = Outline.of(0, 0, 3, 0, 3, 1, 0, 1);
        Outline post = Outline.of(0, 0, 1, 0, 1, 1, 0, 1);
        // A 3 x 3 frame: two bars on top and bottom, two unit squares in the middle row.
        assertThatThrownBy(() -> Silhouettes.assemble(List.of(bar, bar, post, post), List.of(
                ORIGIN, new Placement(0, 2, 0, false), new Placement(0, 1, 0, false), new Placement(2, 1, 0, false))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hole");
    }

    @Test
    void rejectsAMismatchedNumberOfPlacements() {
        assertThatThrownBy(() -> Silhouettes.assemble(List.of(UNIT), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

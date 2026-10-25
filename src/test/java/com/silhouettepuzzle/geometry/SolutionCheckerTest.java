package com.silhouettepuzzle.geometry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class SolutionCheckerTest {

    private final SolutionChecker checker = new SolutionChecker(0.01);

    @Test
    void acceptsTheClassicTangramSquareAndSmallSlack() {
        CheckResult exact = checker.check(Tangram.SILHOUETTE, Tangram.PIECES, Tangram.SOLUTION);
        assertThat(exact.solved()).isTrue();
        assertThat(exact.overlapRatio()).isCloseTo(0, within(1e-9));
        assertThat(exact.mismatchRatio()).isCloseTo(0, within(1e-9));

        assertThat(checker.check(Tangram.SILHOUETTE, Tangram.PIECES,
                replace(5, new Placement(2.01, 3.005, 45, false))).solved()).isTrue();
    }

    @Test
    void rejectsAMisplacedOrMissingPiece() {
        // The small triangle is 1/16 of the square: moved away it is missing once and outside once.
        CheckResult misplaced = checker.check(Tangram.SILHOUETTE, Tangram.PIECES,
                replace(3, new Placement(10, 10, 90, false)));
        assertThat(misplaced.solved()).isFalse();
        assertThat(misplaced.mismatchRatio()).isCloseTo(2.0 / 16, within(1e-9));

        List<Outline> pieces = new ArrayList<>(Tangram.PIECES);
        List<Placement> placements = new ArrayList<>(Tangram.SOLUTION);
        pieces.remove(4);
        placements.remove(4);
        CheckResult missing = checker.check(Tangram.SILHOUETTE, pieces, placements);
        assertThat(missing.solved()).isFalse();
        assertThat(missing.mismatchRatio()).isCloseTo(1.0 / 16, within(1e-9));
    }

    @Test
    void rejectsTheParallelogramWithoutFlipInAnyRotation() {
        for (int rotation = 0; rotation < 360; rotation += Placement.ROTATION_STEP) {
            assertThat(checker.check(Tangram.SILHOUETTE, Tangram.PIECES,
                    replace(6, new Placement(3, 1, rotation, false))).solved())
                    .as("rotation %d", rotation).isFalse();
        }
    }

    @Test
    void rejectsOverlappingPiecesEvenWhenTheyCoverTheSilhouette() {
        Outline square = Outline.of(0, 0, 2, 0, 2, 2, 0, 2);
        Placement origin = new Placement(0, 0, 0, false);

        CheckResult result = checker.check(List.of(square), List.of(square, square), List.of(origin, origin));

        assertThat(result.solved()).isFalse();
        assertThat(result.mismatchRatio()).isCloseTo(0, within(1e-9));
        assertThat(result.overlapRatio()).isCloseTo(1, within(1e-9));
    }

    @Test
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> checker.check(Tangram.SILHOUETTE, Tangram.PIECES, Tangram.SOLUTION.subList(0, 6)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> checker.check(List.of(), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        for (double tolerance : new double[] {-0.1, 1, Double.NaN}) {
            assertThatThrownBy(() -> new SolutionChecker(tolerance)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    private static List<Placement> replace(int index, Placement placement) {
        List<Placement> placements = new ArrayList<>(Tangram.SOLUTION);
        placements.set(index, placement);
        return placements;
    }
}

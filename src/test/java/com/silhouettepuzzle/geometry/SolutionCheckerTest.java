package com.silhouettepuzzle.geometry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class SolutionCheckerTest {

    private final SolutionChecker checker = new SolutionChecker();

    @Test
    void acceptsTheClassicTangramSquare() {
        CheckResult result = checker.check(Tangram.SILHOUETTE, Tangram.PIECES, Tangram.SOLUTION);

        assertThat(result.solved()).isTrue();
        assertThat(result.overlapRatio()).isCloseTo(0, within(1e-9));
        assertThat(result.mismatchRatio()).isCloseTo(0, within(1e-9));
    }

    @Test
    void acceptsSmallSlackWithinTolerance() {
        List<Placement> placements = new ArrayList<>(Tangram.SOLUTION);
        placements.set(5, new Placement(2.01, 3.005, 45, false));

        assertThat(checker.check(Tangram.SILHOUETTE, Tangram.PIECES, placements).solved()).isTrue();
    }

    @Test
    void rejectsAPieceMovedOutOfTheSilhouette() {
        List<Placement> placements = new ArrayList<>(Tangram.SOLUTION);
        placements.set(3, new Placement(10, 10, 90, false));

        CheckResult result = checker.check(Tangram.SILHOUETTE, Tangram.PIECES, placements);

        assertThat(result.solved()).isFalse();
        // The small triangle is 1/16 of the square: once uncovered, once outside.
        assertThat(result.mismatchRatio()).isCloseTo(2.0 / 16, within(1e-9));
    }

    @Test
    void rejectsASolutionWithAPieceMissing() {
        List<Outline> pieces = new ArrayList<>(Tangram.PIECES);
        List<Placement> placements = new ArrayList<>(Tangram.SOLUTION);
        pieces.remove(4);
        placements.remove(4);

        CheckResult result = checker.check(Tangram.SILHOUETTE, pieces, placements);

        assertThat(result.solved()).isFalse();
        assertThat(result.mismatchRatio()).isCloseTo(1.0 / 16, within(1e-9));
    }

    @Test
    void rejectsTheParallelogramWithoutFlipInAnyRotation() {
        for (int rotation = 0; rotation < 360; rotation += Placement.ROTATION_STEP) {
            List<Placement> placements = new ArrayList<>(Tangram.SOLUTION);
            placements.set(6, new Placement(3, 1, rotation, false));

            assertThat(checker.check(Tangram.SILHOUETTE, Tangram.PIECES, placements).solved())
                    .as("rotation %d", rotation)
                    .isFalse();
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
    void acceptsASilhouetteMadeOfSeveralParts() {
        Outline left = Outline.of(0, 0, 1, 0, 1, 1, 0, 1);
        Outline right = Outline.of(1, 0, 2, 0, 2, 1, 1, 1);
        Outline unit = Outline.of(0, 0, 1, 0, 1, 1, 0, 1);

        CheckResult result = checker.check(List.of(left, right), List.of(unit, unit),
                List.of(new Placement(0, 0, 0, false), new Placement(1, 0, 0, false)));

        assertThat(result.solved()).isTrue();
    }

    @Test
    void rejectsAWrongNumberOfPlacements() {
        assertThatThrownBy(() -> checker.check(Tangram.SILHOUETTE, Tangram.PIECES, Tangram.SOLUTION.subList(0, 6)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnEmptySilhouette() {
        assertThatThrownBy(() -> checker.check(List.of(), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAToleranceOutsideTheRange() {
        assertThatThrownBy(() -> new SolutionChecker(-0.1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SolutionChecker(1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SolutionChecker(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
    }
}

package com.silhouettepuzzle.geometry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

class PuzzleGeometryTest {

    private static final Outline BAR = Outline.of(0, 0, 2, 0, 2, 1, 0, 1);
    private static final Outline UNIT = Outline.of(0, 0, 1, 0, 1, 1, 0, 1);

    private final PuzzleGeometry geometry = new PuzzleGeometry(0.01);

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
        for (int angle = -45; angle <= 405; angle += 15) {
            int rotation = angle;
            if (rotation >= 0 && rotation < 360 && rotation % 45 == 0) {
                assertThat(new Placement(0, 0, rotation, false).rotation()).isEqualTo(rotation);
            } else {
                assertThatThrownBy(() -> new Placement(0, 0, rotation, false))
                        .as("rotation %d", rotation).isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test
    void acceptsTheClassicTangramSquareAndSmallSlack() {
        PuzzleGeometry.Check exact = geometry.check(Tangram.SILHOUETTE, Tangram.PIECES, Tangram.SOLUTION);
        assertThat(exact.solved()).isTrue();
        assertThat(exact.overlap()).isCloseTo(0, within(1e-9));
        assertThat(exact.mismatch()).isCloseTo(0, within(1e-9));

        assertThat(geometry.check(Tangram.SILHOUETTE, Tangram.PIECES,
                replace(5, new Placement(2.01, 3.005, 45, false))).solved()).isTrue();
    }

    @Test
    void rejectsAMisplacedOrMissingPieceAndTheUnflippedParallelogram() {
        // The small triangle is 1/16 of the square: moved away it is missing once and outside once.
        PuzzleGeometry.Check misplaced = geometry.check(Tangram.SILHOUETTE, Tangram.PIECES,
                replace(3, new Placement(10, 10, 90, false)));
        assertThat(misplaced.solved()).isFalse();
        assertThat(misplaced.mismatch()).isCloseTo(2.0 / 16, within(1e-9));

        List<Outline> pieces = new ArrayList<>(Tangram.PIECES);
        List<Placement> placements = new ArrayList<>(Tangram.SOLUTION);
        pieces.remove(4);
        placements.remove(4);
        assertThat(geometry.check(Tangram.SILHOUETTE, pieces, placements).mismatch())
                .isCloseTo(1.0 / 16, within(1e-9));

        for (int rotation = 0; rotation < 360; rotation += Placement.ROTATION_STEP) {
            assertThat(geometry.check(Tangram.SILHOUETTE, Tangram.PIECES,
                    replace(6, new Placement(3, 1, rotation, false))).solved())
                    .as("parallelogram unflipped at %d", rotation).isFalse();
        }
    }

    @Test
    void rejectsOverlappingPiecesEvenWhenTheyCoverTheSilhouette() {
        Outline square = Outline.of(0, 0, 2, 0, 2, 2, 0, 2);

        PuzzleGeometry.Check result = geometry.check(List.of(square), List.of(square, square),
                List.of(at(0, 0), at(0, 0)));

        assertThat(result.solved()).isFalse();
        assertThat(result.mismatch()).isCloseTo(0, within(1e-9));
        assertThat(result.overlap()).isCloseTo(1, within(1e-9));
    }

    @Test
    void assemblesSilhouettesAndRejectsOverlapsAndHoles() {
        // Vertices where pieces meet along a straight edge are dropped and
        // float noise from the 45-degree rotations is rounded away.
        assertThat(PuzzleGeometry.assemble(Tangram.PIECES, Tangram.SOLUTION)).singleElement()
                .satisfies(square -> assertThat(square.vertices()).containsExactlyInAnyOrder(
                        new Point(0, 0), new Point(4, 0), new Point(4, 4), new Point(0, 4)));
        // Pieces touching only at a corner stay separate parts.
        assertThat(PuzzleGeometry.assemble(List.of(UNIT, UNIT), List.of(at(0, 0), at(1, 1)))).hasSize(2);

        assertThatThrownBy(() -> PuzzleGeometry.assemble(List.of(UNIT, UNIT), List.of(at(0, 0), at(0.5, 0))))
                .hasMessageContaining("overlap");
        // A 3 x 3 frame: bars on top and bottom, unit squares left and right in the middle row.
        Outline frame = Outline.of(0, 0, 3, 0, 3, 1, 0, 1);
        assertThatThrownBy(() -> PuzzleGeometry.assemble(List.of(frame, frame, UNIT, UNIT),
                List.of(at(0, 0), at(0, 2), at(0, 1), at(2, 1))))
                .hasMessageContaining("hole");
    }

    @Test
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> geometry.check(Tangram.SILHOUETTE, Tangram.PIECES, Tangram.SOLUTION.subList(0, 6)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> geometry.check(List.of(), List.of(UNIT), List.of(at(0, 0))))
                .isInstanceOf(IllegalArgumentException.class);
        for (double tolerance : new double[] {-0.1, 1, Double.NaN}) {
            assertThatThrownBy(() -> new PuzzleGeometry(tolerance)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    private static void assertPlaced(Outline piece, Placement placement, double... expected) {
        Geometry placed = PuzzleGeometry.place(piece, placement);
        assertThat(placed.equalsExact(PuzzleGeometry.toPolygon(Outline.of(expected).vertices()), 1e-9))
                .as("%s", placement).isTrue();
    }

    private static List<Placement> replace(int index, Placement placement) {
        List<Placement> placements = new ArrayList<>(Tangram.SOLUTION);
        placements.set(index, placement);
        return placements;
    }

    private static Placement at(double x, double y) {
        return new Placement(x, y, 0, false);
    }
}

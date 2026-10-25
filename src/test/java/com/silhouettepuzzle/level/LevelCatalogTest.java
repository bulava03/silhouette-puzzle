package com.silhouettepuzzle.level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import com.silhouettepuzzle.geometry.Point;
import com.silhouettepuzzle.geometry.SolutionChecker;

import tools.jackson.databind.json.JsonMapper;

class LevelCatalogTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    private final List<Level> levels = LevelCatalog.read(new ClassPathResource(LevelSeeder.CATALOG), mapper);

    @Test
    void readsTheBuiltInPicturesInOrder() {
        assertThat(levels).extracting(Level::id).containsExactly("bus", "house", "rocket");
        assertThat(levels).extracting(Level::number).containsExactly(1, 2, 3);
        assertThat(levels).extracting(level -> level.pieces().size()).containsExactly(18, 10, 10);
    }

    @Test
    void everyPictureIsOneSolidFigure() {
        assertThat(levels).allSatisfy(level -> {
            double partsArea = level.pieces().stream().mapToDouble(piece -> piece.outline().area()).sum();
            assertThat(level.silhouette()).singleElement()
                    .satisfies(part -> assertThat(part.area()).isCloseTo(partsArea, within(1e-6)));
        });
    }

    @Test
    void everyReferenceSolutionPassesTheChecker() {
        SolutionChecker checker = new SolutionChecker();

        assertThat(levels).allSatisfy(level -> assertThat(
                checker.check(level.silhouette(), level.pieceOutlines(), level.solution()).solved()).isTrue());
    }

    @Test
    void buildsTheBusAtItsSize() {
        List<Point> outline = level("bus").silhouette().getFirst().vertices();

        assertThat(outline).extracting(Point::x).allSatisfy(x -> assertThat(x).isBetween(0.0, 10.0));
        assertThat(outline).extracting(Point::y).allSatisfy(y -> assertThat(y).isBetween(0.0, 6.2));
        assertThat(outline).contains(new Point(0, 0), new Point(10, 1), new Point(8.4, 6.2));
    }

    @Test
    void piecesAreCentredOnTheirOwnOrigin() {
        assertThat(levels).flatExtracting(Level::pieces).allSatisfy(piece -> {
            List<Point> vertices = piece.outline().vertices();
            assertThat(vertices.stream().mapToDouble(Point::x).average().orElseThrow()).isCloseTo(0, within(0.01));
            assertThat(vertices.stream().mapToDouble(Point::y).average().orElseThrow()).isCloseTo(0, within(0.01));
        });
    }

    @Test
    void mirroredPartsArriveAsTheMirrorImageAndMustBeFlipped() {
        Level rocket = level("rocket");

        // Left and right hull halves, then left and right fins: the player gets
        // two copies of the same shape and has to flip one of them into place.
        assertThat(rocket.pieces().get(2).outline().vertices())
                .containsExactlyInAnyOrderElementsOf(rocket.pieces().get(1).outline().vertices());
        assertThat(rocket.pieces().get(8).outline().vertices())
                .containsExactlyInAnyOrderElementsOf(rocket.pieces().get(7).outline().vertices());
        assertThat(rocket.solution().get(1).flipped()).isFalse();
        assertThat(rocket.solution().get(2).flipped()).isTrue();
        assertThat(rocket.solution().get(8).flipped()).isTrue();
    }

    @Test
    void piecesCarryTheirNameColourAndStartRotation() {
        Piece wheel = level("bus").pieces().get(16);

        assertThat(wheel.name()).isEqualTo("Wheel");
        assertThat(wheel.color()).isEqualTo("#737A8F");
        assertThat(wheel.startRotation()).isEqualTo(45);
    }

    @Test
    void rejectsPartsThatOverlap() {
        String json = """
                {
                    "colors": {"grey": "#808080"},
                    "levels": [{"id": "broken", "name": "Broken", "number": 1, "parts": [
                        {"name": "A", "color": "grey", "outline": [[0, 0], [2, 0], [2, 2], [0, 2]], "startRotation": 0},
                        {"name": "B", "color": "grey", "outline": [[1, 0], [3, 0], [3, 2], [1, 2]], "startRotation": 0}
                    ]}]
                }
                """;

        assertThatThrownBy(() -> read(json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("broken")
                .hasMessageContaining("overlap");
    }

    @Test
    void rejectsAnUnknownColour() {
        String json = """
                {"colors": {}, "levels": [{"id": "x", "name": "X", "number": 1, "parts": [
                    {"name": "A", "color": "teal", "outline": [[0, 0], [1, 0], [1, 1]], "startRotation": 0}]}]}
                """;

        assertThatThrownBy(() -> read(json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("teal");
    }

    @Test
    void rejectsAStartRotationOffTheStep() {
        String json = """
                {"colors": {"grey": "#808080"}, "levels": [{"id": "x", "name": "X", "number": 1, "parts": [
                    {"name": "A", "color": "grey", "outline": [[0, 0], [1, 0], [1, 1]], "startRotation": 30}]}]}
                """;

        assertThatThrownBy(() -> read(json)).isInstanceOf(IllegalArgumentException.class);
    }

    private List<Level> read(String json) {
        return LevelCatalog.read(new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)), mapper);
    }

    private Level level(String id) {
        return levels.stream().filter(level -> level.id().equals(id)).findFirst().orElseThrow();
    }
}

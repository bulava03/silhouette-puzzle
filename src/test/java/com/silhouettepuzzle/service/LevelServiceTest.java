package com.silhouettepuzzle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import com.silhouettepuzzle.entity.Level;
import com.silhouettepuzzle.entity.Piece;
import com.silhouettepuzzle.geometry.Point;
import com.silhouettepuzzle.geometry.PuzzleGeometry;

import tools.jackson.databind.json.JsonMapper;

class LevelServiceTest {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final List<Level> levels = LevelService.read(new ClassPathResource(LevelService.CATALOG), mapper);

    @Test
    void readsThePictures() {
        assertThat(levels).extracting(Level::id).containsExactly(
                "bus", "house", "rocket", "sailboat", "cat", "lighthouse", "car", "castle", "locomotive");
        assertThat(levels).extracting(Level::number).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertThat(levels).extracting(level -> level.pieces().size()).containsExactly(18, 10, 10, 9, 12, 11, 13, 13, 15);
        assertThat(level("bus").pieces().get(16)).isEqualTo(new Piece("Wheel", "#737A8F",
                level("bus").pieces().get(16).outline(), 45));
    }

    @Test
    void everyPictureIsOneSolvableFigureOfCentredParts() {
        PuzzleGeometry checker = new PuzzleGeometry(0.01);
        assertThat(levels).allSatisfy(level -> {
            double partsArea = level.pieces().stream().mapToDouble(piece -> piece.outline().area()).sum();
            assertThat(level.silhouette()).singleElement()
                    .satisfies(part -> assertThat(part.area()).isCloseTo(partsArea, within(1e-6)));
            assertThat(checker.check(level.silhouette(), level.pieceOutlines(), level.solution()).solved()).isTrue();
            assertThat(level.pieces()).allSatisfy(piece -> {
                List<Point> vertices = piece.outline().vertices();
                assertThat(vertices.stream().mapToDouble(Point::x).average().orElseThrow()).isCloseTo(0, within(0.01));
                assertThat(vertices.stream().mapToDouble(Point::y).average().orElseThrow()).isCloseTo(0, within(0.01));
            });
        });
    }

    @Test
    void mirroredPartsArriveAsTheirPairsShapeAndMustBeFlipped() {
        Level rocket = level("rocket");
        // Hull halves 1 and 2, fins 7 and 8: two copies of one shape, one flipped into place.
        for (int[] pair : new int[][] {{1, 2}, {7, 8}}) {
            assertThat(rocket.pieces().get(pair[1]).outline().vertices())
                    .containsExactlyInAnyOrderElementsOf(rocket.pieces().get(pair[0]).outline().vertices());
            assertThat(rocket.solution().get(pair[0]).flipped()).isFalse();
            assertThat(rocket.solution().get(pair[1]).flipped()).isTrue();
        }
    }

    @Test
    void rejectsBrokenCatalogs() {
        String overlapping = """
                {"colors": {"grey": "#808080"}, "levels": [{"id": "broken", "name": "B", "number": 1, "parts": [
                    {"name": "A", "color": "grey", "outline": [[0, 0], [2, 0], [2, 2], [0, 2]], "startRotation": 0},
                    {"name": "B", "color": "grey", "outline": [[1, 0], [3, 0], [3, 2], [1, 2]], "startRotation": 0}]}]}
                """;
        assertThatThrownBy(() -> read(overlapping)).hasMessageContaining("broken").hasMessageContaining("overlap");
        assertThatThrownBy(() -> read(onePart("teal", 0))).hasMessageContaining("teal");
        assertThatThrownBy(() -> read(onePart("grey", 30))).hasMessageContaining("whole step");
    }

    private static String onePart(String color, int startRotation) {
        return """
                {"colors": {"grey": "#808080"}, "levels": [{"id": "x", "name": "X", "number": 1, "parts": [
                    {"name": "A", "color": "%s", "outline": [[0, 0], [1, 0], [1, 1]], "startRotation": %d}]}]}
                """.formatted(color, startRotation);
    }

    private List<Level> read(String json) {
        return LevelService.read(new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)), mapper);
    }

    private Level level(String id) {
        return levels.stream().filter(level -> level.id().equals(id)).findFirst().orElseThrow();
    }
}

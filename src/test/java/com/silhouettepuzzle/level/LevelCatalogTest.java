package com.silhouettepuzzle.level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import com.silhouettepuzzle.geometry.Outline;
import com.silhouettepuzzle.geometry.Point;
import com.silhouettepuzzle.geometry.SolutionChecker;

import tools.jackson.databind.json.JsonMapper;

class LevelCatalogTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    private final List<Level> levels = LevelCatalog.read(new ClassPathResource(LevelSeeder.CATALOG), mapper);

    @Test
    void readsTheBuiltInLevelsInOrder() {
        assertThat(levels).extracting(Level::id).containsExactly("square", "triangle", "parallelogram");
        assertThat(levels).extracting(Level::number).containsExactly(1, 2, 3);
    }

    @Test
    void everyLevelIsOneSolidFigureMadeOfAllSevenPieces() {
        assertThat(levels).allSatisfy(level -> {
            assertThat(level.pieces()).hasSize(7);
            assertThat(level.silhouette()).singleElement()
                    .satisfies(part -> assertThat(part.area()).isCloseTo(16, within(1e-9)));
        });
    }

    @Test
    void everyReferenceSolutionPassesTheChecker() {
        SolutionChecker checker = new SolutionChecker();

        assertThat(levels).allSatisfy(level ->
                assertThat(checker.check(level.silhouette(), level.pieces(), level.solution()).solved()).isTrue());
    }

    @Test
    void buildsTheExpectedFigures() {
        assertThat(silhouetteOf("square")).containsExactlyInAnyOrder(
                new Point(0, 0), new Point(4, 0), new Point(4, 4), new Point(0, 4));
        assertThat(silhouetteOf("triangle")).containsExactlyInAnyOrder(
                new Point(0, 0), new Point(8, 0), new Point(4, 4));
        assertThat(silhouetteOf("parallelogram")).containsExactlyInAnyOrder(
                new Point(4, 0), new Point(8, 0), new Point(4, 4), new Point(0, 4));
    }

    @Test
    void rejectsALevelWhosePiecesOverlap() {
        String json = """
                {
                    "pieceSets": {"pair": [[[0, 0], [1, 0], [1, 1], [0, 1]], [[0, 0], [1, 0], [1, 1], [0, 1]]]},
                    "levels": [{"id": "broken", "name": "Broken", "number": 1, "pieceSet": "pair",
                                "solution": [{"x": 0, "y": 0, "rotation": 0, "flipped": false},
                                             {"x": 0, "y": 0, "rotation": 0, "flipped": false}]}]
                }
                """;

        assertThatThrownBy(() -> LevelCatalog.read(new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)), mapper))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("broken");
    }

    @Test
    void rejectsAnUnknownPieceSet() {
        String json = """
                {"pieceSets": {}, "levels": [{"id": "x", "name": "X", "number": 1, "pieceSet": "none", "solution": []}]}
                """;

        assertThatThrownBy(() -> LevelCatalog.read(new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)), mapper))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("none");
    }

    private List<Point> silhouetteOf(String id) {
        Outline outline = levels.stream().filter(level -> level.id().equals(id)).findFirst().orElseThrow()
                .silhouette().getFirst();
        return outline.vertices();
    }
}

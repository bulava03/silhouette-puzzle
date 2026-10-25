package com.silhouettepuzzle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.silhouettepuzzle.level.LevelRepository;
import com.silhouettepuzzle.level.LevelSeeder;

/**
 * Starts the whole application against a real MongoDB in a container.
 */
@SpringBootTest
@Import(IntegrationTestConfig.class)
class SilhouettePuzzleApplicationTests {

    @Autowired
    private LevelRepository levels;

    @Autowired
    private LevelSeeder seeder;

    @Test
    void seedsTheBuiltInLevelsOnStartup() {
        assertThat(levels.findAllByOrderByNumberAsc())
                .extracting("id")
                .containsExactly("bus", "house", "rocket");
    }

    @Test
    void seedingAgainAddsNothing() {
        long before = levels.count();

        seeder.run(new DefaultApplicationArguments());

        assertThat(levels.count()).isEqualTo(before);
    }

    @Test
    void storedLevelsReadBackWithTheirGeometry() {
        assertThat(levels.findById("bus")).hasValueSatisfying(level -> {
            assertThat(level.pieces()).hasSize(18);
            assertThat(level.solution()).hasSize(18);
            assertThat(level.silhouette()).singleElement()
                    .satisfies(part -> assertThat(part.area()).isCloseTo(52.38, within(1e-6)));
        });
    }
}

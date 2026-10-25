package com.silhouettepuzzle;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Checks that the application context starts. The MongoDB client connects
 * lazily, so no database is needed here.
 */
@SpringBootTest
class SilhouettePuzzleApplicationTests {

    @Test
    void contextLoads() {
    }
}

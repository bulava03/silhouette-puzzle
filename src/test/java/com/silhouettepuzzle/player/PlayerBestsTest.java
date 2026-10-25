package com.silhouettepuzzle.player;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.silhouettepuzzle.IntegrationTestConfig;

/**
 * Recording solved runs into a player's bests, against a real MongoDB.
 */
@SpringBootTest
@Import(IntegrationTestConfig.class)
class PlayerBestsTest {

    private static final Instant AT = Instant.parse("2026-01-01T12:00:00Z");

    @Autowired
    private PlayerBests bests;

    @Autowired
    private PlayerRepository players;

    @BeforeEach
    void clearPlayers() {
        players.deleteAll();
    }

    @Test
    void keepsOneBestRunPerLevelWithTheCount() {
        // A player stored before there were bests has none at all, not an empty list.
        String id = players.save(new Player(null, "alice", "alice", "{noop}x", AT, null)).id();

        bests.record(id, "bus", 30_000, 20, AT);
        bests.record(id, "bus", 25_000, 30, AT.plusSeconds(60));
        bests.record(id, "bus", 50_000, 5, AT.plusSeconds(120));
        bests.record(id, "house", 40_000, 9, AT);

        assertThat(players.findById(id).orElseThrow().bestsOrEmpty()).containsExactlyInAnyOrder(
                new PlayerBest("bus", 25_000, 30, AT.plusSeconds(60), 3),
                new PlayerBest("house", 40_000, 9, AT, 1));
    }

    @Test
    void concurrentFirstSolvesOfALevelAddItOnceAndCountEverySolve() throws Exception {
        String id = players.save(new Player(null, "alice", "alice", "{noop}x", AT, List.of())).id();
        int threads = 8;
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> runs = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                long timeMs = 10_000 + i * 1_000L;
                runs.add(pool.submit(() -> {
                    go.await();
                    bests.record(id, "bus", timeMs, 7, AT);
                    return null;
                }));
            }
            go.countDown();
            for (Future<?> run : runs) {
                run.get();
            }
        } finally {
            pool.shutdown();
        }

        assertThat(players.findById(id).orElseThrow().bestsOrEmpty()).singleElement().satisfies(best -> {
            assertThat(best.solves()).isEqualTo(threads);
            assertThat(best.timeMs()).isEqualTo(10_000);
        });
    }
}

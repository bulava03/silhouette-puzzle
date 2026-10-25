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

    private String playerId;

    @BeforeEach
    void freshPlayer() {
        players.deleteAll();
        playerId = players.save(new Player(null, "alice", "alice", "{noop}x", AT, List.of())).id();
    }

    @Test
    void keepsOneEntryPerLevelWithTheBestRunAndTheCount() {
        bests.record(playerId, "bus", 30_000, 20, AT);
        bests.record(playerId, "bus", 25_000, 30, AT.plusSeconds(60));
        bests.record(playerId, "bus", 50_000, 5, AT.plusSeconds(120));
        bests.record(playerId, "house", 40_000, 9, AT);

        assertThat(stored()).containsExactlyInAnyOrder(
                new PlayerBest("bus", 25_000, 30, AT.plusSeconds(60), 3),
                new PlayerBest("house", 40_000, 9, AT, 1));
    }

    @Test
    void worksForPlayersStoredBeforeThereWereBests() {
        String legacy = players.save(new Player(null, "bob", "bob", "{noop}x", AT, null)).id();

        bests.record(legacy, "bus", 30_000, 20, AT);

        assertThat(players.findById(legacy).orElseThrow().bestsOrEmpty())
                .containsExactly(new PlayerBest("bus", 30_000, 20, AT, 1));
    }

    @Test
    void concurrentFirstSolvesOfALevelAddItOnceAndCountEverySolve() throws Exception {
        int threads = 8;
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> runs = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                long timeMs = 10_000 + i * 1_000L;
                runs.add(pool.submit(() -> {
                    go.await();
                    bests.record(playerId, "bus", timeMs, 7, AT);
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

        assertThat(stored()).singleElement().satisfies(best -> {
            assertThat(best.solves()).isEqualTo(threads);
            assertThat(best.timeMs()).isEqualTo(10_000);
        });
    }

    private List<PlayerBest> stored() {
        return players.findById(playerId).orElseThrow().bestsOrEmpty();
    }
}

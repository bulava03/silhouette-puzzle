package com.silhouettepuzzle.service;

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

import com.silhouettepuzzle.IntegrationTest;
import com.silhouettepuzzle.entity.Attempt;
import com.silhouettepuzzle.entity.Player;
import com.silhouettepuzzle.repository.PlayerRepository;

/**
 * Players' bests and the leaderboards built from them, against a real MongoDB.
 */
@IntegrationTest
class PlayerServiceTest {

    private static final Instant AT = Instant.parse("2026-01-01T12:00:00Z");

    @Autowired
    private PlayerService service;

    @Autowired
    private PlayerRepository players;

    @BeforeEach
    void clearPlayers() {
        players.deleteAll();
    }

    @Test
    void keepsTheBestSolvedAttemptPerLevel() {
        // A player stored before there were bests has none at all, not an empty list.
        String alice = player("alice", null);

        service.recordBest(alice, solved("bus", 30_000, 0));
        service.recordBest(alice, solved("bus", 25_000, 60));   // faster: the new best
        service.recordBest(alice, solved("bus", 25_000, 120));  // as fast but later: not kept
        service.recordBest(alice, solved("bus", 50_000, 180));  // slower: not kept
        service.recordBest(alice, solved("house", 40_000, 0));

        Player stored = players.findById(alice).orElseThrow();
        assertThat(stored.bests()).hasSize(2);
        assertThat(stored.bestOn("bus")).hasValueSatisfying(best -> {
            assertThat(best.timeMs()).isEqualTo(25_000);
            assertThat(best.solvedAt()).isEqualTo(AT.plusSeconds(60));
            assertThat(best.playerId()).isNull();   // the owner is the document it lives in
        });
    }

    @Test
    void concurrentFirstSolvesOfALevelAddItOnceAndKeepTheFastest() throws Exception {
        String alice = player("alice", List.of());
        int threads = 8;
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> runs = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                long timeMs = 10_000 + i * 1_000L;
                runs.add(pool.submit(() -> {
                    go.await();
                    service.recordBest(alice, solved("bus", timeMs, 0));
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

        assertThat(players.findById(alice).orElseThrow().bests()).singleElement()
                .satisfies(best -> assertThat(best.timeMs()).isEqualTo(10_000));
    }

    @Test
    void ranksPlayersByTimeThenByWhoWasFirst() {
        String alice = player("alice", List.of());
        String bob = player("bob", List.of());
        String carol = player("carol", List.of());
        service.recordBest(alice, solved("bus", 25_000, 60));
        service.recordBest(bob, solved("bus", 25_000, 0));
        service.recordBest(carol, solved("bus", 40_000, 0));
        service.recordBest(carol, solved("house", 1_000, 0));

        assertThat(service.leaderboard("bus", 10)).extracting(Player::username).containsExactly("bob", "alice", "carol");
        assertThat(service.leaderboard("bus", 10).getFirst().bests()).singleElement()
                .satisfies(best -> assertThat(best.levelId()).isEqualTo("bus"));
        assertThat(service.leaderboard("bus", 1)).hasSize(1);
        assertThat(service.rankOf(players.findById(alice).orElseThrow().bestOn("bus").orElseThrow())).isEqualTo(2);
    }

    private String player(String name, List<Attempt> bests) {
        return players.save(new Player(null, name, name, "{noop}x", AT, bests)).id();
    }

    private static Attempt solved(String levelId, long timeMs, int solvedAfterSeconds) {
        Instant solvedAt = AT.plusSeconds(solvedAfterSeconds);
        return new Attempt(null, levelId, "someone", solvedAt.minusMillis(timeMs), solvedAt, timeMs);
    }
}

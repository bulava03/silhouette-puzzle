package com.silhouettepuzzle.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.silhouettepuzzle.ApiException;
import com.silhouettepuzzle.IntegrationTestConfig;
import com.silhouettepuzzle.dto.SubmitRequest;
import com.silhouettepuzzle.entity.Level;
import com.silhouettepuzzle.entity.Player;
import com.silhouettepuzzle.geometry.Placement;
import com.silhouettepuzzle.repository.PlayerRepository;
import com.silhouettepuzzle.security.PlayerPrincipal;
import com.silhouettepuzzle.service.AttemptService;
import com.silhouettepuzzle.service.LevelService;

import tools.jackson.databind.json.JsonMapper;

/**
 * Checking pictures over HTTP, as guests and as logged-in players, against a real MongoDB.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestConfig.class)
class GameFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private LevelService levels;

    @Autowired
    private AttemptService attempts;

    @Autowired
    private PlayerRepository players;

    @Autowired
    private IntegrationTestConfig.MutableClock clock;

    private PlayerPrincipal alice;

    @BeforeEach
    void freshPlayer() {
        players.deleteAll();
        alice = principal("alice");
    }

    @Test
    void guestsGetAVerdictButNoTime() throws Exception {
        List<Placement> wrong = new ArrayList<>(solutionOf("bus"));
        wrong.set(0, new Placement(20, 20, 0, false));

        check("bus", null, wrong, null)
                .andExpect(jsonPath("$.solved").value(false))
                .andExpect(jsonPath("$.off", greaterThan(0.1), Double.class));
        check("bus", null, solutionOf("bus"), null)
                .andExpect(jsonPath("$.solved").value(true))
                .andExpect(jsonPath("$.timeMs").doesNotExist())
                .andExpect(jsonPath("$.rank").doesNotExist());
    }

    @Test
    void aPlayersSolvedAttemptIsTimedRankedAndClosed() throws Exception {
        String attemptId = start("bus", alice);
        clock.advance(Duration.ofSeconds(42));

        check("bus", attemptId, solutionOf("bus"), alice)
                .andExpect(jsonPath("$.solved").value(true))
                .andExpect(jsonPath("$.timeMs").value(42_000))
                .andExpect(jsonPath("$.rank").value(1));
        check("bus", attemptId, solutionOf("bus"), alice).andExpect(status().isConflict());
        assertThat(players.findById(alice.getId()).orElseThrow().bestOn("bus"))
                .hasValueSatisfying(best -> assertThat(best.timeMs()).isEqualTo(42_000));
    }

    @Test
    void concurrentCorrectChecksOfOneAttemptCountOnce() throws Exception {
        String attemptId = start("house", alice);
        List<Placement> solution = solutionOf("house");
        int threads = 8;
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        // A ticking clock makes every update write different values, so a
        // missing guard cannot hide behind an update that changes nothing.
        clock.tickOnEveryRead(true);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Callable<Boolean> task = () -> {
                    go.await();
                    try {
                        return attempts.submit("house", new SubmitRequest(attemptId, solution), alice).solved();
                    } catch (ApiException e) {
                        return false;
                    }
                };
                results.add(pool.submit(task));
            }
            go.countDown();
            int accepted = 0;
            for (Future<Boolean> result : results) {
                accepted += result.get() ? 1 : 0;
            }
            assertThat(accepted).isEqualTo(1);
        } finally {
            clock.tickOnEveryRead(false);
            pool.shutdown();
        }
    }

    @Test
    void rejectsWhatCannotCount() throws Exception {
        String attemptId = start("bus", alice);

        check("nope", null, solutionOf("bus"), null).andExpect(status().isNotFound());
        check("bus", "000000000000000000000000", solutionOf("bus"), alice).andExpect(status().isNotFound());
        check("bus", attemptId, solutionOf("bus"), principal("bob")).andExpect(status().isNotFound());
        check("house", attemptId, solutionOf("house"), alice).andExpect(status().isNotFound());
        check("bus", attemptId, solutionOf("bus").subList(0, 6), alice)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Level bus has 18 pieces, got 6 placements"));
        mvc.perform(post("/api/levels/bus/check").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placements\": [{\"x\": 0, \"y\": 0, \"rotation\": 30, \"flipped\": false}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/levels/bus/check").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new SubmitRequest(null, solutionOf("bus")))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/levels")).andExpect(status().isUnauthorized());
    }

    @Test
    void attemptsExpireAfterADay() throws Exception {
        String inTime = start("bus", alice);
        String late = start("bus", alice);
        clock.advance(Duration.ofHours(24));
        check("bus", inTime, solutionOf("bus"), alice).andExpect(jsonPath("$.solved").value(true));
        clock.advance(Duration.ofSeconds(1));

        check("bus", late, solutionOf("bus"), alice)
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.detail").value("Attempt " + late + " has expired; start the level again"));
    }

    private ResultActions check(String levelId, String attemptId, List<Placement> placements,
                                PlayerPrincipal player) throws Exception {
        var request = post("/api/levels/{id}/check", levelId).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new SubmitRequest(attemptId, placements)));
        return mvc.perform(player == null ? request : request.with(user(player)));
    }

    private String start(String levelId, PlayerPrincipal player) {
        return attempts.start(levels.get(levelId), player).id();
    }

    private List<Placement> solutionOf(String levelId) {
        Level level = levels.get(levelId);
        return level.solution();
    }

    private PlayerPrincipal principal(String name) {
        return new PlayerPrincipal(players.save(new Player(null, name, name, "{noop}x", Instant.now(), List.of())));
    }
}

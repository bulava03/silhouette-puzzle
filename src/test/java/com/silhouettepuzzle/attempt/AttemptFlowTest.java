package com.silhouettepuzzle.attempt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import com.silhouettepuzzle.geometry.Placement;
import com.silhouettepuzzle.level.LevelRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Levels and the attempt flow over HTTP, against a real MongoDB.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestConfig.class)
class AttemptFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private LevelRepository levels;

    @Autowired
    private AttemptService service;

    @Autowired
    private AttemptRepository attempts;

    @Autowired
    private IntegrationTestConfig.MutableClock clock;

    @Test
    void levelsAreServedWithoutTheirSolution() throws Exception {
        mvc.perform(get("/api/levels"))
                .andExpect(jsonPath("$[*].id").value(contains("bus", "house", "rocket")));
        mvc.perform(get("/api/levels/bus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pieces.length()").value(18))
                .andExpect(jsonPath("$.pieces[16].color").value("#737A8F"))
                .andExpect(jsonPath("$.pieces[16].startRotation").value(45))
                .andExpect(jsonPath("$.silhouette[0].vertices").isArray())
                .andExpect(jsonPath("$.solution").doesNotExist());
        mvc.perform(get("/api/levels/nope"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Level not found: nope"));
    }

    @Test
    void solvingRecordsServerTimeAndClosesTheAttempt() throws Exception {
        String attemptId = start("bus");
        List<Placement> wrong = new ArrayList<>(solutionOf("bus"));
        wrong.set(0, new Placement(20, 20, 0, false));

        submit(attemptId, wrong, 3)
                .andExpect(jsonPath("$.solved").value(false))
                .andExpect(jsonPath("$.mismatchRatio").value(greaterThan(0.1)))
                .andExpect(jsonPath("$.timeMs").doesNotExist());
        clock.advance(Duration.ofSeconds(42));
        submit(attemptId, solutionOf("bus"), 17)
                .andExpect(jsonPath("$.solved").value(true))
                .andExpect(jsonPath("$.timeMs").value(42_000))
                .andExpect(jsonPath("$.moves").value(17));
        submit(attemptId, solutionOf("bus"), 1).andExpect(status().isConflict());
    }

    @Test
    void concurrentCorrectSubmissionsAreRecordedOnce() throws Exception {
        String attemptId = start("house");
        List<Placement> solution = solutionOf("house");
        int threads = 8;
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                // Distinct move counts: a repeated update with identical values
                // would not count as a modification and hide a missing guard.
                int moves = i + 1;
                Callable<Integer> task = () -> {
                    go.await();
                    try {
                        return service.submit(attemptId, null, solution, moves).moves();
                    } catch (ApiException e) {
                        return null;
                    }
                };
                results.add(pool.submit(task));
            }
            go.countDown();
            List<Integer> accepted = new ArrayList<>();
            for (Future<Integer> result : results) {
                if (result.get() != null) {
                    accepted.add(result.get());
                }
            }
            assertThat(accepted).hasSize(1);
            assertThat(attempts.findById(attemptId).orElseThrow().moves()).isEqualTo(accepted.getFirst());
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void rejectsInvalidRequests() throws Exception {
        mvc.perform(post("/api/levels/nope/attempts").with(csrf())).andExpect(status().isNotFound());
        submit("000000000000000000000000", solutionOf("bus"), 1).andExpect(status().isNotFound());

        String attemptId = start("bus");
        submit(attemptId, solutionOf("bus").subList(0, 6), 1)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Level bus has 18 pieces, got 6 placements"));
        submit(attemptId, solutionOf("bus"), -1).andExpect(status().isBadRequest());
        mvc.perform(post("/api/attempts/{id}/submit", attemptId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placements\": [{\"x\": 0, \"y\": 0, \"rotation\": 30, \"flipped\": false}],"
                                + " \"moves\": 1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attemptsExpireAfterADay() throws Exception {
        String inTime = start("bus");
        String late = start("bus");
        clock.advance(Duration.ofHours(24));
        submit(inTime, solutionOf("bus"), 9).andExpect(jsonPath("$.solved").value(true));
        clock.advance(Duration.ofSeconds(1));
        submit(late, solutionOf("bus"), 9)
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.detail").value("Attempt " + late + " has expired; start the level again"));
    }

    private String start(String levelId) throws Exception {
        String body = mvc.perform(post("/api/levels/{id}/attempts", levelId).with(csrf()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asString();
    }

    private ResultActions submit(String attemptId, List<Placement> placements, int moves) throws Exception {
        return mvc.perform(post("/api/attempts/{id}/submit", attemptId).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new SubmitRequest(placements, moves))));
    }

    private List<Placement> solutionOf(String levelId) {
        return levels.findById(levelId).orElseThrow().solution();
    }
}

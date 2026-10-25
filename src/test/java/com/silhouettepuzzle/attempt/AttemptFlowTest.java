package com.silhouettepuzzle.attempt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.silhouettepuzzle.IntegrationTestConfig;
import com.silhouettepuzzle.geometry.Placement;
import com.silhouettepuzzle.level.LevelRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * The attempt flow over HTTP, against a real MongoDB.
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

    @Autowired
    private MongoTemplate mongo;

    @Test
    void solvingALevelRecordsServerTimeAndMoves() throws Exception {
        String attemptId = start("bus");
        clock.advance(Duration.ofSeconds(42));

        submit(attemptId, solutionOf("bus"), 17)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(true))
                .andExpect(jsonPath("$.timeMs").value(42_000))
                .andExpect(jsonPath("$.moves").value(17));
    }

    @Test
    void aWrongSolutionKeepsTheAttemptOpen() throws Exception {
        String attemptId = start("bus");
        List<Placement> wrong = new ArrayList<>(solutionOf("bus"));
        wrong.set(0, new Placement(20, 20, 0, false));

        submit(attemptId, wrong, 3)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(false))
                .andExpect(jsonPath("$.mismatchRatio").value(greaterThan(0.1)))
                .andExpect(jsonPath("$.timeMs").doesNotExist());

        submit(attemptId, solutionOf("bus"), 5)
                .andExpect(jsonPath("$.solved").value(true));
    }

    @Test
    void aSolvedAttemptAcceptsNoMoreSubmissions() throws Exception {
        String attemptId = start("bus");
        submit(attemptId, solutionOf("bus"), 7).andExpect(status().isOk());

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
                    } catch (AttemptAlreadySolvedException e) {
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
    void startingAnUnknownLevelIsNotFound() throws Exception {
        mvc.perform(post("/api/levels/nope/attempts").with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void submittingToAnUnknownAttemptIsNotFound() throws Exception {
        submit("000000000000000000000000", solutionOf("bus"), 1).andExpect(status().isNotFound());
    }

    @Test
    void aWrongNumberOfPlacementsIsABadRequest() throws Exception {
        String attemptId = start("bus");

        submit(attemptId, solutionOf("bus").subList(0, 6), 1)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Level bus has 18 pieces, got 6 placements"));
    }

    @Test
    void aRotationOffTheStepIsABadRequest() throws Exception {
        String attemptId = start("bus");

        mvc.perform(post("/api/attempts/{id}/submit", attemptId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placements\": [{\"x\": 0, \"y\": 0, \"rotation\": 30, \"flipped\": false}],"
                                + " \"moves\": 1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negativeMovesAreABadRequest() throws Exception {
        String attemptId = start("bus");

        submit(attemptId, solutionOf("bus"), -1).andExpect(status().isBadRequest());
    }

    @Test
    void anAttemptCanBeSubmittedForADay() throws Exception {
        String attemptId = start("bus");
        clock.advance(Duration.ofHours(24));

        submit(attemptId, solutionOf("bus"), 9).andExpect(jsonPath("$.solved").value(true));
    }

    @Test
    void anAttemptOlderThanADayHasExpired() throws Exception {
        String attemptId = start("bus");
        clock.advance(Duration.ofHours(24).plusSeconds(1));

        submit(attemptId, solutionOf("bus"), 9)
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.detail").value("Attempt " + attemptId + " has expired; start the level again"));
    }

    @Test
    void mongoRemovesAttemptsTwoDaysAfterTheyStart() {
        assertThat(mongo.indexOps(Attempt.class).getIndexInfo())
                .filteredOn(index -> index.getName().equals(AttemptIndexes.EXPIRY_INDEX))
                .singleElement()
                .satisfies(index -> assertThat(index.getExpireAfter()).contains(Duration.ofHours(48)));
    }

    private String start(String levelId) throws Exception {
        String body = mvc.perform(post("/api/levels/{id}/attempts", levelId).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
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

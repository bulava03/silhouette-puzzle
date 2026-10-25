package com.silhouettepuzzle.attempt;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.silhouettepuzzle.IntegrationTestConfig;
import com.silhouettepuzzle.geometry.Placement;
import com.silhouettepuzzle.level.LevelRepository;
import com.silhouettepuzzle.player.PlayerRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Leaderboards, progress and attempt ownership, over HTTP against a real MongoDB.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestConfig.class)
class LeaderboardFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    @Autowired
    private LevelRepository levels;

    @Autowired
    private PlayerRepository players;

    @Autowired
    private AttemptRepository attempts;

    @Autowired
    private IntegrationTestConfig.MutableClock clock;

    @BeforeEach
    void clearResults() {
        attempts.deleteAll();
        players.deleteAll();
    }

    @Test
    void ranksEachPlayersBestRunByTimeThenMoves() throws Exception {
        MockHttpSession alice = player("alice");
        solve(alice, "bus", 30, 20);
        solve(alice, "bus", 25, 30);
        solve(player("bob"), "bus", 25, 10);
        solve(player("carol"), "bus", 40, 5);
        solve(null, "bus", 1, 1);           // anonymous: never ranked
        start(player("dave"), "bus");       // unsolved: never ranked

        mvc.perform(get("/api/levels/bus/leaderboard"))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].username").value("bob"))
                .andExpect(jsonPath("$[0].timeMs").value(25_000))
                .andExpect(jsonPath("$[1].username").value("alice"))
                .andExpect(jsonPath("$[1].moves").value(30))
                .andExpect(jsonPath("$[2].rank").value(3));
        mvc.perform(get("/api/levels/bus/leaderboard").param("limit", "1"))
                .andExpect(jsonPath("$.length()").value(1));
        for (String limit : List.of("0", "101")) {
            mvc.perform(get("/api/levels/bus/leaderboard").param("limit", limit)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/levels/nope/leaderboard")).andExpect(status().isNotFound());
    }

    @Test
    void progressKeepsTheBestRunOfEachLevel() throws Exception {
        MockHttpSession alice = player("alice");
        solve(alice, "bus", 25, 30);
        solve(alice, "bus", 25, 12);   // as fast with fewer moves: the new best
        solve(alice, "bus", 40, 3);    // slower: counted, not kept
        solve(alice, "house", 50, 12);

        mvc.perform(get("/api/me/progress").session(alice))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].levelId").value("bus"))
                .andExpect(jsonPath("$[0].solves").value(3))
                .andExpect(jsonPath("$[0].timeMs").value(25_000))
                .andExpect(jsonPath("$[0].moves").value(12))
                .andExpect(jsonPath("$[1].levelId").value("house"));
    }

    @Test
    void anotherPlayersAttemptCannotBeSubmitted() throws Exception {
        String attemptId = start(player("alice"), "bus");

        submit(player("bob"), attemptId, 1).andExpect(status().isNotFound());
        submit(null, attemptId, 1).andExpect(status().isNotFound());
    }

    private MockHttpSession player(String username) throws Exception {
        mvc.perform(post("/api/players").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"" + username + "\", \"password\": \"password-" + username + "\"}"))
                .andExpect(status().isCreated());
        return (MockHttpSession) mvc.perform(formLogin().user(username).password("password-" + username))
                .andReturn().getRequest().getSession(false);
    }

    private void solve(MockHttpSession session, String levelId, int seconds, int moves) throws Exception {
        String attemptId = start(session, levelId);
        clock.advance(Duration.ofSeconds(seconds));
        submit(session, attemptId, moves).andExpect(jsonPath("$.solved").value(true));
    }

    private String start(MockHttpSession session, String levelId) throws Exception {
        String body = mvc.perform(as(session, post("/api/levels/{id}/attempts", levelId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asString();
    }

    private ResultActions submit(MockHttpSession session, String attemptId, int moves) throws Exception {
        String levelId = attempts.findById(attemptId).orElseThrow().levelId();
        List<Placement> solution = levels.findById(levelId).orElseThrow().solution();
        return mvc.perform(as(session, post("/api/attempts/{id}/submit", attemptId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new SubmitRequest(solution, moves))));
    }

    private static MockHttpServletRequestBuilder as(MockHttpSession session, MockHttpServletRequestBuilder request) {
        request.with(csrf());
        return session == null ? request : request.session(session);
    }
}

package com.silhouettepuzzle.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.silhouettepuzzle.IntegrationTestConfig;
import com.silhouettepuzzle.attempt.AttemptRepository;
import com.silhouettepuzzle.attempt.AttemptService;
import com.silhouettepuzzle.geometry.Placement;
import com.silhouettepuzzle.level.LevelRepository;
import com.silhouettepuzzle.player.PlayerPrincipal;
import com.silhouettepuzzle.player.PlayerRepository;

/**
 * The HTML pages, rendered against a real MongoDB.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestConfig.class)
class PageFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PlayerRepository players;

    @Autowired
    private AttemptRepository attempts;

    @Autowired
    private LevelRepository levels;

    @Autowired
    private AttemptService attemptService;

    @Autowired
    private IntegrationTestConfig.MutableClock clock;

    @BeforeEach
    void clearPlayersAndResults() {
        attempts.deleteAll();
        players.deleteAll();
    }

    @Test
    void levelsPageListsEveryPictureForAGuest() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Fill the void"),
                        containsString("Bus"), containsString("House"), containsString("Rocket"),
                        containsString("18 parts"),
                        containsString("href=\"/play/bus\""),
                        containsString("data-void"),
                        containsString("Log in to keep your times"),
                        containsString("href=\"/login\""))));
    }

    @Test
    void levelsPageShowsThePlayersBests() throws Exception {
        MockHttpSession session = register("alice");
        solve("alice", "house", 48, 19);

        mvc.perform(get("/").session(session))
                .andExpect(content().string(allOf(
                        containsString("BUILT"), containsString("00:48"), containsString("19 moves"),
                        containsString("alice"), containsString("Log out"))));
    }

    @Test
    void gamePageCarriesTheLevelAndTheNextOne() throws Exception {
        mvc.perform(get("/play/rocket"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("data-level=\"rocket\""),
                        containsString("data-next=\"/play/bus\""),
                        containsString("id=\"board-svg\""),
                        containsString("/js/game.js"))));
    }

    @Test
    void unknownPicturesAreNotFound() throws Exception {
        mvc.perform(get("/play/nope"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Nothing in this void")));
        mvc.perform(get("/leaderboard").param("level", "nope")).andExpect(status().isNotFound());
    }

    @Test
    void leaderboardPageRanksPlayersAndMarksTheCurrentOne() throws Exception {
        MockHttpSession session = register("alice");
        register("bob");
        solve("bob", "bus", 52, 21);
        solve("alice", "bus", 67, 31);

        mvc.perform(get("/leaderboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Fastest builders"),
                        containsString("00:52"), containsString("01:07"),
                        containsString("(you)"))));
        mvc.perform(get("/leaderboard").param("level", "rocket"))
                .andExpect(content().string(containsString("Nobody has built this picture yet")));
    }

    @Test
    void loginPageShowsTheBuiltBusAndLoginErrors() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Welcome back"), containsString("Bus · built"),
                        not(containsString("Wrong username or password")))));
        mvc.perform(get("/login").param("error", ""))
                .andExpect(content().string(containsString("Wrong username or password")));
    }

    @Test
    void registeringFromTheFormLogsThePlayerIn() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("username", "Carol").param("password", "correct-horse"))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("Carol"));

        assertThat(players.findByUsernameKey("carol")).isPresent();
    }

    @Test
    void registrationErrorsAreShownOnTheForm() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("username", "a!").param("password", "short"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("must be 3 to 20 Latin letters, digits or underscores"),
                        containsString("Create an account"))));

        register("alice");
        mvc.perform(post("/register").with(csrf()).param("username", "ALICE").param("password", "correct-horse"))
                .andExpect(content().string(containsString("Username is taken: ALICE")));
    }

    @Test
    void theFormsNeedACsrfToken() throws Exception {
        mvc.perform(post("/register").param("username", "Carol").param("password", "correct-horse"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loggingInThroughTheFormWorks() throws Exception {
        register("alice");

        mvc.perform(formLogin("/login").user("ALICE").password("password-alice"))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("alice"));
    }

    private MockHttpSession register(String username) throws Exception {
        return (MockHttpSession) mvc.perform(post("/register").with(csrf())
                        .param("username", username).param("password", "password-" + username))
                .andExpect(redirectedUrl("/"))
                .andReturn().getRequest().getSession(false);
    }

    private void solve(String username, String levelId, int seconds, int moves) {
        PlayerPrincipal player = new PlayerPrincipal(players.findByUsernameKey(username).orElseThrow());
        String attemptId = attemptService.start(levelId, player).id();
        clock.advance(Duration.ofSeconds(seconds));
        List<Placement> solution = levels.findById(levelId).orElseThrow().solution();
        assertThat(attemptService.submit(attemptId, player, solution, moves).solved()).isTrue();
    }
}

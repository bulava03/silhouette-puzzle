package com.silhouettepuzzle.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.silhouettepuzzle.IntegrationTestConfig;

/**
 * Registration, login and the current player, over HTTP against a real MongoDB.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestConfig.class)
class PlayerFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PlayerRepository players;

    @BeforeEach
    void clearPlayers() {
        players.deleteAll();
    }

    @Test
    void registersAPlayerWithAHashedPassword() throws Exception {
        register("Alice", "correct-horse")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("Alice"));

        Player stored = players.findByUsernameKey("alice").orElseThrow();
        assertThat(stored.passwordHash()).startsWith("{bcrypt}").doesNotContain("correct-horse");
    }

    @Test
    void usernamesAreUniqueIgnoringCase() throws Exception {
        register("Alice", "correct-horse").andExpect(status().isCreated());

        register("ALICE", "another-pass").andExpect(status().isConflict());
    }

    @Test
    void rejectsAMalformedUsernameOrPassword() throws Exception {
        register("a!", "correct-horse").andExpect(status().isBadRequest());
        register("Alice", "short").andExpect(status().isBadRequest());
        // 42 characters but 84 bytes in UTF-8: over the BCrypt limit.
        register("Alice", "пароль".repeat(7))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Password must be at most 72 bytes in UTF-8"));
    }

    @Test
    void rejectsAStateChangingRequestWithoutCsrfToken() throws Exception {
        mvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"Alice\", \"password\": \"correct-horse\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void logsInIgnoringCaseAndShowsTheCurrentPlayer() throws Exception {
        register("Alice", "correct-horse");

        MockHttpSession session = login("ALICE", "correct-horse");

        mvc.perform(get("/api/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Alice"));
    }

    @Test
    void aWrongPasswordDoesNotLogIn() throws Exception {
        register("Alice", "correct-horse");

        mvc.perform(formLogin().user("Alice").password("wrong-horse")).andExpect(unauthenticated());
    }

    @Test
    void theCurrentPlayerNeedsALogin() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me/progress")).andExpect(status().isUnauthorized());
    }

    @Test
    void logsOut() throws Exception {
        register("Alice", "correct-horse");
        MockHttpSession session = login("Alice", "correct-horse");

        mvc.perform(post("/logout").with(csrf()).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(unauthenticated());
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void unknownApiPathsAreClosed() throws Exception {
        mvc.perform(get("/api/attempts")).andExpect(status().isUnauthorized());
    }

    private ResultActions register(String username, String password) throws Exception {
        return mvc.perform(post("/api/players").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}"));
    }

    private MockHttpSession login(String username, String password) throws Exception {
        return (MockHttpSession) mvc.perform(formLogin().user(username).password(password))
                .andExpect(authenticated())
                .andReturn().getRequest().getSession(false);
    }
}

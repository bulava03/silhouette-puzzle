package com.silhouettepuzzle.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
 * Registration, login and the current player over the API, against a real MongoDB.
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
    void registersPlayersWithHashedPasswordsAndUniqueNames() throws Exception {
        register("Alice", "correct-horse")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("Alice"));
        assertThat(players.findByUsernameKey("alice").orElseThrow().passwordHash())
                .startsWith("{bcrypt}").doesNotContain("correct-horse");

        register("ALICE", "another-pass").andExpect(status().isConflict());
        register("a!", "correct-horse").andExpect(status().isBadRequest());
        register("Bob", "short").andExpect(status().isBadRequest());
        // 42 characters but 84 bytes in UTF-8: over the BCrypt limit.
        register("Bob", "пароль".repeat(7))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Password must be at most 72 bytes in UTF-8"));
    }

    @Test
    void stateChangingRequestsNeedACsrfToken() throws Exception {
        mvc.perform(post("/api/players").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"Alice\", \"password\": \"correct-horse\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void theCurrentPlayerNeedsALogin() throws Exception {
        for (String path : new String[] {"/api/me", "/api/me/progress", "/api/attempts"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
        register("Alice", "correct-horse");
        mvc.perform(formLogin().user("Alice").password("wrong-horse")).andExpect(unauthenticated());

        MockHttpSession session = (MockHttpSession) mvc.perform(formLogin().user("ALICE").password("correct-horse"))
                .andReturn().getRequest().getSession(false);
        mvc.perform(get("/api/me").session(session)).andExpect(jsonPath("$.username").value("Alice"));

        mvc.perform(post("/logout").with(csrf()).session(session)).andExpect(unauthenticated());
        assertThat(session.isInvalid()).isTrue();
    }

    private ResultActions register(String username, String password) throws Exception {
        return mvc.perform(post("/api/players").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}"));
    }
}

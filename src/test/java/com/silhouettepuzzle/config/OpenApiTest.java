package com.silhouettepuzzle.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.silhouettepuzzle.IntegrationTestConfig;

/**
 * The OpenAPI document and Swagger UI are served without logging in.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestConfig.class)
class OpenApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void describesTheWholeApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Silhouette Puzzle API"))
                .andExpect(jsonPath("$.paths['/api/levels']").exists())
                .andExpect(jsonPath("$.paths['/api/levels/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/levels/{levelId}/attempts'].post").exists())
                .andExpect(jsonPath("$.paths['/api/attempts/{attemptId}/submit'].post").exists())
                .andExpect(jsonPath("$.paths['/api/levels/{levelId}/leaderboard']").exists())
                .andExpect(jsonPath("$.paths['/api/players'].post").exists())
                .andExpect(jsonPath("$.paths['/api/me/progress'].get.security[0].session").exists())
                .andExpect(jsonPath("$.paths['/login'].post").exists())
                .andExpect(jsonPath("$.components.securitySchemes.session.in").value("cookie"));
    }

    @Test
    void neverExposesTheReferenceSolution() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.LevelView.properties.silhouette").exists())
                .andExpect(jsonPath("$.components.schemas.LevelView.properties.solution").doesNotExist());
    }

    @Test
    void servesSwaggerUi() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
    }
}

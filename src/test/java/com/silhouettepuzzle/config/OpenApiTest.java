package com.silhouettepuzzle.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void describesTheApiWithoutTheReferenceSolution() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/levels/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/attempts/{attemptId}/submit'].post").exists())
                .andExpect(jsonPath("$.paths['/api/me/progress'].get.security[0].session").exists())
                .andExpect(jsonPath("$.paths['/login'].post").exists())
                .andExpect(jsonPath("$.components.schemas.Level.properties.pieces").exists())
                .andExpect(jsonPath("$.components.schemas.Level.properties.solution").doesNotExist());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}

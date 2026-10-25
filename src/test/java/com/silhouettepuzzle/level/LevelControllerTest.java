package com.silhouettepuzzle.level;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.silhouettepuzzle.geometry.Outline;
import com.silhouettepuzzle.geometry.Placement;

@WebMvcTest(LevelController.class)
class LevelControllerTest {

    private static final Outline UNIT = Outline.of(0, 0, 1, 0, 1, 1, 0, 1);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LevelRepository levels;

    @Test
    void listsLevelsInOrder() throws Exception {
        when(levels.findAllByOrderByNumberAsc()).thenReturn(List.of(
                new LevelSummary("square", "Square", 1), new LevelSummary("triangle", "Triangle", 2)));

        mvc.perform(get("/api/levels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("square"))
                .andExpect(jsonPath("$[1].number").value(2));
    }

    @Test
    void returnsALevelWithoutItsSolution() throws Exception {
        when(levels.findById("unit")).thenReturn(Optional.of(new Level("unit", "Unit", 1,
                List.of(UNIT), List.of(UNIT), List.of(new Placement(0, 0, 0, false)))));

        mvc.perform(get("/api/levels/unit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Unit"))
                .andExpect(jsonPath("$.silhouette[0].vertices[2].x").value(1.0))
                .andExpect(jsonPath("$.pieces.length()").value(1))
                .andExpect(jsonPath("$.solution").doesNotExist());
    }

    @Test
    void answersNotFoundWithAProblemDetail() throws Exception {
        when(levels.findById("missing")).thenReturn(Optional.empty());

        mvc.perform(get("/api/levels/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Level not found: missing"));
    }
}

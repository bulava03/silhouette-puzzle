package com.silhouettepuzzle.level;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Read-only access to levels.
 */
@RestController
@RequestMapping("/api/levels")
@Tag(name = "Levels", description = "Pictures to build")
public class LevelController {

    private final LevelRepository levels;

    public LevelController(LevelRepository levels) {
        this.levels = levels;
    }

    @GetMapping
    @Operation(summary = "List levels in play order")
    public List<Level> list() {
        return levels.findAllByOrderByNumberAsc();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a level to play",
            description = "Silhouette on the board and parts around their own origins; never the solution.")
    public Level get(@PathVariable String id) {
        return levels.findById(id).orElseThrow(() -> Level.notFound(id));
    }
}

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
@Tag(name = "Levels", description = "Puzzle levels")
public class LevelController {

    private final LevelRepository levels;

    /**
     * Creates the controller.
     *
     * @param levels level storage
     */
    public LevelController(LevelRepository levels) {
        this.levels = levels;
    }

    /**
     * Lists all levels in play order.
     *
     * @return level summaries
     */
    @GetMapping
    @Operation(summary = "List levels in play order")
    public List<LevelSummary> list() {
        return levels.findAllByOrderByNumberAsc();
    }

    /**
     * Returns one level ready to play.
     *
     * @param id level identifier
     * @return the level without its reference solution
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a level to play",
            description = "Silhouette in board coordinates and pieces in their own coordinates. "
                    + "The reference solution is never included.")
    public LevelView get(@PathVariable String id) {
        return levels.findById(id).map(LevelView::of).orElseThrow(() -> new LevelNotFoundException(id));
    }
}

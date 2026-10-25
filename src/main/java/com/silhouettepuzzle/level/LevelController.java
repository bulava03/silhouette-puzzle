package com.silhouettepuzzle.level;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only access to levels.
 */
@RestController
@RequestMapping("/api/levels")
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
    public LevelView get(@PathVariable String id) {
        return levels.findById(id).map(LevelView::of).orElseThrow(() -> new LevelNotFoundException(id));
    }
}

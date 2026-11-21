package com.silhouettepuzzle.controller;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import com.silhouettepuzzle.ApiException;
import com.silhouettepuzzle.dto.LevelTab;
import com.silhouettepuzzle.dto.LevelView;
import com.silhouettepuzzle.entity.Level;
import com.silhouettepuzzle.service.LevelService;

/**
 * The levels as the game shows them: each with its colour, which comes
 * from its place in the list, and the level that follows it.
 */
@Component
public class LevelViews {

    private final LevelService levels;
    private final Svg svg;

    public LevelViews(LevelService levels, Svg svg) {
        this.levels = levels;
        this.svg = svg;
    }

    /** The level with the given id, or 404. */
    public LevelView view(String id) {
        List<Level> all = levels.all();
        for (int i = 0; i < all.size(); i++) {
            Level level = all.get(i);
            if (level.id().equals(id)) {
                return new LevelView(level.id(), level.name(), level.number(), svg.accent(i),
                        all.get((i + 1) % all.size()).id(), level.silhouette(), level.pieces());
            }
        }
        throw ApiException.notFound("Level not found: " + id);
    }

    public List<LevelTab> tabs() {
        List<Level> all = levels.all();
        return IntStream.range(0, all.size())
                .mapToObj(i -> new LevelTab(all.get(i).id(), all.get(i).name(), all.get(i).number(), svg.accent(i)))
                .toList();
    }
}

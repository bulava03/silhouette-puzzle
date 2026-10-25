package com.silhouettepuzzle.level;

import java.util.List;

import com.silhouettepuzzle.geometry.Outline;

/**
 * Everything a client needs to play a level. The reference solution is
 * deliberately left out.
 *
 * @param id         level identifier
 * @param name       display name
 * @param number     position in the level list
 * @param silhouette parts of the figure to assemble, in board coordinates
 * @param pieces     piece outlines in their own coordinates
 */
public record LevelView(String id, String name, int number, List<Outline> silhouette, List<Outline> pieces) {

    /**
     * Creates the client view of a stored level.
     *
     * @param level the stored level
     * @return the view without the solution
     */
    public static LevelView of(Level level) {
        return new LevelView(level.id(), level.name(), level.number(), level.silhouette(), level.pieces());
    }
}

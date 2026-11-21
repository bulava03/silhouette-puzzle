package com.silhouettepuzzle.dto;

import java.util.List;

import com.silhouettepuzzle.entity.Piece;
import com.silhouettepuzzle.geometry.Outline;

/**
 * A level as the game gets it: what to show and what to build, never the
 * reference solution. {@code accent} is the colour of the silhouette's edge
 * and {@code next} the id of the level after it.
 */
public record LevelView(
        String id,
        String name,
        int number,
        String accent,
        String next,
        List<Outline> silhouette,
        List<Piece> pieces) {
}

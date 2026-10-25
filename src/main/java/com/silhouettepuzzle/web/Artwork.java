package com.silhouettepuzzle.web;

import java.util.ArrayList;
import java.util.List;

import com.silhouettepuzzle.geometry.Outline;
import com.silhouettepuzzle.level.Level;
import com.silhouettepuzzle.level.Piece;

/**
 * A finished picture, every part in its place, drawn as decoration.
 *
 * @param name    the level's name
 * @param viewBox SVG viewBox framing the picture
 * @param parts   each part's SVG points and colour
 */
public record Artwork(String name, String viewBox, List<Part> parts) {

    /**
     * Assembles a level from its reference solution.
     *
     * @param level the level
     * @return the finished picture
     */
    static Artwork of(Level level) {
        List<Outline> placed = new ArrayList<>(level.pieces().size());
        List<Part> parts = new ArrayList<>(level.pieces().size());
        for (int i = 0; i < level.pieces().size(); i++) {
            Piece piece = level.pieces().get(i);
            Outline outline = Shapes.placed(piece.outline(), level.solution().get(i));
            placed.add(outline);
            parts.add(new Part(Shapes.points(outline), piece.color()));
        }
        return new Artwork(level.name(), Shapes.viewBox(placed, 0.5), parts);
    }

    /**
     * One part of the picture.
     *
     * @param points SVG points
     * @param color  fill colour
     */
    public record Part(String points, String color) {
    }
}

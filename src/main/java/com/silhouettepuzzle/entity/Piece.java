package com.silhouettepuzzle.entity;

import com.silhouettepuzzle.geometry.Outline;
import com.silhouettepuzzle.geometry.Placement;

/**
 * One part of a picture: what it is, its natural colour ({@code #RRGGBB}),
 * its shape around its own origin and how it lies in the tray at the start.
 */
public record Piece(String name, String color, Outline outline, int startRotation) {

    public Piece {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A piece needs a name");
        }
        if (color == null || !color.matches("#[0-9A-Fa-f]{6}")) {
            throw new IllegalArgumentException("Piece colour must be #RRGGBB: " + color);
        }
        if (!Placement.isWholeStep(startRotation)) {
            throw new IllegalArgumentException("Start rotation must be a whole step: " + startRotation);
        }
    }
}

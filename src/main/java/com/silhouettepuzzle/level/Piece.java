package com.silhouettepuzzle.level;

import com.silhouettepuzzle.geometry.Outline;
import com.silhouettepuzzle.geometry.Placement;

/**
 * One part of a picture, as handed to the player.
 *
 * @param name          what the part is, such as "Window" or "Wheel"
 * @param color         fill colour as {@code #RRGGBB}, the natural colour of the real thing
 * @param outline       shape around the part's own origin
 * @param startRotation how the part lies in the tray at the start, in degrees
 */
public record Piece(String name, String color, Outline outline, int startRotation) {

    /**
     * Checks the colour format and that the start rotation is a whole step.
     */
    public Piece {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A piece needs a name");
        }
        if (color == null || !color.matches("#[0-9A-Fa-f]{6}")) {
            throw new IllegalArgumentException("Piece colour must be #RRGGBB: " + color);
        }
        if (startRotation < 0 || startRotation >= 360 || startRotation % Placement.ROTATION_STEP != 0) {
            throw new IllegalArgumentException("Start rotation must be a multiple of "
                    + Placement.ROTATION_STEP + " in [0, 360): " + startRotation);
        }
    }
}

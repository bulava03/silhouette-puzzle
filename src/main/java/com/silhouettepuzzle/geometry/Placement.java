package com.silhouettepuzzle.geometry;

/**
 * Where a player put a piece on the board.
 *
 * <p>The piece is first mirrored horizontally (if {@code flipped}), then
 * rotated around its own origin, then moved so that its origin lands on
 * ({@code x}, {@code y}). This matches the SVG transform
 * {@code translate(x, y) rotate(rotation) scale(-1, 1)}.
 *
 * @param x        board x of the piece origin
 * @param y        board y of the piece origin
 * @param rotation rotation in degrees, clockwise on screen; one of 0, 45, ..., 315
 * @param flipped  whether the piece is mirrored horizontally
 */
public record Placement(double x, double y, int rotation, boolean flipped) {

    /** Pieces turn in steps of this many degrees, as in the classic tangram. */
    public static final int ROTATION_STEP = 45;

    /**
     * Checks that the position is finite and the rotation is a whole step.
     */
    public Placement {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Placement position must be finite: (" + x + ", " + y + ")");
        }
        if (rotation < 0 || rotation >= 360 || rotation % ROTATION_STEP != 0) {
            throw new IllegalArgumentException(
                    "Rotation must be a multiple of " + ROTATION_STEP + " in [0, 360): " + rotation);
        }
    }
}

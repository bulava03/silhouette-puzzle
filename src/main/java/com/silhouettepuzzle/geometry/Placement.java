package com.silhouettepuzzle.geometry;

/**
 * Where a piece lies on the board: mirrored horizontally if {@code flipped},
 * then turned clockwise on screen by {@code rotation} degrees around its own
 * origin, then moved so the origin lands on (x, y). This is the SVG
 * transform {@code translate(x, y) rotate(rotation) scale(-1, 1)}.
 */
public record Placement(double x, double y, int rotation, boolean flipped) {

    /** Pieces turn in steps of this many degrees. */
    public static final int ROTATION_STEP = 45;

    public Placement {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Placement position must be finite: (" + x + ", " + y + ")");
        }
        if (!isWholeStep(rotation)) {
            throw new IllegalArgumentException(
                    "Rotation must be a multiple of " + ROTATION_STEP + " in [0, 360): " + rotation);
        }
    }

    /** Tells whether an angle is one of 0, 45, ..., 315. */
    public static boolean isWholeStep(int degrees) {
        return degrees >= 0 && degrees < 360 && degrees % ROTATION_STEP == 0;
    }
}

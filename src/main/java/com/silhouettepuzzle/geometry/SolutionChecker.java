package com.silhouettepuzzle.geometry;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.overlayng.OverlayNG;
import org.locationtech.jts.operation.overlayng.OverlayNGRobust;

/**
 * Decides whether placed pieces assemble a silhouette.
 *
 * <p>A solution is accepted when two areas, both measured as a fraction of
 * the silhouette area, stay within the tolerance:
 * <ul>
 *     <li>overlap: the sum of piece areas minus the area of their union;</li>
 *     <li>mismatch: the symmetric difference between the union of the pieces
 *     and the silhouette.</li>
 * </ul>
 * The tolerance absorbs floating-point noise and the small slack of pieces
 * placed by hand. It must stay well below the share of the smallest piece,
 * otherwise a solution with a piece missing would pass.
 */
public class SolutionChecker {

    /** Default tolerance: 1% of the silhouette area. */
    public static final double DEFAULT_TOLERANCE = 0.01;

    private final double tolerance;

    /**
     * Creates a checker with the {@link #DEFAULT_TOLERANCE default tolerance}.
     */
    public SolutionChecker() {
        this(DEFAULT_TOLERANCE);
    }

    /**
     * Creates a checker with the given tolerance.
     *
     * @param tolerance allowed overlap and mismatch, as a fraction of the
     *                  silhouette area, in [0, 1)
     */
    public SolutionChecker(double tolerance) {
        if (!(tolerance >= 0 && tolerance < 1)) {
            throw new IllegalArgumentException("Tolerance must be in [0, 1): " + tolerance);
        }
        this.tolerance = tolerance;
    }

    /**
     * Checks a solution.
     *
     * @param silhouette parts of the silhouette in board coordinates; parts may
     *                   touch but must not overlap
     * @param pieces     piece outlines in their own coordinates
     * @param placements placement of each piece, in the same order as pieces
     * @return the result with the measured overlap and mismatch
     */
    public CheckResult check(List<Outline> silhouette, List<Outline> pieces, List<Placement> placements) {
        if (silhouette.isEmpty()) {
            throw new IllegalArgumentException("Silhouette must have at least one part");
        }
        if (pieces.size() != placements.size()) {
            throw new IllegalArgumentException(
                    "Expected " + pieces.size() + " placements, got " + placements.size());
        }

        Geometry target = OverlayNGRobust.union(
                silhouette.stream().map(part -> (Geometry) Geometries.toPolygon(part.vertices())).toList());
        double targetArea = target.getArea();

        List<Geometry> placed = new ArrayList<>(pieces.size());
        double piecesArea = 0;
        for (int i = 0; i < pieces.size(); i++) {
            Geometry piece = Geometries.place(pieces.get(i), placements.get(i));
            placed.add(piece);
            piecesArea += piece.getArea();
        }
        Geometry covered = OverlayNGRobust.union(placed);

        double overlap = (piecesArea - covered.getArea()) / targetArea;
        double mismatch = OverlayNGRobust.overlay(covered, target, OverlayNG.SYMDIFFERENCE).getArea() / targetArea;
        boolean solved = overlap <= tolerance && mismatch <= tolerance;
        return new CheckResult(solved, overlap, mismatch);
    }
}

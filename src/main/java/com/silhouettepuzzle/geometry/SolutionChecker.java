package com.silhouettepuzzle.geometry;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.overlayng.OverlayNG;
import org.locationtech.jts.operation.overlayng.OverlayNGRobust;

/**
 * Decides whether placed pieces assemble a silhouette.
 *
 * <p>A solution is accepted when two areas, both as a fraction of the
 * silhouette area, stay within the tolerance: the overlap (sum of piece
 * areas minus the area of their union) and the mismatch (symmetric
 * difference between that union and the silhouette). The tolerance absorbs
 * float noise and the slack of hand-placed pieces; it must stay well below
 * the share of the smallest piece, or a solution missing it would pass.
 */
public class SolutionChecker {

    private final double tolerance;

    public SolutionChecker(double tolerance) {
        if (!(tolerance >= 0 && tolerance < 1)) {
            throw new IllegalArgumentException("Tolerance must be in [0, 1): " + tolerance);
        }
        this.tolerance = tolerance;
    }

    /**
     * Checks a solution: silhouette parts on the board, pieces in their own
     * coordinates and one placement per piece, in the same order.
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
        return new CheckResult(overlap <= tolerance && mismatch <= tolerance, overlap, mismatch);
    }
}

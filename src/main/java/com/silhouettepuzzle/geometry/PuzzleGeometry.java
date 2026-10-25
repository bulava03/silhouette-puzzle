package com.silhouettepuzzle.geometry;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.operation.overlayng.OverlayNG;
import org.locationtech.jts.operation.overlayng.OverlayNGRobust;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;

/**
 * The geometry of the puzzle, on JTS: placing pieces, checking a solution
 * against a silhouette, and building a silhouette from a reference solution.
 *
 * <p>A solution is accepted when two areas, both as a fraction of the
 * silhouette area, stay within the tolerance: the overlap (sum of piece
 * areas minus the area of their union) and the mismatch (symmetric
 * difference between that union and the silhouette). The tolerance absorbs
 * float noise and the slack of hand-placed pieces; it must stay well below
 * the share of the smallest piece, or a solution missing it would pass.
 */
public class PuzzleGeometry {

    private static final GeometryFactory FACTORY = new GeometryFactory();
    private static final PrecisionModel SILHOUETTE_PRECISION = new PrecisionModel(1e6);
    private static final double MAX_REFERENCE_OVERLAP = 1e-9;
    private static final double COLLINEAR_TOLERANCE = 1e-6;

    private final double tolerance;

    public PuzzleGeometry(double tolerance) {
        if (!(tolerance >= 0 && tolerance < 1)) {
            throw new IllegalArgumentException("Tolerance must be in [0, 1): " + tolerance);
        }
        this.tolerance = tolerance;
    }

    /** Outcome of a check; overlap and mismatch are fractions of the silhouette area. */
    public record Check(boolean solved, double overlap, double mismatch) {
    }

    /** Checks pieces, in their own coordinates, placed one placement per piece onto a silhouette. */
    public Check check(List<Outline> silhouette, List<Outline> pieces, List<Placement> placements) {
        if (silhouette.isEmpty()) {
            throw new IllegalArgumentException("Silhouette must have at least one part");
        }
        Geometry target = OverlayNGRobust.union(
                silhouette.stream().map(part -> (Geometry) toPolygon(part.vertices())).toList());
        Placed placed = place(pieces, placements);
        double overlap = (placed.area() - placed.union().getArea()) / target.getArea();
        double mismatch = OverlayNGRobust.overlay(placed.union(), target, OverlayNG.SYMDIFFERENCE).getArea()
                / target.getArea();
        return new Check(overlap <= tolerance && mismatch <= tolerance, overlap, mismatch);
    }

    /**
     * Unites placed pieces into silhouette parts; more than one part when
     * pieces only touch at a point. Overlapping pieces and holes are rejected.
     */
    public static List<Outline> assemble(List<Outline> pieces, List<Placement> placements) {
        Placed placed = place(pieces, placements);
        if (placed.area() - placed.union().getArea() > MAX_REFERENCE_OVERLAP * placed.area()) {
            throw new IllegalArgumentException("Pieces of the reference solution overlap");
        }
        // Round away float noise, then drop vertices where pieces meet along a straight edge.
        Geometry clean = DouglasPeuckerSimplifier.simplify(
                GeometryPrecisionReducer.reduce(placed.union(), SILHOUETTE_PRECISION), COLLINEAR_TOLERANCE);
        List<Outline> parts = new ArrayList<>(clean.getNumGeometries());
        for (int i = 0; i < clean.getNumGeometries(); i++) {
            Polygon polygon = (Polygon) clean.getGeometryN(i);
            if (polygon.getNumInteriorRing() > 0) {
                throw new IllegalArgumentException("The silhouette encloses a hole");
            }
            Coordinate[] ring = polygon.getExteriorRing().getCoordinates();
            List<Point> vertices = new ArrayList<>(ring.length - 1);
            for (int j = 0; j < ring.length - 1; j++) {
                vertices.add(new Point(ring[j].x + 0.0, ring[j].y + 0.0)); // + 0.0 turns -0.0 into 0.0
            }
            parts.add(new Outline(vertices));
        }
        return parts;
    }

    /** Moves a piece from its own coordinates to where the placement puts it. */
    static Geometry place(Outline piece, Placement placement) {
        AffineTransformation transform = new AffineTransformation();
        if (placement.flipped()) {
            transform.scale(-1, 1);
        }
        // With y pointing down, a positive JTS (counter-clockwise) angle
        // turns the piece clockwise on screen, as SVG rotate() does.
        transform.rotate(Math.toRadians(placement.rotation()));
        transform.translate(placement.x(), placement.y());
        return transform.transform(toPolygon(piece.vertices()));
    }

    static Polygon toPolygon(List<Point> vertices) {
        Coordinate[] ring = new Coordinate[vertices.size() + 1];
        for (int i = 0; i < vertices.size(); i++) {
            ring[i] = new Coordinate(vertices.get(i).x(), vertices.get(i).y());
        }
        ring[vertices.size()] = ring[0];
        return FACTORY.createPolygon(ring);
    }

    private static Placed place(List<Outline> pieces, List<Placement> placements) {
        if (pieces.isEmpty() || pieces.size() != placements.size()) {
            throw new IllegalArgumentException("Need one placement per piece, got " + pieces.size()
                    + " pieces and " + placements.size() + " placements");
        }
        List<Geometry> placed = new ArrayList<>(pieces.size());
        double area = 0;
        for (int i = 0; i < pieces.size(); i++) {
            Geometry piece = place(pieces.get(i), placements.get(i));
            placed.add(piece);
            area += piece.getArea();
        }
        return new Placed(OverlayNGRobust.union(placed), area);
    }

    /** The union of placed pieces and the sum of their areas. */
    private record Placed(Geometry union, double area) {
    }
}

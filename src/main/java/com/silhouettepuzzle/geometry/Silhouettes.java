package com.silhouettepuzzle.geometry;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.operation.overlayng.OverlayNGRobust;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;

/**
 * Builds a silhouette from a reference solution, so every level has one.
 */
public final class Silhouettes {

    private static final PrecisionModel PRECISION = new PrecisionModel(1e6);
    private static final double MAX_OVERLAP_RATIO = 1e-9;
    private static final double COLLINEAR_TOLERANCE = 1e-6;

    private Silhouettes() {
    }

    /**
     * Unites placed pieces into silhouette parts; more than one part when
     * pieces only touch at a point. Overlapping pieces and holes are rejected.
     */
    public static List<Outline> assemble(List<Outline> pieces, List<Placement> placements) {
        if (pieces.isEmpty() || pieces.size() != placements.size()) {
            throw new IllegalArgumentException("Need one placement per piece, got " + pieces.size()
                    + " pieces and " + placements.size() + " placements");
        }
        List<Geometry> placed = new ArrayList<>(pieces.size());
        double piecesArea = 0;
        for (int i = 0; i < pieces.size(); i++) {
            Geometry piece = Geometries.place(pieces.get(i), placements.get(i));
            placed.add(piece);
            piecesArea += piece.getArea();
        }
        Geometry union = OverlayNGRobust.union(placed);
        if (piecesArea - union.getArea() > MAX_OVERLAP_RATIO * piecesArea) {
            throw new IllegalArgumentException("Pieces of the reference solution overlap");
        }
        // Round away float noise, then drop vertices where pieces meet along a straight edge.
        Geometry clean = DouglasPeuckerSimplifier.simplify(
                GeometryPrecisionReducer.reduce(union, PRECISION), COLLINEAR_TOLERANCE);
        List<Outline> parts = new ArrayList<>(clean.getNumGeometries());
        for (int i = 0; i < clean.getNumGeometries(); i++) {
            parts.add(toOutline((Polygon) clean.getGeometryN(i)));
        }
        return parts;
    }

    private static Outline toOutline(Polygon polygon) {
        if (polygon.getNumInteriorRing() > 0) {
            throw new IllegalArgumentException("The silhouette encloses a hole");
        }
        Coordinate[] ring = polygon.getExteriorRing().getCoordinates();
        List<Point> vertices = new ArrayList<>(ring.length - 1);
        for (int i = 0; i < ring.length - 1; i++) {
            // Adding 0.0 turns -0.0 into 0.0.
            vertices.add(new Point(ring[i].x + 0.0, ring[i].y + 0.0));
        }
        return new Outline(vertices);
    }
}

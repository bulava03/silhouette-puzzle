package com.silhouettepuzzle.geometry;

import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.AffineTransformation;

/**
 * Conversions between the puzzle model and JTS geometries.
 */
final class Geometries {

    private static final GeometryFactory FACTORY = new GeometryFactory();

    private Geometries() {
    }

    static Polygon toPolygon(List<Point> vertices) {
        Coordinate[] ring = new Coordinate[vertices.size() + 1];
        for (int i = 0; i < vertices.size(); i++) {
            ring[i] = new Coordinate(vertices.get(i).x(), vertices.get(i).y());
        }
        ring[vertices.size()] = ring[0];
        return FACTORY.createPolygon(ring);
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
}

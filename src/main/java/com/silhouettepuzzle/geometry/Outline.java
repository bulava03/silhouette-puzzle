package com.silhouettepuzzle.geometry;

import java.util.List;

/**
 * A simple polygon given by its vertices in order, without repeating the
 * first one. Used for pieces (around their own origin) and for silhouette
 * parts (on the board). It must have a non-zero area and no self-intersections.
 */
public record Outline(List<Point> vertices) {

    public Outline {
        if (vertices == null || vertices.size() < 3) {
            throw new IllegalArgumentException("An outline needs at least 3 vertices");
        }
        vertices = List.copyOf(vertices);
        if (!Geometries.toPolygon(vertices).isValid()) {
            throw new IllegalArgumentException("Outline is not a valid simple polygon: " + vertices);
        }
    }

    /** Creates an outline from coordinates given as x1, y1, x2, y2, ... */
    public static Outline of(double... coordinates) {
        if (coordinates.length % 2 != 0) {
            throw new IllegalArgumentException("Coordinates must come in x, y pairs");
        }
        Point[] points = new Point[coordinates.length / 2];
        for (int i = 0; i < points.length; i++) {
            points[i] = new Point(coordinates[2 * i], coordinates[2 * i + 1]);
        }
        return new Outline(List.of(points));
    }

    public double area() {
        return Geometries.toPolygon(vertices).getArea();
    }
}

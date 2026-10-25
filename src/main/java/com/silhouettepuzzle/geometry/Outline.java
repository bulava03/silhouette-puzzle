package com.silhouettepuzzle.geometry;

import java.util.List;

/**
 * A simple polygon given by its vertices in order, without repeating the
 * first vertex at the end. Used both for puzzle pieces (in the piece's own
 * coordinates) and for the parts of a silhouette (in board coordinates).
 *
 * <p>The outline is validated on creation: it needs at least three vertices,
 * a non-zero area and no self-intersections.
 *
 * @param vertices polygon vertices in order
 */
public record Outline(List<Point> vertices) {

    /**
     * Copies the vertices and checks that they form a valid polygon.
     */
    public Outline {
        if (vertices == null || vertices.size() < 3) {
            throw new IllegalArgumentException("An outline needs at least 3 vertices");
        }
        vertices = List.copyOf(vertices);
        if (!Geometries.toPolygon(vertices).isValid()) {
            throw new IllegalArgumentException("Outline is not a valid simple polygon: " + vertices);
        }
    }

    /**
     * Creates an outline from a flat list of coordinates.
     *
     * @param coordinates x and y of each vertex in turn: x1, y1, x2, y2, ...
     * @return the outline
     */
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

    /**
     * Returns the area enclosed by the outline.
     *
     * @return the area, always positive
     */
    public double area() {
        return Geometries.toPolygon(vertices).getArea();
    }
}

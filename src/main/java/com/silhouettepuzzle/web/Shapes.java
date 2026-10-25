package com.silhouettepuzzle.web;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.silhouettepuzzle.geometry.Outline;
import com.silhouettepuzzle.geometry.Placement;
import com.silhouettepuzzle.geometry.Point;

/**
 * Turns outlines into SVG attribute values for the page templates.
 */
final class Shapes {

    private Shapes() {
    }

    /**
     * Formats an outline as the {@code points} attribute of an SVG polygon.
     *
     * @param outline the outline
     * @return points like {@code "0,0 4,0 4,4"}
     */
    static String points(Outline outline) {
        return outline.vertices().stream()
                .map(p -> number(p.x()) + "," + number(p.y()))
                .collect(Collectors.joining(" "));
    }

    /**
     * Returns a {@code viewBox} that frames all outlines with a margin.
     *
     * @param outlines the outlines
     * @param margin   space around them, in board units
     * @return the viewBox value
     */
    static String viewBox(List<Outline> outlines, double margin) {
        List<Point> all = outlines.stream().flatMap(o -> o.vertices().stream()).toList();
        double minX = all.stream().mapToDouble(Point::x).min().orElse(0);
        double minY = all.stream().mapToDouble(Point::y).min().orElse(0);
        double maxX = all.stream().mapToDouble(Point::x).max().orElse(1);
        double maxY = all.stream().mapToDouble(Point::y).max().orElse(1);
        return number(minX - margin) + " " + number(minY - margin) + " "
                + number(maxX - minX + 2 * margin) + " " + number(maxY - minY + 2 * margin);
    }

    /**
     * Moves a piece outline to where a placement puts it: mirrored if
     * flipped, turned clockwise on screen, then moved, as the server checks it.
     *
     * @param outline   the piece around its own origin
     * @param placement where the piece goes
     * @return the outline in board coordinates
     */
    static Outline placed(Outline outline, Placement placement) {
        double angle = Math.toRadians(placement.rotation());
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Outline(outline.vertices().stream().map(v -> {
            double x = placement.flipped() ? -v.x() : v.x();
            return new Point(x * cos - v.y() * sin + placement.x(), x * sin + v.y() * cos + placement.y());
        }).toList());
    }

    private static String number(double value) {
        String text = String.format(Locale.ROOT, "%.4f", value + 0.0);
        return text.replaceAll("\\.?0+$", "");
    }
}

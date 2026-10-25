package com.silhouettepuzzle.web;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import com.silhouettepuzzle.geometry.Outline;
import com.silhouettepuzzle.geometry.Placement;
import com.silhouettepuzzle.geometry.Point;
import com.silhouettepuzzle.level.Level;

/**
 * Helpers the templates call as {@code svg}: SVG attribute values for
 * outlines, the level colours, and times and dates as the pages show them.
 */
@Component
public class Svg {

    private static final List<String> ACCENTS = List.of("#6BD8DE", "#EE8DBD", "#E4AC59");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
            .withZone(ZoneOffset.UTC);

    /** The {@code points} of an SVG polygon, like {@code "0,0 4,0 4,4"}. */
    public String points(Outline outline) {
        return outline.vertices().stream()
                .map(p -> number(p.x()) + "," + number(p.y()))
                .collect(Collectors.joining(" "));
    }

    /** A {@code viewBox} that frames the outlines with half a unit around them. */
    public String viewBox(List<Outline> outlines) {
        List<Point> all = outlines.stream().flatMap(o -> o.vertices().stream()).toList();
        double minX = all.stream().mapToDouble(Point::x).min().orElse(0) - 0.5;
        double minY = all.stream().mapToDouble(Point::y).min().orElse(0) - 0.5;
        double maxX = all.stream().mapToDouble(Point::x).max().orElse(1) + 0.5;
        double maxY = all.stream().mapToDouble(Point::y).max().orElse(1) + 0.5;
        return number(minX) + " " + number(minY) + " " + number(maxX - minX) + " " + number(maxY - minY);
    }

    /** Every piece of a level in its place: the finished picture. */
    public List<Outline> picture(Level level) {
        return IntStream.range(0, level.pieces().size())
                .mapToObj(i -> placed(level.pieces().get(i).outline(), level.solution().get(i)))
                .toList();
    }

    /** The colour of the level at a position in the level list. */
    public String accent(int index) {
        return ACCENTS.get(index % ACCENTS.size());
    }

    /** Minutes and seconds, like {@code 01:07}. */
    public String clock(long millis) {
        long seconds = millis / 1000;
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    /** A day like {@code 25 Oct}. */
    public String day(Instant instant) {
        return DAY.format(instant);
    }

    /** Mirrored, turned and moved as the server checks it. */
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
        return String.format(Locale.ROOT, "%.4f", value + 0.0).replaceAll("\\.?0+$", "");
    }
}

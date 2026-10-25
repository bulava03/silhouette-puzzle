package com.silhouettepuzzle.level;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;

import com.silhouettepuzzle.geometry.Outline;
import com.silhouettepuzzle.geometry.Placement;
import com.silhouettepuzzle.geometry.Point;
import com.silhouettepuzzle.geometry.Silhouettes;

import tools.jackson.databind.json.JsonMapper;

/**
 * Reads built-in levels from a JSON file.
 *
 * <p>A level in the file is the finished picture: every part is drawn where
 * it belongs. Each part's own shape is taken around its centre, mirrored for
 * parts marked {@code mirrored} so the player has to flip them back. The
 * reference solution puts each centre back in place, and the silhouette is
 * the union of the placed parts, so every level is solvable by construction.
 */
public final class LevelCatalog {

    // Centres are rounded to 0.01 and shapes to 1e-6: catalog coordinates have
    // at most two decimals, so corners shared by two parts stay exactly equal.
    private static final double CENTRE_SCALE = 100;
    private static final double SHAPE_SCALE = 1_000_000;

    private LevelCatalog() {
    }

    public static List<Level> read(Resource file, JsonMapper mapper) {
        CatalogFile catalog;
        try (InputStream in = file.getInputStream()) {
            catalog = mapper.readValue(in, CatalogFile.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read level catalog " + file, e);
        }
        List<Level> levels = new ArrayList<>(catalog.levels().size());
        for (LevelSpec spec : catalog.levels()) {
            try {
                levels.add(build(spec, catalog.colors()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Level " + spec.id() + " is invalid: " + e.getMessage(), e);
            }
        }
        return levels;
    }

    private static Level build(LevelSpec spec, Map<String, String> colors) {
        if (spec.parts() == null || spec.parts().isEmpty()) {
            throw new IllegalArgumentException("a level needs parts");
        }
        List<Piece> pieces = new ArrayList<>(spec.parts().size());
        List<Placement> solution = new ArrayList<>(spec.parts().size());
        for (PartSpec part : spec.parts()) {
            String color = colors.get(part.color());
            if (color == null) {
                throw new IllegalArgumentException("unknown colour " + part.color() + " of part " + part.name());
            }
            double cx = round(part.outline().stream().mapToDouble(v -> v[0]).average().orElseThrow(), CENTRE_SCALE);
            double cy = round(part.outline().stream().mapToDouble(v -> v[1]).average().orElseThrow(), CENTRE_SCALE);
            boolean mirrored = Boolean.TRUE.equals(part.mirrored());
            List<Point> shape = part.outline().stream()
                    .map(v -> new Point(round((mirrored ? cx - v[0] : v[0] - cx), SHAPE_SCALE) + 0.0,
                            round(v[1] - cy, SHAPE_SCALE) + 0.0))
                    .toList();
            pieces.add(new Piece(part.name(), color, new Outline(shape), part.startRotation()));
            solution.add(new Placement(cx, cy, 0, mirrored));
        }
        List<Outline> silhouette = Silhouettes.assemble(pieces.stream().map(Piece::outline).toList(), solution);
        return new Level(spec.id(), spec.name(), spec.number(), silhouette, pieces, solution);
    }

    private static double round(double value, double scale) {
        return Math.round(value * scale) / scale;
    }

    /** Layout of the catalog file: named colours and the levels. */
    record CatalogFile(Map<String, String> colors, List<LevelSpec> levels) {
    }

    /** A level as written in the file. */
    record LevelSpec(String id, String name, int number, List<PartSpec> parts) {
    }

    /** A part drawn where it belongs; {@code mirrored} is optional. */
    record PartSpec(String name, String color, List<double[]> outline, int startRotation, Boolean mirrored) {
    }
}

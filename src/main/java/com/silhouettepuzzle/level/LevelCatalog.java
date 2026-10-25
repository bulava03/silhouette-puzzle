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
 * it belongs. From that the catalog derives what the game needs. Each part's
 * own shape is taken around its centre, mirrored for parts marked
 * {@code mirrored} so the player has to flip them back. The reference
 * solution puts each centre back in place. The silhouette is the union of
 * the placed parts, so every level is solvable by construction.
 */
public final class LevelCatalog {

    /** Part centres are rounded to this many decimals, keeping shared corners exact. */
    private static final double CENTRE_SCALE = 100;

    /** Part shapes are rounded to this many decimals to drop float noise. */
    private static final double SHAPE_SCALE = 1_000_000;

    private LevelCatalog() {
    }

    /**
     * Reads and builds the levels of a catalog file.
     *
     * @param file   the JSON catalog
     * @param mapper JSON mapper
     * @return the levels, ready to store
     * @throws IllegalArgumentException if a level is inconsistent
     */
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
            double mirror = part.isMirrored() ? -1 : 1;
            List<Point> shape = part.outline().stream()
                    .map(v -> new Point(round(mirror * (v[0] - cx), SHAPE_SCALE) + 0.0, round(v[1] - cy, SHAPE_SCALE) + 0.0))
                    .toList();
            pieces.add(new Piece(part.name(), color, new Outline(shape), part.startRotation()));
            solution.add(new Placement(cx, cy, 0, part.isMirrored()));
        }
        List<Outline> silhouette = Silhouettes.assemble(pieces.stream().map(Piece::outline).toList(), solution);
        return new Level(spec.id(), spec.name(), spec.number(), silhouette, pieces, solution);
    }

    private static double round(double value, double scale) {
        return Math.round(value * scale) / scale;
    }

    /**
     * Layout of a catalog file.
     *
     * @param colors named colours, as {@code #RRGGBB}
     * @param levels the levels
     */
    record CatalogFile(Map<String, String> colors, List<LevelSpec> levels) {
    }

    /**
     * A level as written in a catalog file.
     *
     * @param id     level identifier
     * @param name   display name
     * @param number position in the level list
     * @param parts  the parts, each drawn where it belongs in the picture
     */
    record LevelSpec(String id, String name, int number, List<PartSpec> parts) {
    }

    /**
     * A part as written in a catalog file.
     *
     * @param name          what the part is
     * @param color         name of a colour from the file's colour list
     * @param outline       the part's [x, y] vertices in the finished picture
     * @param startRotation how the part lies in the tray at the start, in degrees
     * @param mirrored      whether the player gets the mirror image and has to flip it; optional
     */
    record PartSpec(String name, String color, List<double[]> outline, int startRotation, Boolean mirrored) {

        /**
         * Tells whether the part is handed out mirrored; absent in the file means no.
         *
         * @return true for a mirrored part
         */
        boolean isMirrored() {
            return Boolean.TRUE.equals(mirrored);
        }
    }
}

package com.silhouettepuzzle.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.silhouettepuzzle.ApiException;
import com.silhouettepuzzle.entity.Level;
import com.silhouettepuzzle.entity.Piece;
import com.silhouettepuzzle.geometry.Outline;
import com.silhouettepuzzle.geometry.Placement;
import com.silhouettepuzzle.geometry.Point;
import com.silhouettepuzzle.geometry.PuzzleGeometry;
import com.silhouettepuzzle.repository.LevelRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * The levels: built from the catalog file and written over their stored
 * copies on every start, so edits to the catalog reach the database.
 *
 * <p>A level in the catalog is the finished picture: every part is drawn
 * where it belongs. Each part's own shape is taken around its centre,
 * mirrored for parts marked {@code mirrored} so the player has to flip them
 * back. The reference solution puts each centre back in place, and the
 * silhouette is the union of the placed parts, so every level is solvable.
 */
@Service
public class LevelService implements ApplicationRunner {

    static final String CATALOG = "levels/pictures.json";

    private static final Logger LOG = LoggerFactory.getLogger(LevelService.class);
    // Centres are rounded to 0.01 and shapes to 1e-6: catalog coordinates have
    // at most two decimals, so corners shared by two parts stay exactly equal.
    private static final double CENTRE_SCALE = 100;
    private static final double SHAPE_SCALE = 1_000_000;

    private final LevelRepository levels;
    private final JsonMapper mapper;

    public LevelService(LevelRepository levels, JsonMapper mapper) {
        this.levels = levels;
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Level> builtIn = read(new ClassPathResource(CATALOG), mapper);
        levels.saveAll(builtIn);
        LOG.info("Stored {} built-in level(s)", builtIn.size());
    }

    public List<Level> all() {
        return levels.findAllByOrderByNumberAsc();
    }

    public Level get(String id) {
        return levels.findById(id).orElseThrow(() -> ApiException.notFound("Level not found: " + id));
    }

    static List<Level> read(Resource file, JsonMapper mapper) {
        CatalogFile catalog;
        try (InputStream in = file.getInputStream()) {
            catalog = mapper.readValue(in, CatalogFile.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read level catalog " + file, e);
        }
        List<Level> built = new ArrayList<>(catalog.levels().size());
        for (LevelSpec spec : catalog.levels()) {
            try {
                built.add(build(spec, catalog.colors()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Level " + spec.id() + " is invalid: " + e.getMessage(), e);
            }
        }
        return built;
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
                    .map(v -> new Point(round(mirrored ? cx - v[0] : v[0] - cx, SHAPE_SCALE) + 0.0,
                            round(v[1] - cy, SHAPE_SCALE) + 0.0))
                    .toList();
            pieces.add(new Piece(part.name(), color, new Outline(shape), part.startRotation()));
            solution.add(new Placement(cx, cy, 0, mirrored));
        }
        List<Outline> silhouette = PuzzleGeometry.assemble(pieces.stream().map(Piece::outline).toList(), solution);
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

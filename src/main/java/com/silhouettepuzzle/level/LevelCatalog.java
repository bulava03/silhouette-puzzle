package com.silhouettepuzzle.level;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;

import com.silhouettepuzzle.geometry.Outline;
import com.silhouettepuzzle.geometry.Placement;
import com.silhouettepuzzle.geometry.Point;
import com.silhouettepuzzle.geometry.Silhouettes;

import tools.jackson.databind.json.JsonMapper;

/**
 * Reads built-in levels from a JSON file. A level in the file names a piece
 * set and gives a reference solution; its silhouette is built from that
 * solution, so every level is solvable by construction.
 */
public final class LevelCatalog {

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
            levels.add(build(spec, catalog.pieceSets()));
        }
        return levels;
    }

    private static Level build(LevelSpec spec, Map<String, List<double[][]>> pieceSets) {
        List<double[][]> pieceSet = pieceSets.get(spec.pieceSet());
        if (pieceSet == null) {
            throw new IllegalArgumentException("Level " + spec.id() + " uses unknown piece set " + spec.pieceSet());
        }
        List<Outline> pieces = pieceSet.stream().map(LevelCatalog::toOutline).toList();
        try {
            List<Outline> silhouette = Silhouettes.assemble(pieces, spec.solution());
            return new Level(spec.id(), spec.name(), spec.number(), silhouette, pieces, spec.solution());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Level " + spec.id() + " is invalid: " + e.getMessage(), e);
        }
    }

    private static Outline toOutline(double[][] vertices) {
        return new Outline(Arrays.stream(vertices).map(v -> new Point(v[0], v[1])).toList());
    }

    /**
     * Layout of a catalog file.
     *
     * @param pieceSets named piece sets; each piece is a list of [x, y] vertices
     * @param levels    the levels
     */
    record CatalogFile(Map<String, List<double[][]>> pieceSets, List<LevelSpec> levels) {
    }

    /**
     * A level as written in a catalog file.
     *
     * @param id       level identifier
     * @param name     display name
     * @param number   position in the level list
     * @param pieceSet name of the piece set
     * @param solution reference placement of each piece of the set
     */
    record LevelSpec(String id, String name, int number, String pieceSet, List<Placement> solution) {
    }
}

package com.silhouettepuzzle.level;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

/**
 * Stores the built-in levels on startup. Levels already in the database are
 * left as they are, so the seeder is safe to run on every start.
 */
@Component
public class LevelSeeder implements ApplicationRunner {

    /** Classpath location of the built-in level catalog. */
    static final String CATALOG = "levels/tangram.json";

    private static final Logger LOG = LoggerFactory.getLogger(LevelSeeder.class);

    private final LevelRepository levels;
    private final JsonMapper mapper;

    /**
     * Creates the seeder.
     *
     * @param levels level storage
     * @param mapper JSON mapper
     */
    public LevelSeeder(LevelRepository levels, JsonMapper mapper) {
        this.levels = levels;
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Level> missing = LevelCatalog.read(new ClassPathResource(CATALOG), mapper).stream()
                .filter(level -> !levels.existsById(level.id()))
                .toList();
        levels.saveAll(missing);
        LOG.info("Seeded {} new level(s)", missing.size());
    }
}

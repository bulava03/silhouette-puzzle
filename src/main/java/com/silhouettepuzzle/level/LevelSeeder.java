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
 * Stores the built-in levels on startup. Each one is written over its stored
 * copy by id, so edits to the catalog file reach the database on the next
 * start and running the seeder again changes nothing.
 */
@Component
public class LevelSeeder implements ApplicationRunner {

    /** Classpath location of the built-in level catalog. */
    static final String CATALOG = "levels/pictures.json";

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
        List<Level> builtIn = LevelCatalog.read(new ClassPathResource(CATALOG), mapper);
        levels.saveAll(builtIn);
        LOG.info("Stored {} built-in level(s)", builtIn.size());
    }
}

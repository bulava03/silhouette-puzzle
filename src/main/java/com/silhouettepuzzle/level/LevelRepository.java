package com.silhouettepuzzle.level;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Stored levels.
 */
public interface LevelRepository extends MongoRepository<Level, String> {

    /**
     * Lists all levels in play order, reading only the summary fields.
     *
     * @return level summaries ordered by number
     */
    List<LevelSummary> findAllByOrderByNumberAsc();
}

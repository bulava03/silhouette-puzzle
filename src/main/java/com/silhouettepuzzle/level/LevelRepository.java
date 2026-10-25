package com.silhouettepuzzle.level;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Stored levels.
 */
public interface LevelRepository extends MongoRepository<Level, String> {

    List<Level> findAllByOrderByNumberAsc();
}

package com.silhouettepuzzle.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.silhouettepuzzle.entity.Level;

/**
 * Stored levels.
 */
public interface LevelRepository extends MongoRepository<Level, String> {

    List<Level> findAllByOrderByNumberAsc();
}

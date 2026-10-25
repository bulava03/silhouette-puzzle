package com.silhouettepuzzle.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.silhouettepuzzle.entity.Attempt;

/**
 * Open attempts.
 */
public interface AttemptRepository extends MongoRepository<Attempt, String> {
}

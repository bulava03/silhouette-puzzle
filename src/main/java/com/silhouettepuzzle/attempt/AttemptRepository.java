package com.silhouettepuzzle.attempt;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Stored attempts.
 */
public interface AttemptRepository extends MongoRepository<Attempt, String> {
}

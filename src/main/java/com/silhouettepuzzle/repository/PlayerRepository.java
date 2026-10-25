package com.silhouettepuzzle.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.silhouettepuzzle.entity.Player;

/**
 * Stored players, looked up by the lower-cased {@link Player#keyOf(String) username key}.
 */
public interface PlayerRepository extends MongoRepository<Player, String> {

    Optional<Player> findByUsernameKey(String usernameKey);

    boolean existsByUsernameKey(String usernameKey);
}

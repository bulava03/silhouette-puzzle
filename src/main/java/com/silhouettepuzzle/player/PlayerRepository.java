package com.silhouettepuzzle.player;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Stored players, looked up by the lower-cased {@link Player#keyOf(String) username key}.
 */
public interface PlayerRepository extends MongoRepository<Player, String> {

    Optional<Player> findByUsernameKey(String usernameKey);

    boolean existsByUsernameKey(String usernameKey);
}

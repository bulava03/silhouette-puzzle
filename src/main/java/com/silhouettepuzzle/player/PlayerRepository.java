package com.silhouettepuzzle.player;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Stored players.
 */
public interface PlayerRepository extends MongoRepository<Player, String> {

    /**
     * Finds a player by the lower-cased username.
     *
     * @param usernameKey the key from {@link Player#keyOf(String)}
     * @return the player, if registered
     */
    Optional<Player> findByUsernameKey(String usernameKey);

    /**
     * Tells whether a username is taken, ignoring case.
     *
     * @param usernameKey the key from {@link Player#keyOf(String)}
     * @return true if a player has this name
     */
    boolean existsByUsernameKey(String usernameKey);
}

package com.silhouettepuzzle.player;

import java.time.Instant;
import java.util.Locale;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A registered player.
 *
 * @param id           player identifier, assigned by MongoDB
 * @param username     name as the player typed it, shown in leaderboards
 * @param usernameKey  lower-cased name; unique, so names differing only in case clash
 * @param passwordHash encoded password
 * @param createdAt    registration time
 */
@Document("players")
public record Player(
        @Id String id,
        String username,
        @Indexed(unique = true) String usernameKey,
        String passwordHash,
        Instant createdAt) {

    /**
     * Returns the lookup key of a username.
     *
     * @param username a username in any case
     * @return the lower-cased key
     */
    public static String keyOf(String username) {
        return username.toLowerCase(Locale.ROOT);
    }
}

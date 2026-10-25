package com.silhouettepuzzle.player;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A registered player with their best run on every level they solved.
 *
 * <p>The bests live inside the player's document rather than in a
 * collection of their own: there is at most one per level, a player's
 * progress is read with the player, and the leaderboard of a level is an
 * aggregation over the players that have it.
 *
 * @param id           player identifier, assigned by MongoDB
 * @param username     name as the player typed it, shown in leaderboards
 * @param usernameKey  lower-cased name; unique, so names differing only in case clash
 * @param passwordHash encoded password
 * @param createdAt    registration time
 * @param bests        best run per solved level; null in documents stored before there were any
 */
@Document("players")
@CompoundIndex(name = "bests-level", def = "{'bests.levelId': 1}")
public record Player(
        @Id String id,
        String username,
        @Indexed(unique = true) String usernameKey,
        String passwordHash,
        Instant createdAt,
        List<PlayerBest> bests) {

    /**
     * Returns the lookup key of a username.
     *
     * @param username a username in any case
     * @return the lower-cased key
     */
    public static String keyOf(String username) {
        return username.toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the bests, never null.
     *
     * @return best run per solved level
     */
    public List<PlayerBest> bestsOrEmpty() {
        return bests == null ? List.of() : bests;
    }
}

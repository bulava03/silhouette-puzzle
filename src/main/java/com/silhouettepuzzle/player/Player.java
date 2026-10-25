package com.silhouettepuzzle.player;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A registered player with their best run on every level they solved.
 * {@code usernameKey} is the lower-cased name and is unique (see the
 * migrations in {@code db/changelog}), so names that differ only in case clash.
 * {@code bests} is null in documents stored before there were any.
 */
@Document("players")
public record Player(
        @Id String id,
        String username,
        String usernameKey,
        String passwordHash,
        Instant createdAt,
        List<PlayerBest> bests) {

    public static String keyOf(String username) {
        return username.toLowerCase(Locale.ROOT);
    }

    public List<PlayerBest> bestsOrEmpty() {
        return bests == null ? List.of() : bests;
    }
}

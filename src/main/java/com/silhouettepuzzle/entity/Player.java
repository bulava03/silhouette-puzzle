package com.silhouettepuzzle.entity;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A registered player with their best solved attempt on every level they
 * built. {@code usernameKey} is the lower-cased name and is unique (see
 * {@code db/changelog}), so names that differ only in case clash.
 * {@code bests} is null in documents stored before there were any.
 */
@Document("players")
public record Player(
        @Id String id,
        String username,
        String usernameKey,
        String passwordHash,
        Instant createdAt,
        List<Attempt> bests) {

    public static String keyOf(String username) {
        return username.toLowerCase(Locale.ROOT);
    }

    public Optional<Attempt> bestOn(String levelId) {
        return bests == null ? Optional.empty()
                : bests.stream().filter(best -> best.levelId().equals(levelId)).findFirst();
    }
}

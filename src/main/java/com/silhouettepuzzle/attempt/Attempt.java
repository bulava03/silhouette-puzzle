package com.silhouettepuzzle.attempt;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.silhouettepuzzle.player.PlayerPrincipal;

/**
 * One try at a level, timed by the server. Attempts are temporary: a solved
 * one hands its result to the player's bests, and MongoDB removes every
 * attempt some time after it started. Anonymous attempts have no player.
 */
@Document("attempts")
public record Attempt(
        @Id String id,
        String levelId,
        String playerId,
        Instant startedAt,
        Instant solvedAt,
        Long timeMs,
        Integer moves) {

    public static Attempt start(String levelId, PlayerPrincipal player, Instant startedAt) {
        return new Attempt(null, levelId, player == null ? null : player.getId(), startedAt, null, null, null);
    }

    /** An anonymous attempt is open to anyone holding its id, a player's only to that player. */
    public boolean playableBy(PlayerPrincipal player) {
        return playerId == null || player != null && playerId.equals(player.getId());
    }
}

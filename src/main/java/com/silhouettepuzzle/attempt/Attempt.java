package com.silhouettepuzzle.attempt;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.silhouettepuzzle.player.PlayerPrincipal;

/**
 * One try at a level, timed by the server. Anonymous attempts have no
 * player and never appear in leaderboards or progress.
 *
 * @param id        attempt identifier, assigned by MongoDB
 * @param levelId   the level being played
 * @param playerId  the player, or null for an anonymous attempt
 * @param username  the player's name, copied for leaderboards; null when anonymous
 * @param startedAt when the server started the attempt
 * @param solvedAt  when the server accepted a solution; null while unsolved
 * @param timeMs    time from start to the accepted solution; null while unsolved
 * @param moves     moves reported by the client with the accepted solution
 */
@Document("attempts")
@CompoundIndex(name = "leaderboard", def = "{'levelId': 1, 'timeMs': 1, 'moves': 1}")
@CompoundIndex(name = "progress", def = "{'playerId': 1, 'levelId': 1}")
public record Attempt(
        @Id String id,
        String levelId,
        String playerId,
        String username,
        Instant startedAt,
        Instant solvedAt,
        Long timeMs,
        Integer moves) {

    /**
     * Creates a new, unsolved attempt.
     *
     * @param levelId   the level being played
     * @param player    the logged-in player, or null when anonymous
     * @param startedAt start time
     * @return the attempt, not yet stored
     */
    public static Attempt start(String levelId, PlayerPrincipal player, Instant startedAt) {
        return player == null
                ? new Attempt(null, levelId, null, null, startedAt, null, null, null)
                : new Attempt(null, levelId, player.getId(), player.getUsername(), startedAt, null, null, null);
    }

    /**
     * Tells whether a solution has been accepted.
     *
     * @return true once solved
     */
    public boolean solved() {
        return solvedAt != null;
    }

    /**
     * Tells whether a player may submit solutions for this attempt. An
     * anonymous attempt is open to anyone holding its id; a player's attempt
     * only to that player.
     *
     * @param player the current player, or null when anonymous
     * @return true if the submission is allowed
     */
    public boolean playableBy(PlayerPrincipal player) {
        return playerId == null || player != null && playerId.equals(player.getId());
    }
}

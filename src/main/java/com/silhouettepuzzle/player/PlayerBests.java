package com.silhouettepuzzle.player;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.time.Instant;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * Records solved runs into the players' bests.
 *
 * <p>Every step is a single conditional update, so concurrent solves of the
 * same player never lose a count, never add a level twice and never replace
 * a better run with a worse one.
 */
@Component
public class PlayerBests {

    private final MongoTemplate mongo;

    /**
     * Creates the recorder.
     *
     * @param mongo template for the conditional updates
     */
    public PlayerBests(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /**
     * Counts a solved run and keeps it if it beats the player's best:
     * faster, or as fast with fewer moves.
     *
     * @param playerId the player
     * @param levelId  the level
     * @param timeMs   server-measured time
     * @param moves    reported moves
     * @param solvedAt when it was solved
     */
    public void record(String playerId, String levelId, long timeMs, int moves, Instant solvedAt) {
        if (!countSolve(playerId, levelId)) {
            Query withoutLevel = query(where("_id").is(playerId).and("bests.levelId").ne(levelId));
            PlayerBest first = new PlayerBest(levelId, timeMs, moves, solvedAt, 1);
            if (mongo.updateFirst(withoutLevel, new Update().push("bests", first), Player.class).getMatchedCount() == 1) {
                return;
            }
            // A concurrent solve added the level between the two updates.
            countSolve(playerId, levelId);
        }
        Query worse = query(where("_id").is(playerId).and("bests").elemMatch(where("levelId").is(levelId)
                .orOperator(where("timeMs").gt(timeMs), where("timeMs").is(timeMs).and("moves").gt(moves))));
        Update better = new Update().set("bests.$.timeMs", timeMs).set("bests.$.moves", moves)
                .set("bests.$.solvedAt", solvedAt);
        mongo.updateFirst(worse, better, Player.class);
    }

    private boolean countSolve(String playerId, String levelId) {
        Query withLevel = query(where("_id").is(playerId).and("bests").elemMatch(where("levelId").is(levelId)));
        return mongo.updateFirst(withLevel, new Update().inc("bests.$.solves", 1), Player.class).getMatchedCount() == 1;
    }
}

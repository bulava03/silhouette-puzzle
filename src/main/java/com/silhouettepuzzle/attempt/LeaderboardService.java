package com.silhouettepuzzle.attempt;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.silhouettepuzzle.level.LevelNotFoundException;
import com.silhouettepuzzle.level.LevelRepository;

/**
 * Results built from solved attempts with MongoDB aggregations. Only
 * attempts of registered players count.
 */
@Service
public class LeaderboardService {

    /** Ranking order: faster first, then fewer moves, then whoever was first. */
    private static final Sort RANKING = Sort.by("timeMs", "moves", "solvedAt");

    private final MongoTemplate mongo;
    private final LevelRepository levels;

    /**
     * Creates the service.
     *
     * @param mongo  template for aggregations
     * @param levels level storage, to tell an empty leaderboard from a missing level
     */
    public LeaderboardService(MongoTemplate mongo, LevelRepository levels) {
        this.mongo = mongo;
        this.levels = levels;
    }

    /**
     * Returns the best run of each player on a level.
     *
     * @param levelId the level
     * @param size    how many places to return
     * @return entries, best first
     */
    public List<LeaderboardEntry> leaderboard(String levelId, int size) {
        if (!levels.existsById(levelId)) {
            throw new LevelNotFoundException(levelId);
        }
        Aggregation aggregation = newAggregation(
                match(Criteria.where("levelId").is(levelId).and("playerId").ne(null).and("solvedAt").ne(null)),
                sort(RANKING),
                // After the sort, the first run of each player is that player's best.
                group("playerId").first("username").as("username").first("timeMs").as("timeMs")
                        .first("moves").as("moves").first("solvedAt").as("solvedAt"),
                sort(RANKING),
                limit(size));
        List<BestRun> runs = mongo.aggregate(aggregation, Attempt.class, BestRun.class).getMappedResults();

        List<LeaderboardEntry> entries = new ArrayList<>(runs.size());
        for (int i = 0; i < runs.size(); i++) {
            BestRun run = runs.get(i);
            entries.add(new LeaderboardEntry(i + 1, run.username(), run.timeMs(), run.moves(), run.solvedAt()));
        }
        return entries;
    }

    /**
     * Returns a player's personal bests on every level they solved.
     *
     * @param playerId the player
     * @return one entry per solved level, ordered by level id
     */
    public List<ProgressEntry> progressOf(String playerId) {
        Aggregation aggregation = newAggregation(
                match(Criteria.where("playerId").is(playerId).and("solvedAt").ne(null)),
                group("levelId").count().as("solves").min("timeMs").as("bestTimeMs")
                        .min("moves").as("fewestMoves"),
                sort(Sort.by("_id")));
        return mongo.aggregate(aggregation, Attempt.class, LevelBest.class).getMappedResults().stream()
                .map(best -> new ProgressEntry(best.levelId(), best.solves(), best.bestTimeMs(), best.fewestMoves()))
                .toList();
    }

    /**
     * A player's best run, as the leaderboard aggregation returns it.
     *
     * @param playerId the player
     * @param username the player's name
     * @param timeMs   time of the run
     * @param moves    moves of the run
     * @param solvedAt when the run was solved
     */
    record BestRun(@Id String playerId, String username, long timeMs, int moves, Instant solvedAt) {
    }

    /**
     * A player's bests on one level, as the progress aggregation returns them.
     *
     * @param levelId     the level
     * @param solves      number of solved attempts
     * @param bestTimeMs  fastest time
     * @param fewestMoves fewest moves
     */
    record LevelBest(@Id String levelId, int solves, long bestTimeMs, int fewestMoves) {
    }
}

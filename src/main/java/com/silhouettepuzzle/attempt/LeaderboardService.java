package com.silhouettepuzzle.attempt;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.unwind;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.silhouettepuzzle.level.LevelNotFoundException;
import com.silhouettepuzzle.level.LevelRepository;
import com.silhouettepuzzle.player.Player;
import com.silhouettepuzzle.player.PlayerRepository;

/**
 * Results built from the players' bests.
 */
@Service
public class LeaderboardService {

    /** Ranking order: faster first, then fewer moves, then whoever was first. */
    private static final Sort RANKING = Sort.by("bests.timeMs", "bests.moves", "bests.solvedAt");

    private final MongoTemplate mongo;
    private final LevelRepository levels;
    private final PlayerRepository players;

    /**
     * Creates the service.
     *
     * @param mongo   template for aggregations
     * @param levels  level storage, to tell an empty leaderboard from a missing level
     * @param players player storage, for a player's own progress
     */
    public LeaderboardService(MongoTemplate mongo, LevelRepository levels, PlayerRepository players) {
        this.mongo = mongo;
        this.levels = levels;
        this.players = players;
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
        Criteria onLevel = Criteria.where("bests.levelId").is(levelId);
        Aggregation aggregation = newAggregation(
                // The first match uses the index to pick the players who have the level,
                // the second keeps only that level's entry of each after unwinding.
                match(onLevel),
                unwind("bests"),
                match(onLevel),
                sort(RANKING),
                limit(size),
                project("username").and("bests.timeMs").as("timeMs").and("bests.moves").as("moves")
                        .and("bests.solvedAt").as("solvedAt"));
        List<BestRun> runs = mongo.aggregate(aggregation, Player.class, BestRun.class).getMappedResults();

        List<LeaderboardEntry> entries = new ArrayList<>(runs.size());
        for (int i = 0; i < runs.size(); i++) {
            BestRun run = runs.get(i);
            entries.add(new LeaderboardEntry(i + 1, run.username(), run.timeMs(), run.moves(), run.solvedAt()));
        }
        return entries;
    }

    /**
     * Returns a player's best run on every level they solved.
     *
     * @param playerId the player
     * @return one entry per solved level, ordered by level id
     */
    public List<ProgressEntry> progressOf(String playerId) {
        return players.findById(playerId).map(Player::bestsOrEmpty).orElse(List.of()).stream()
                .map(best -> new ProgressEntry(best.levelId(), best.solves(), best.timeMs(), best.moves()))
                .sorted(Comparator.comparing(ProgressEntry::levelId))
                .toList();
    }

    /**
     * A player's best run on the level, as the aggregation returns it.
     *
     * @param username the player's name
     * @param timeMs   time of the run
     * @param moves    moves of the run
     * @param solvedAt when the run was solved
     */
    record BestRun(String username, long timeMs, int moves, Instant solvedAt) {
    }
}

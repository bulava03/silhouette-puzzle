package com.silhouettepuzzle.attempt;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.unwind;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.silhouettepuzzle.level.Level;
import com.silhouettepuzzle.level.LevelRepository;
import com.silhouettepuzzle.player.Player;

/**
 * Level leaderboards, aggregated from the players' bests.
 */
@Service
public class LeaderboardService {

    private final MongoTemplate mongo;
    private final LevelRepository levels;

    public LeaderboardService(MongoTemplate mongo, LevelRepository levels) {
        this.mongo = mongo;
        this.levels = levels;
    }

    /** Best run of each player on a level: faster first, then fewer moves, then who was first. */
    public List<LeaderboardEntry> leaderboard(String levelId, int size) {
        if (!levels.existsById(levelId)) {
            throw Level.notFound(levelId);
        }
        Criteria onLevel = Criteria.where("bests.levelId").is(levelId);
        List<Run> runs = mongo.aggregate(newAggregation(
                        // The first match picks players with the level by index,
                        // the second keeps only that level's entry after unwinding.
                        match(onLevel),
                        unwind("bests"),
                        match(onLevel),
                        sort(Sort.by("bests.timeMs", "bests.moves", "bests.solvedAt")),
                        limit(size),
                        project("username").and("bests.timeMs").as("timeMs").and("bests.moves").as("moves")
                                .and("bests.solvedAt").as("solvedAt")),
                Player.class, Run.class).getMappedResults();
        return IntStream.range(0, runs.size())
                .mapToObj(i -> new LeaderboardEntry(i + 1, runs.get(i).username(), runs.get(i).timeMs(),
                        runs.get(i).moves(), runs.get(i).solvedAt()))
                .toList();
    }

    /** One row of the aggregation result. */
    record Run(String username, long timeMs, int moves, Instant solvedAt) {
    }
}

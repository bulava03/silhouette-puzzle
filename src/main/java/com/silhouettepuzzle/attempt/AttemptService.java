package com.silhouettepuzzle.attempt;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.silhouettepuzzle.geometry.CheckResult;
import com.silhouettepuzzle.geometry.Placement;
import com.silhouettepuzzle.geometry.SolutionChecker;
import com.silhouettepuzzle.level.Level;
import com.silhouettepuzzle.level.LevelNotFoundException;
import com.silhouettepuzzle.level.LevelRepository;

/**
 * Starts attempts and checks submitted solutions. Time is measured on the
 * server only: the client never reports how long it took.
 */
@Service
public class AttemptService {

    private final AttemptRepository attempts;
    private final LevelRepository levels;
    private final MongoTemplate mongo;
    private final SolutionChecker checker;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param attempts attempt storage
     * @param levels   level storage
     * @param mongo    template for the conditional "mark solved" update
     * @param checker  solution checker
     * @param clock    source of the current time
     */
    public AttemptService(AttemptRepository attempts, LevelRepository levels, MongoTemplate mongo,
                          SolutionChecker checker, Clock clock) {
        this.attempts = attempts;
        this.levels = levels;
        this.mongo = mongo;
        this.checker = checker;
        this.clock = clock;
    }

    /**
     * Starts a timed attempt at a level.
     *
     * @param levelId the level to play
     * @return the stored attempt
     */
    public Attempt start(String levelId) {
        if (!levels.existsById(levelId)) {
            throw new LevelNotFoundException(levelId);
        }
        return attempts.save(Attempt.start(levelId, clock.instant()));
    }

    /**
     * Checks a submitted solution. An unsolved attempt stays open, so the
     * player can keep going; a solved one accepts no further submissions.
     *
     * @param attemptId  the attempt
     * @param placements placement of each level piece, in level order
     * @param moves      number of moves the client reports
     * @return the verdict, with the time if the solution was accepted
     */
    public SubmitResult submit(String attemptId, List<Placement> placements, int moves) {
        Attempt attempt = attempts.findById(attemptId).orElseThrow(() -> new AttemptNotFoundException(attemptId));
        if (attempt.solved()) {
            throw new AttemptAlreadySolvedException(attemptId);
        }
        Level level = levels.findById(attempt.levelId())
                .orElseThrow(() -> new LevelNotFoundException(attempt.levelId()));
        if (placements.size() != level.pieces().size()) {
            throw new InvalidSubmissionException(
                    "Level " + level.id() + " has " + level.pieces().size() + " pieces, got "
                            + placements.size() + " placements");
        }

        CheckResult result = checker.check(level.silhouette(), level.pieces(), placements);
        if (!result.solved()) {
            return SubmitResult.unsolved(result);
        }
        Instant now = clock.instant();
        long timeMs = Duration.between(attempt.startedAt(), now).toMillis();
        markSolved(attemptId, now, timeMs, moves);
        return SubmitResult.solved(result, timeMs, moves);
    }

    /**
     * Marks the attempt solved only if it is still unsolved, so of two
     * concurrent correct submissions exactly one is recorded.
     *
     * @param attemptId the attempt
     * @param solvedAt  when the solution was accepted
     * @param timeMs    time from start to the solution
     * @param moves     reported moves
     */
    private void markSolved(String attemptId, Instant solvedAt, long timeMs, int moves) {
        Query unsolved = Query.query(Criteria.where("_id").is(attemptId).and("solvedAt").isNull());
        Update update = new Update().set("solvedAt", solvedAt).set("timeMs", timeMs).set("moves", moves);
        if (mongo.updateFirst(unsolved, update, Attempt.class).getModifiedCount() == 0) {
            throw new AttemptAlreadySolvedException(attemptId);
        }
    }
}

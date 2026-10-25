package com.silhouettepuzzle.service;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.addFields;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.unwind;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.silhouettepuzzle.ApiException;
import com.silhouettepuzzle.dto.RegisterForm;
import com.silhouettepuzzle.entity.Attempt;
import com.silhouettepuzzle.entity.Player;
import com.silhouettepuzzle.repository.PlayerRepository;

/**
 * Players and their results: registration, each player's best attempt per
 * level, and the leaderboards built from those bests.
 */
@Service
public class PlayerService {

    /** BCrypt only looks at the first 72 bytes of a password. */
    private static final int MAX_PASSWORD_BYTES = 72;

    private final PlayerRepository players;
    private final MongoTemplate mongo;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public PlayerService(PlayerRepository players, MongoTemplate mongo, PasswordEncoder encoder, Clock clock) {
        this.players = players;
        this.mongo = mongo;
        this.encoder = encoder;
        this.clock = clock;
    }

    public Player register(RegisterForm form) {
        if (form.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw ApiException.badRequest("Password must be at most 72 bytes in UTF-8");
        }
        String key = Player.keyOf(form.username());
        if (players.existsByUsernameKey(key)) {
            throw taken(form.username());
        }
        try {
            return players.save(new Player(null, form.username(), key, encoder.encode(form.password()),
                    clock.instant(), List.of()));
        } catch (DuplicateKeyException e) {
            // Someone registered the same name between the check and the insert.
            throw taken(form.username());
        }
    }

    public Optional<Player> find(String id) {
        return players.findById(id);
    }

    /**
     * Keeps a solved attempt as the player's best on its level if there is
     * none yet or it beats the current one. Both steps are conditional
     * updates, so concurrent solves never add a level twice or replace a
     * better attempt with a worse one.
     */
    public void recordBest(String playerId, Attempt solved) {
        Attempt best = new Attempt(solved.id(), solved.levelId(), null, solved.startedAt(), solved.solvedAt(),
                solved.timeMs());
        boolean added = mongo.updateFirst(
                query(where("_id").is(playerId).and("bests.levelId").ne(solved.levelId())),
                new Update().push("bests", best), Player.class).getMatchedCount() == 1;
        if (!added) {
            mongo.updateFirst(query(where("_id").is(playerId).and("bests").elemMatch(beatenBy(solved))),
                    new Update().set("bests.$", best), Player.class);
        }
    }

    /** Players with a best on the level, each carrying only that best, in ranking order. */
    public List<Player> leaderboard(String levelId, int size) {
        Criteria onLevel = where("bests.levelId").is(levelId);
        return mongo.aggregate(newAggregation(
                        // The first match picks players with the level by index,
                        // the second keeps only that level's best after unwinding.
                        match(onLevel),
                        unwind("bests"),
                        match(onLevel),
                        sort(Sort.by("bests.timeMs", "bests.solvedAt")),
                        limit(size),
                        addFields().addFieldWithValue("bests", List.of("$bests")).build(),
                        project("username", "bests")),
                Player.class, Player.class).getMappedResults();
    }

    /** The place an attempt would take on its level's leaderboard. */
    public int rankOf(Attempt attempt) {
        return (int) mongo.count(query(where("bests").elemMatch(beating(attempt))), Player.class) + 1;
    }

    /** Bests on the attempt's level that are worse than it. */
    private static Criteria beatenBy(Attempt attempt) {
        return where("levelId").is(attempt.levelId()).orOperator(where("timeMs").gt(attempt.timeMs()),
                where("timeMs").is(attempt.timeMs()).and("solvedAt").gt(attempt.solvedAt()));
    }

    /** Bests on the attempt's level that are better than it. */
    private static Criteria beating(Attempt attempt) {
        return where("levelId").is(attempt.levelId()).orOperator(where("timeMs").lt(attempt.timeMs()),
                where("timeMs").is(attempt.timeMs()).and("solvedAt").lt(attempt.solvedAt()));
    }

    private static ApiException taken(String username) {
        return ApiException.conflict("Username is taken: " + username);
    }
}

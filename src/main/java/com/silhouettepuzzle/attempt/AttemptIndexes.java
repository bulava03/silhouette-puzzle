package com.silhouettepuzzle.attempt;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Lets MongoDB remove old attempts by itself with a TTL index.
 *
 * <p>An attempt can be submitted for {@code app.attempts.lifetime} after it
 * started; MongoDB removes it after twice that time. For the period in
 * between the server still finds the attempt and can say it has expired,
 * rather than that it never existed.
 */
@Component
@Order(0)
public class AttemptIndexes implements ApplicationRunner {

    /** Name of the TTL index on the start time. */
    static final String EXPIRY_INDEX = "attempt-expiry";

    private final MongoTemplate mongo;
    private final Duration lifetime;

    /**
     * Creates the index setup.
     *
     * @param mongo    template for index operations
     * @param lifetime how long an attempt can be submitted
     */
    public AttemptIndexes(MongoTemplate mongo, @Value("${app.attempts.lifetime}") Duration lifetime) {
        this.mongo = mongo;
        this.lifetime = lifetime;
    }

    @Override
    public void run(ApplicationArguments args) {
        mongo.indexOps(Attempt.class).createIndex(new Index("startedAt", Sort.Direction.ASC)
                .named(EXPIRY_INDEX)
                .expire(lifetime.multipliedBy(2)));
    }
}

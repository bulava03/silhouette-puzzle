package com.silhouettepuzzle.attempt;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * A TTL index that lets MongoDB remove attempts twice their lifetime after
 * they start; in between, a late submission still finds its attempt and is
 * told it expired rather than that it never existed.
 */
@Component
public class AttemptIndexes implements ApplicationRunner {

    static final String EXPIRY_INDEX = "attempt-expiry";

    private final MongoTemplate mongo;
    private final Duration lifetime;

    public AttemptIndexes(MongoTemplate mongo, @Value("${app.attempts.lifetime}") Duration lifetime) {
        this.mongo = mongo;
        this.lifetime = lifetime;
    }

    @Override
    public void run(ApplicationArguments args) {
        mongo.indexOps(Attempt.class).createIndex(new Index("startedAt", Sort.Direction.ASC)
                .named(EXPIRY_INDEX).expire(lifetime.multipliedBy(2)));
    }
}

package com.silhouettepuzzle;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * Shared setup for integration tests: MongoDB in a container, from the same
 * image that docker-compose uses, and a clock the tests can move forward.
 * Every integration test imports this one class, so they all share a single
 * application context and a single container.
 */
@TestConfiguration(proxyBeanMethods = false)
public class IntegrationTestConfig {

    /** The image shared with docker-compose.yml. */
    public static final String MONGO_IMAGE = "mongo:7";

    @Bean
    @ServiceConnection
    MongoDBContainer mongoContainer() {
        return new MongoDBContainer(MONGO_IMAGE);
    }

    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock(Instant.parse("2026-01-01T12:00:00Z"));
    }

    /**
     * A clock that stands still until a test advances it.
     */
    public static final class MutableClock extends Clock {

        private Instant now;
        private boolean ticking;

        MutableClock(Instant start) {
            this.now = start;
        }

        /**
         * Moves the clock forward.
         *
         * @param duration how far
         */
        public synchronized void advance(Duration duration) {
            now = now.plus(duration);
        }

        /** Makes every reading a millisecond later than the one before, until switched off. */
        public synchronized void tickOnEveryRead(boolean on) {
            ticking = on;
        }

        @Override
        public synchronized Instant instant() {
            if (ticking) {
                now = now.plusMillis(1);
            }
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }
    }
}

package com.silhouettepuzzle;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * A MongoDB container for integration tests, from the same image that
 * docker-compose uses. Import it with {@code @Import(MongoTestContainer.class)}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MongoTestContainer {

    /** The image shared with docker-compose.yml. */
    public static final String IMAGE = "mongo:7";

    @Bean
    @ServiceConnection
    MongoDBContainer mongoContainer() {
        return new MongoDBContainer(IMAGE);
    }
}

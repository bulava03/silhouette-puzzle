package com.silhouettepuzzle.config;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.silhouettepuzzle.geometry.SolutionChecker;

/**
 * Beans of the game logic that is kept free of Spring.
 */
@Configuration(proxyBeanMethods = false)
public class GameConfig {

    /**
     * The solution checker with the configured tolerance.
     *
     * @param tolerance allowed overlap and mismatch, as a fraction of the silhouette area
     * @return the checker
     */
    @Bean
    public SolutionChecker solutionChecker(@Value("${app.solution.tolerance}") double tolerance) {
        return new SolutionChecker(tolerance);
    }

    /**
     * The clock attempts are timed with; replaced by a fixed clock in tests.
     *
     * @return the system clock in UTC
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

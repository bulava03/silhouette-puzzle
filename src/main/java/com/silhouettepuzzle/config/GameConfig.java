package com.silhouettepuzzle.config;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.silhouettepuzzle.geometry.PuzzleGeometry;

/**
 * Beans for the game: the geometry, which itself stays free of Spring, and
 * the clock attempts are timed with, so tests can control time.
 */
@Configuration(proxyBeanMethods = false)
public class GameConfig {

    @Bean
    public PuzzleGeometry puzzleGeometry(@Value("${app.solution.tolerance}") double tolerance) {
        return new PuzzleGeometry(tolerance);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

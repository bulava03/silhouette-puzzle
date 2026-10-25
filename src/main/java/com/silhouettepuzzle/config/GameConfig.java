package com.silhouettepuzzle.config;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.silhouettepuzzle.geometry.SolutionChecker;

/**
 * Beans for the game logic, which itself stays free of Spring. The clock is
 * a bean so tests can control time.
 */
@Configuration(proxyBeanMethods = false)
public class GameConfig {

    @Bean
    public SolutionChecker solutionChecker(@Value("${app.solution.tolerance}") double tolerance) {
        return new SolutionChecker(tolerance);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

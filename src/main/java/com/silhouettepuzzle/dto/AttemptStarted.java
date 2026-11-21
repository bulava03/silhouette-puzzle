package com.silhouettepuzzle.dto;

/**
 * A timed attempt the server has just started for a logged-in player.
 */
public record AttemptStarted(String attemptId) {
}

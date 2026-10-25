package com.silhouettepuzzle.web;

/**
 * A leaderboard row as the page shows it.
 *
 * @param rank     place, starting at 1
 * @param username the player
 * @param time     best time, like {@code 01:07}
 * @param moves    moves of that run
 * @param date     day of that run, like {@code 25 Oct}
 * @param me       whether this is the logged-in player
 */
public record ResultRow(int rank, String username, String time, int moves, String date, boolean me) {
}

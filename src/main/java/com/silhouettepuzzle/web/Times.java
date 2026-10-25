package com.silhouettepuzzle.web;

/**
 * Formats durations for the pages.
 */
final class Times {

    private Times() {
    }

    /**
     * Formats a duration as minutes and seconds, like {@code 01:07}.
     *
     * @param millis the duration in milliseconds
     * @return the formatted time
     */
    static String clock(long millis) {
        long seconds = millis / 1000;
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}

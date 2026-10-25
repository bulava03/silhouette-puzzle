package com.silhouettepuzzle.web;

/**
 * A level in a row of tabs.
 *
 * @param id      level identifier
 * @param label   number and name, like {@code 01 Bus}
 * @param accent  the level's colour
 * @param current whether this is the level on screen
 */
public record LevelTab(String id, String label, String accent, boolean current) {
}

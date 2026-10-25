package com.silhouettepuzzle.level;

/**
 * A level in the level list.
 *
 * @param id     level identifier
 * @param name   display name
 * @param number position in the level list
 */
public record LevelSummary(String id, String name, int number) {
}

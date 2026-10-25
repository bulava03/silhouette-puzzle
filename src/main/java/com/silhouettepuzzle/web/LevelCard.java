package com.silhouettepuzzle.web;

import java.util.List;

/**
 * A level on the levels page.
 *
 * @param id         level identifier
 * @param number     position, zero-padded like {@code 01}
 * @param name       display name
 * @param parts      how many parts the picture has
 * @param accent     colour of the silhouette edge
 * @param viewBox    SVG viewBox framing the silhouette
 * @param silhouette SVG points of each silhouette part
 * @param solved     whether the logged-in player has built it
 * @param best       the player's best time, or null
 * @param moves      the player's fewest moves, or null
 */
public record LevelCard(String id, String number, String name, int parts, String accent, String viewBox,
                        List<String> silhouette, boolean solved, String best, Integer moves) {
}

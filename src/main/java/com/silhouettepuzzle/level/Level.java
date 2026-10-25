package com.silhouettepuzzle.level;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.silhouettepuzzle.geometry.Outline;
import com.silhouettepuzzle.geometry.Placement;

/**
 * A puzzle level as stored in MongoDB.
 *
 * @param id         stable identifier, also used in URLs
 * @param name       display name
 * @param number     position in the level list, starting at 1
 * @param silhouette parts of the figure to assemble, in board coordinates
 * @param pieces     piece outlines in their own coordinates
 * @param solution   reference placement of each piece; never sent to clients
 */
@Document("levels")
public record Level(
        @Id String id,
        String name,
        int number,
        List<Outline> silhouette,
        List<Outline> pieces,
        List<Placement> solution) {
}

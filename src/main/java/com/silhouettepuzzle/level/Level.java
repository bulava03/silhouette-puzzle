package com.silhouettepuzzle.level;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.silhouettepuzzle.geometry.Outline;
import com.silhouettepuzzle.geometry.Placement;

/**
 * A puzzle level as stored in MongoDB: a picture to assemble from its own parts.
 *
 * @param id         stable identifier, also used in URLs
 * @param name       display name
 * @param number     position in the level list, starting at 1
 * @param silhouette parts of the figure to assemble, in board coordinates
 * @param pieces     the parts, each around its own origin
 * @param solution   reference placement of each piece; never sent to clients
 */
@Document("levels")
public record Level(
        @Id String id,
        String name,
        int number,
        List<Outline> silhouette,
        List<Piece> pieces,
        List<Placement> solution) {

    /**
     * Returns the piece shapes in level order, as the solution checker takes them.
     *
     * @return one outline per piece
     */
    public List<Outline> pieceOutlines() {
        return pieces.stream().map(Piece::outline).toList();
    }
}

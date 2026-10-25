package com.silhouettepuzzle.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.silhouettepuzzle.geometry.Outline;
import com.silhouettepuzzle.geometry.Placement;

/**
 * A picture to assemble from its own parts: the silhouette on the board,
 * the parts around their own origins, and the reference solution, which
 * stays on the server.
 */
@Document("levels")
public record Level(
        @Id String id,
        String name,
        int number,
        List<Outline> silhouette,
        List<Piece> pieces,
        List<Placement> solution) {

    public List<Outline> pieceOutlines() {
        return pieces.stream().map(Piece::outline).toList();
    }
}

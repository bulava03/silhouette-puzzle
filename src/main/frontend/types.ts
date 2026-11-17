// What the server sends and expects, mirroring the Java records of the same
// names: the level the game page carries and the check endpoint's body and verdict.

/** A point on the board; x grows to the right, y downwards, as in SVG. */
export interface Point {
    x: number;
    y: number;
}

export interface Outline {
    vertices: Point[];
}

export interface Piece {
    name: string;
    /** #RRGGBB */
    color: string;
    /** The part's shape around its own origin. */
    outline: Outline;
    /** Degrees, a whole 45° step. */
    startRotation: number;
}

/** A level as the game page carries it: never the reference solution. */
export interface Level {
    silhouette: Outline[];
    pieces: Piece[];
}

/** Where a part lies, in silhouette coordinates. */
export interface Placement {
    x: number;
    y: number;
    rotation: number;
    flipped: boolean;
}

export interface SubmitRequest {
    attemptId: string | null;
    placements: Placement[];
}

export interface SubmitResult {
    solved: boolean;
    /** How much misses the silhouette or overlaps, as a share of its area. */
    off: number;
    /** Server-measured time of a solved timed attempt. */
    timeMs?: number;
    /** The player's place on the leaderboard after a solved timed attempt. */
    rank?: number;
}

// What the server sends and expects, mirroring the Java records of the same
// names: levels, attempts, and the check endpoint's body and verdict.

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

/** A level as the game gets it: never the reference solution. */
export interface Level {
    id: string;
    name: string;
    number: number;
    /** The colour of the silhouette's edge. */
    accent: string;
    /** The id of the level after this one. */
    next: string;
    silhouette: Outline[];
    pieces: Piece[];
}

/** One level in the level tabs. */
export interface LevelTab {
    id: string;
    name: string;
    number: number;
    accent: string;
}

export interface AttemptStarted {
    attemptId: string;
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

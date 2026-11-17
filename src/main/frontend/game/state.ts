// Everything the game page shows, and every way it can change. Pure, so the
// rules of play can be tested without a browser.
import { flipped, snap, turned, type Layout, type Pose, type Vec } from "../geometry";
import type { Level, SubmitResult } from "../types";

/** What the last check said, if anything is shown. */
export type Result =
    | { kind: "solved"; timeMs: number; moves: number; rank?: number }
    | { kind: "missed"; percentOff: number }
    | { kind: "expired" }
    | { kind: "error"; message: string };

/** A short glow where a dropped part snapped. */
export interface Flash {
    id: number;
    at: Vec;
    colour: string;
}

export interface GameState {
    level: Level;
    layout: Layout;
    pieces: Pose[];
    /** Drawing order, bottom first: a part comes on top when picked up and stays there. */
    order: number[];
    selected: number | null;
    /** The part being dragged and where on it the pointer holds it. */
    drag: { i: number; dx: number; dy: number; moved: boolean } | null;
    moves: number;
    /** The logged-in player's timed attempt; null for a guest or once it expired. */
    attemptId: string | null;
    solved: boolean;
    /** The time the clock stopped at, once the picture is solved or the attempt expired. */
    stoppedAt: number | null;
    busy: boolean;
    result: Result | null;
    flashes: Flash[];
}

export type Action =
    | { type: "select"; i: number | null }
    | { type: "grab"; i: number; at: Vec }
    | { type: "drag"; at: Vec }
    | { type: "drop" }
    | { type: "turn"; direction: 1 | -1 }
    | { type: "flip"; axis: "horizontal" | "vertical" }
    | { type: "fade"; id: number }
    | { type: "checking" }
    | { type: "checked"; result: SubmitResult; elapsedMs: number }
    | { type: "expired"; elapsedMs: number }
    | { type: "failed"; message: string };

/** How far from the board's edge a dragged part's origin has to stay. */
const EDGE = 0.2;

export function initialState(level: Level, layout: Layout, attemptId: string | null): GameState {
    return {
        level,
        layout,
        pieces: layout.start.map((p) => ({ ...p })),
        order: layout.start.map((_, i) => i),
        selected: null,
        drag: null,
        moves: 0,
        attemptId,
        solved: false,
        stoppedAt: null,
        busy: false,
        result: null,
        flashes: [],
    };
}

/** Pulls part i onto a nearby corner, if one is close, and counts the move. */
function commit(state: GameState, i: number, pieces: Pose[]): GameState {
    const pull = snap(i, pieces, state.layout);
    let flash = state.flashes;
    if (pull !== null) {
        pieces = pieces.map((p, j) => (j === i ? { ...p, x: p.x + pull.dx, y: p.y + pull.dy } : p));
        // Named after the move it ends, which no other move shares.
        flash = [...flash, { id: state.moves + 1, at: pull.at, colour: state.level.pieces[i].color }];
    }
    return { ...state, pieces, flashes: flash, moves: state.moves + 1, result: null };
}

function toTop(order: number[], i: number): number[] {
    return [...order.filter((j) => j !== i), i];
}

function reorient(state: GameState, change: (p: Pose) => Pose): GameState {
    const i = state.selected;
    if (i === null || state.solved) return state;
    return commit(state, i, state.pieces.map((p, j) => (j === i ? change(p) : p)));
}

export function reduce(state: GameState, action: Action): GameState {
    switch (action.type) {
        case "select":
            return { ...state, selected: action.i, order: action.i === null ? state.order : toTop(state.order, action.i) };
        case "grab": {
            if (state.solved) return state;
            const p = state.pieces[action.i];
            return {
                ...state,
                selected: action.i,
                order: toTop(state.order, action.i),
                drag: { i: action.i, dx: action.at[0] - p.x, dy: action.at[1] - p.y, moved: false },
            };
        }
        case "drag": {
            const drag = state.drag;
            if (drag === null) return state;
            const { width, height } = state.layout;
            const x = Math.min(width - EDGE, Math.max(EDGE, action.at[0] - drag.dx));
            const y = Math.min(height - EDGE, Math.max(EDGE, action.at[1] - drag.dy));
            return {
                ...state,
                pieces: state.pieces.map((p, j) => (j === drag.i ? { ...p, x, y } : p)),
                drag: { ...drag, moved: true },
            };
        }
        case "drop": {
            const drag = state.drag;
            if (drag === null) return state;
            const dropped = { ...state, drag: null };
            return drag.moved ? commit(dropped, drag.i, dropped.pieces) : dropped;
        }
        case "turn":
            return reorient(state, (p) => turned(p, action.direction));
        case "flip":
            return reorient(state, (p) => flipped(p, action.axis));
        case "fade":
            return { ...state, flashes: state.flashes.filter((f) => f.id !== action.id) };
        case "checking":
            return { ...state, busy: true, selected: null };
        case "checked": {
            const { result } = action;
            if (result.solved) {
                // A player's time comes from the server; a guest only has the page's clock.
                const timeMs = result.timeMs ?? action.elapsedMs;
                return {
                    ...state,
                    busy: false,
                    solved: true,
                    stoppedAt: timeMs,
                    result: { kind: "solved", timeMs, moves: state.moves, rank: result.rank },
                };
            }
            // Parts lying outside count as well as gaps, so the server's figure can pass 1.
            const percentOff = Math.min(100, Math.max(1, Math.round(result.off * 100)));
            return { ...state, busy: false, result: { kind: "missed", percentOff } };
        }
        case "expired":
            return { ...state, busy: false, attemptId: null, stoppedAt: action.elapsedMs, result: { kind: "expired" } };
        case "failed":
            return { ...state, busy: false, result: { kind: "error", message: action.message } };
    }
}

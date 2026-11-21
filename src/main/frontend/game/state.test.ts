import { describe, expect, it } from "vitest";
import type { Level } from "../types";
import { initialState, reduce, type Action, type GameState } from "./state";

const bar = { vertices: [{ x: 0, y: 0 }, { x: 1, y: 0 }, { x: 1, y: 2 }, { x: 0, y: 2 }] };
const level: Level = {
    id: "square", name: "Square", number: 1, accent: "#6BD8DE", next: "other",
    silhouette: [{ vertices: [{ x: 0, y: 0 }, { x: 2, y: 0 }, { x: 2, y: 2 }, { x: 0, y: 2 }] }],
    pieces: [
        { name: "Left", color: "#FF0000", outline: bar, startRotation: 0 },
        { name: "Right", color: "#00FF00", outline: bar, startRotation: 0 },
    ],
};

const other: Level = { ...level, id: "other", name: "Other", next: "square", pieces: level.pieces.slice(0, 1) };

function play(...actions: Action[]): GameState {
    return actions.reduce(reduce, initialState(level, "attempt-1", 1_000));
}

describe("the game", () => {
    it("brings a picked-up part on top and keeps it there after letting go", () => {
        const state = play({ type: "select", i: 0 }, { type: "select", i: null });
        expect(state.order).toEqual([1, 0]);
        expect(state.selected).toBeNull();
    });

    it("drags a part, snaps it onto the silhouette's corner and counts one move", () => {
        const start = play();
        const { offset } = start.layout;
        const from = start.pieces[0];
        const state = play(
            { type: "grab", i: 0, at: [from.x, from.y] },
            { type: "drag", at: [offset.x + 0.1, offset.y + 0.1] },
            { type: "drop" },
        );
        expect(state.pieces[0].x).toBeCloseTo(offset.x, 9);
        expect(state.pieces[0].y).toBeCloseTo(offset.y, 9);
        expect(state.moves).toBe(1);
        expect(state.flashes).toHaveLength(1);
        expect(state.drag).toBeNull();
    });

    it("does not count a click without a drag as a move", () => {
        const from = play().pieces[0];
        const state = play({ type: "grab", i: 0, at: [from.x, from.y] }, { type: "drop" });
        expect(state.moves).toBe(0);
        expect(state.selected).toBe(0);
    });

    it("turns and flips only a selected part, each a move", () => {
        expect(play({ type: "turn", direction: 1 }).moves).toBe(0);
        const state = play({ type: "select", i: 1 }, { type: "turn", direction: 1 }, { type: "flip", axis: "horizontal" });
        expect(state.pieces[1]).toMatchObject({ r: 315, f: true });
        expect(state.moves).toBe(2);
    });

    it("hides the last verdict once the player moves again", () => {
        const state = play(
            { type: "checking" },
            { type: "checked", round: 0, result: { solved: false, off: 0.42 }, at: 2_000 },
        );
        expect(state.result).toEqual({ kind: "missed", percentOff: 42 });
        expect(play(
            { type: "checked", round: 0, result: { solved: false, off: 0.42 }, at: 2_000 },
            { type: "select", i: 0 },
            { type: "turn", direction: 1 },
        ).result).toBeNull();
    });

    it("keeps the off share between 1% and 100%", () => {
        const off = (share: number) => play({ type: "checked", round: 0, result: { solved: false, off: share }, at: 1_000 }).result;
        expect(off(0.001)).toEqual({ kind: "missed", percentOff: 1 });
        expect(off(1.7)).toEqual({ kind: "missed", percentOff: 100 });
    });

    it("stops on a solved picture with the server's time, and locks the parts", () => {
        const state = play(
            { type: "checking" },
            { type: "checked", round: 0, result: { solved: true, off: 0, timeMs: 48_000, rank: 3 }, at: 1_000 + 50_000 },
        );
        expect(state.solved).toBe(true);
        expect(state.stoppedAt).toBe(48_000);
        expect(state.result).toEqual({ kind: "solved", timeMs: 48_000, moves: 0, rank: 3 });
        const from = state.pieces[0];
        expect(reduce(state, { type: "grab", i: 0, at: [from.x, from.y] }).drag).toBeNull();
    });

    it("times a guest's picture by the page's clock", () => {
        const state = play({ type: "checked", round: 0, result: { solved: true, off: 0 }, at: 1_000 + 50_000 });
        expect(state.stoppedAt).toBe(50_000);
    });

    it("drops an expired attempt so later checks go without it", () => {
        const state = play({ type: "checking" }, { type: "expired", round: 0, at: 1_000 + 9_000 });
        expect(state.attemptId).toBeNull();
        expect(state.stoppedAt).toBe(9_000);
        expect(state.result).toEqual({ kind: "expired" });
        expect(state.busy).toBe(false);
    });

    it("starts another level afresh, with its own clock and round", () => {
        const state = play(
            { type: "select", i: 0 },
            { type: "turn", direction: 1 },
            { type: "start", level: other, attemptId: "attempt-2", at: 5_000 },
        );
        expect(state.level.id).toBe("other");
        expect(state.pieces).toHaveLength(1);
        expect(state).toMatchObject({ round: 1, startedAt: 5_000, attemptId: "attempt-2", moves: 0, selected: null });
        expect(state.result).toBeNull();
    });

    it("drops a verdict that comes back for a level the player has left", () => {
        const state = play(
            { type: "checking" },
            { type: "start", level: other, attemptId: null, at: 5_000 },
            { type: "checked", round: 0, result: { solved: true, off: 0, timeMs: 3_000 }, at: 6_000 },
        );
        expect(state.solved).toBe(false);
        expect(state.result).toBeNull();
    });
});

import { describe, expect, it } from "vitest";
import { boundsOf, clock, flipped, layOut, snap, toPlacement, toWorld, turned, type Pose, type Vec } from "./geometry";
import type { Level } from "./types";

const square = (size: number) => ({
    vertices: [{ x: 0, y: 0 }, { x: size, y: 0 }, { x: size, y: size }, { x: 0, y: size }],
});

/** A 2×2 silhouette made of two 1×2 parts, one of them handed out turned. */
const level: Level = {
    silhouette: [square(2)],
    pieces: [
        { name: "Left", color: "#FF0000", outline: { vertices: [{ x: 0, y: 0 }, { x: 1, y: 0 }, { x: 1, y: 2 }, { x: 0, y: 2 }] }, startRotation: 0 },
        { name: "Right", color: "#00FF00", outline: { vertices: [{ x: 0, y: 0 }, { x: 1, y: 0 }, { x: 1, y: 2 }, { x: 0, y: 2 }] }, startRotation: 90 },
    ],
};

const near = (actual: Vec, expected: Vec) => {
    expect(actual[0]).toBeCloseTo(expected[0], 9);
    expect(actual[1]).toBeCloseTo(expected[1], 9);
};

describe("toWorld", () => {
    it("mirrors, then turns clockwise on screen, then moves, as the server does", () => {
        near(toWorld([1, 0], { x: 0, y: 0, r: 90, f: false }), [0, 1]);
        near(toWorld([1, 0], { x: 0, y: 0, r: 0, f: true }), [-1, 0]);
        near(toWorld([1, 0], { x: 0, y: 0, r: 90, f: true }), [0, -1]);
        near(toWorld([1, 2], { x: 5, y: 7, r: 0, f: false }), [6, 9]);
    });
});

describe("turning and flipping", () => {
    const pose: Pose = { x: 0, y: 0, r: 45, f: false };

    it("turns in 45° steps and wraps around", () => {
        expect(turned(pose, 1).r).toBe(90);
        expect(turned({ ...pose, r: 0 }, -1).r).toBe(315);
        expect(turned({ ...pose, r: 315 }, 1).r).toBe(0);
    });

    it("mirrors left to right on screen whatever the turn", () => {
        const before = toWorld([1, 0], pose);
        const after = toWorld([1, 0], flipped(pose, "horizontal"));
        near(after, [-before[0], before[1]]);
    });

    it("mirrors top to bottom on screen whatever the turn", () => {
        const before = toWorld([1, 0], pose);
        const after = toWorld([1, 0], flipped(pose, "vertical"));
        near(after, [before[0], -before[1]]);
    });
});

describe("layOut", () => {
    const layout = layOut(level);

    it("puts every part in the tray, right of the silhouette and inside the board", () => {
        const silhouette = boundsOf(layout.silhouette.flat());
        layout.start.forEach((pose, i) => {
            const part = boundsOf(layout.shapes[i].map((v) => toWorld(v, pose)));
            expect(part.minX).toBeGreaterThan(silhouette.maxX);
            expect(part.maxX).toBeLessThanOrEqual(layout.width);
            expect(part.minY).toBeGreaterThanOrEqual(0);
            expect(part.maxY).toBeLessThanOrEqual(layout.height);
        });
    });

    it("hands parts out at their start rotation, unflipped", () => {
        expect(layout.start.map((p) => [p.r, p.f])).toEqual([[0, false], [90, false]]);
    });

    it("converts board poses back to silhouette coordinates", () => {
        const pose = { x: layout.offset.x + 1, y: layout.offset.y, r: 90, f: true };
        expect(toPlacement(pose, layout)).toEqual({ x: 1, y: 0, rotation: 90, flipped: true });
    });
});

describe("snap", () => {
    const layout = layOut(level);
    const at = (x: number, y: number): Pose => ({ x: layout.offset.x + x, y: layout.offset.y + y, r: 0, f: false });

    it("pulls a corner that comes close onto the nearest corner", () => {
        const pieces = [at(0.1, 0), layout.start[1]];
        const pull = snap(0, pieces, layout)!;
        expect(pull.dx).toBeCloseTo(-0.1, 9);
        expect(pull.dy).toBeCloseTo(0, 9);
        // The part is as tall as the silhouette: its top and bottom corners come equally close.
        expect(pull.at[0]).toBeCloseTo(layout.offset.x, 9);
    });

    it("snaps to the corners of other parts too", () => {
        // Both below the silhouette, so only the other part's corners are near.
        const pieces = [at(0, 3), at(1.1, 3)];
        const pull = snap(1, pieces, layout)!;
        expect(pull.dx).toBeCloseTo(-0.1, 9);
        expect(pull.dy).toBeCloseTo(0, 9);
        near(pull.at, [layout.offset.x + 1, layout.offset.y + 3]);
    });

    it("leaves a part alone when no corner is close", () => {
        expect(snap(0, [at(0.5, 0.5), layout.start[1]], layout)).toBeNull();
    });
});

describe("clock", () => {
    it("shows minutes and seconds", () => {
        expect(clock(0)).toBe("00:00");
        expect(clock(67_999)).toBe("01:07");
        expect(clock(600_000)).toBe("10:00");
    });
});

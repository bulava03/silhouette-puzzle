// The board's geometry, free of the DOM: where parts lie, how they start in
// the tray and how a dropped part snaps to a nearby corner.
import type { Level, Outline, Placement } from "./types";

export type Vec = [number, number];

/** A part on the board: position, turn in degrees (clockwise on screen) and mirroring. */
export interface Pose {
    x: number;
    y: number;
    r: number;
    f: boolean;
}

export interface Bounds {
    minX: number;
    minY: number;
    maxX: number;
    maxY: number;
}

export interface Layout {
    width: number;
    height: number;
    /** From silhouette coordinates to board coordinates. */
    offset: { x: number; y: number };
    /** The silhouette parts on the board. */
    silhouette: Vec[][];
    /** Each part's shape around its own origin. */
    shapes: Vec[][];
    /** Each part's pose in the tray at the start. */
    start: Pose[];
}

export interface Snap {
    dx: number;
    dy: number;
    /** The corner the part snapped to. */
    at: Vec;
}

/** How close, in board units, a corner has to come to snap. */
export const SNAP = 0.25;
/** One turn, in degrees. */
export const STEP = 45;
const MARGIN = 1;
const TRAY_WIDTH = 11.2;
const TRAY_GAP = 0.4;

/**
 * Same order as the server and the SVG transform: mirror, rotate
 * (clockwise on screen), move.
 */
export function toWorld(point: Vec, p: Pose): Vec {
    const x = p.f ? -point[0] : point[0];
    const a = (p.r * Math.PI) / 180;
    return [x * Math.cos(a) - point[1] * Math.sin(a) + p.x, x * Math.sin(a) + point[1] * Math.cos(a) + p.y];
}

/** The SVG transform that draws a part at its pose. */
export function transformOf(p: Pose): string {
    return "translate(" + p.x + " " + p.y + ") rotate(" + p.r + ")" + (p.f ? " scale(-1 1)" : "");
}

export function verticesOf(outline: Outline): Vec[] {
    return outline.vertices.map((v) => [v.x, v.y]);
}

export function boundsOf(points: Vec[]): Bounds {
    const xs = points.map((p) => p[0]);
    const ys = points.map((p) => p[1]);
    return { minX: Math.min(...xs), minY: Math.min(...ys), maxX: Math.max(...xs), maxY: Math.max(...ys) };
}

/**
 * Puts the silhouette on the left and packs the parts onto shelves in the
 * tray to its right, tallest first, at their start rotation and unflipped.
 */
export function layOut(level: Level): Layout {
    const silhouette = level.silhouette.map(verticesOf);
    const box = boundsOf(silhouette.flat());
    const shapes = level.pieces.map((piece) => verticesOf(piece.outline));
    const trayX = MARGIN + (box.maxX - box.minX) + 1.2;
    const boxes = shapes
        .map((shape, i) => {
            const turned = boundsOf(shape.map((v) => toWorld(v, { x: 0, y: 0, r: level.pieces[i].startRotation, f: false })));
            return { i, ...turned, w: turned.maxX - turned.minX, h: turned.maxY - turned.minY };
        })
        .sort((a, b) => b.h - a.h);
    const width = Math.max(TRAY_WIDTH, ...boxes.map((b) => b.w + TRAY_GAP));
    const start: Pose[] = [];
    let cx = 0;
    let cy = 0;
    let rowH = 0;
    boxes.forEach((b) => {
        if (cx > 0 && cx + b.w > width) {
            cx = 0;
            cy += rowH + TRAY_GAP;
            rowH = 0;
        }
        start[b.i] = { x: cx - b.minX, y: cy - b.minY, r: level.pieces[b.i].startRotation, f: false };
        cx += b.w + TRAY_GAP;
        rowH = Math.max(rowH, b.h);
    });
    const trayHeight = cy + rowH;
    const height = Math.max(box.maxY - box.minY, trayHeight) + 2 * MARGIN;
    const offset = { x: MARGIN - box.minX, y: (height - (box.maxY - box.minY)) / 2 - box.minY };
    const trayY = (height - trayHeight) / 2;
    return {
        width: trayX + width + MARGIN - TRAY_GAP,
        height,
        offset,
        silhouette: silhouette.map((polygon) => polygon.map((p): Vec => [p[0] + offset.x, p[1] + offset.y])),
        shapes,
        start: start.map((p) => ({ ...p, x: p.x + trayX, y: p.y + trayY })),
    };
}

/**
 * The shortest pull that lands one of part i's corners on a corner of the
 * silhouette or of another part, if any lies within {@link SNAP}.
 */
export function snap(i: number, pieces: Pose[], layout: Layout): Snap | null {
    const targets: Vec[] = layout.silhouette.flat();
    pieces.forEach((other, j) => {
        if (j !== i) layout.shapes[j].forEach((v) => targets.push(toWorld(v, other)));
    });
    let best: (Snap & { d: number }) | null = null;
    for (const v of layout.shapes[i]) {
        const w = toWorld(v, pieces[i]);
        for (const t of targets) {
            const d = Math.hypot(t[0] - w[0], t[1] - w[1]);
            if (d < SNAP && (best === null || d < best.d)) best = { d, dx: t[0] - w[0], dy: t[1] - w[1], at: t };
        }
    }
    return best === null ? null : { dx: best.dx, dy: best.dy, at: best.at };
}

/** Turns one step: +1 clockwise, -1 counter-clockwise. */
export function turned(p: Pose, direction: 1 | -1): Pose {
    return { ...p, r: (p.r + 360 + direction * STEP) % 360 };
}

/**
 * Mirrors on screen, whatever way the part is turned: mirroring the part
 * itself reverses its turn, and top to bottom is left to right followed by
 * half a turn.
 */
export function flipped(p: Pose, axis: "horizontal" | "vertical"): Pose {
    return { ...p, r: ((axis === "horizontal" ? 360 : 540) - p.r) % 360, f: !p.f };
}

/** A pose on the board as the server wants it: in silhouette coordinates. */
export function toPlacement(p: Pose, layout: Layout): Placement {
    return { x: p.x - layout.offset.x, y: p.y - layout.offset.y, rotation: p.r, flipped: p.f };
}

/** mm:ss */
export function clock(ms: number): string {
    const s = Math.floor(ms / 1000);
    return String(Math.floor(s / 60)).padStart(2, "0") + ":" + String(s % 60).padStart(2, "0");
}

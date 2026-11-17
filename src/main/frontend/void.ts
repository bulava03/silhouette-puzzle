// Draws silhouettes as a starry void with a glowing outer edge. Shared by the
// levels page and the game board.
import type { Vec } from "./geometry";

const SVG = "http://www.w3.org/2000/svg";
const VOID = "#04050C";
let ids = 0;

export function el(name: string, attrs: Record<string, string | number>, parent?: Element): SVGElement {
    const node = document.createElementNS(SVG, name);
    Object.entries(attrs).forEach(([key, value]) => node.setAttribute(key, String(value)));
    if (parent) parent.appendChild(node);
    return node;
}

function pointsOf(polygon: Vec[]): string {
    return polygon.map((p) => p[0] + "," + p[1]).join(" ");
}

function boundsOf(polygons: Vec[][]) {
    const xs = polygons.flat().map((p) => p[0]);
    const ys = polygons.flat().map((p) => p[1]);
    return { minX: Math.min(...xs), minY: Math.min(...ys), maxX: Math.max(...xs), maxY: Math.max(...ys) };
}

function stars(parent: Element, box: ReturnType<typeof boundsOf>, count: number, maxR: number): void {
    for (let i = 0; i < count; i++) {
        el("circle", {
            cx: (box.minX + Math.random() * (box.maxX - box.minX)).toFixed(3),
            cy: (box.minY + Math.random() * (box.maxY - box.minY)).toFixed(3),
            r: (maxR * (0.35 + Math.random() * 0.65)).toFixed(3),
            fill: "#FFFFFF",
            opacity: (0.2 + Math.random() * 0.6).toFixed(2),
        }, parent);
    }
}

export interface Silhouette {
    /** Recolours the glowing edge. */
    setAccent(colour: string): void;
}

/**
 * Draws a silhouette into an SVG element or group.
 *
 * The outer edge comes from a filter: the shape is grown a little and the
 * shape itself is cut out of it, so only the outline glows and the seams
 * between the parts stay invisible.
 */
export function draw(target: Element, polygons: Vec[][], accent: string): Silhouette {
    const id = "void-" + ++ids;
    const defs = el("defs", {}, target);
    const clip = el("clipPath", { id: id + "-clip" }, defs);
    polygons.forEach((polygon) => el("polygon", { points: pointsOf(polygon) }, clip));

    const filter = el("filter", { id: id + "-edge", x: "-10%", y: "-10%", width: "120%", height: "120%" }, defs);
    el("feMorphology", { in: "SourceAlpha", operator: "dilate", radius: "0.05", result: "grown" }, filter);
    el("feComposite", { in: "grown", in2: "SourceAlpha", operator: "out", result: "edge" }, filter);
    const flood = el("feFlood", { "flood-color": accent, result: "paint" }, filter);
    el("feComposite", { in: "paint", in2: "edge", operator: "in", result: "line" }, filter);
    el("feGaussianBlur", { in: "line", stdDeviation: "0.05", result: "glow" }, filter);
    const merge = el("feMerge", {}, filter);
    ["SourceGraphic", "glow", "line"].forEach((input) => el("feMergeNode", { in: input }, merge));

    const shape = el("g", { filter: "url(#" + id + "-edge)" }, target);
    // A thin stroke in the same colour closes the hairline seams between parts.
    polygons.forEach((polygon) => el("polygon", {
        points: pointsOf(polygon), fill: VOID, stroke: VOID, "stroke-width": "0.03", "stroke-linejoin": "round",
    }, shape));

    const box = boundsOf(polygons);
    const sky = el("g", { "clip-path": "url(#" + id + "-clip)" }, target);
    stars(sky, box, Math.round((box.maxX - box.minX) * (box.maxY - box.minY) * 2), 0.06);

    return { setAccent: (colour) => flood.setAttribute("flood-color", colour) };
}

/** Turns every svg[data-void] holding plain polygons into a drawn silhouette. */
export function drawAll(): void {
    document.querySelectorAll<SVGSVGElement>("svg[data-void]").forEach((svg) => {
        const polygons = Array.from(svg.querySelectorAll("polygon")).map((polygon) =>
            (polygon.getAttribute("points") ?? "").trim().split(/\s+/)
                .map((pair) => pair.split(",").map(Number) as Vec));
        svg.replaceChildren();
        draw(svg, polygons, svg.dataset.accent || "#6BD8DE");
    });
}

/** Scatters faint stars over every svg.sky[data-stars]. */
export function drawSky(): void {
    document.querySelectorAll<SVGSVGElement>("svg.sky[data-stars]").forEach((svg) => {
        for (let i = 0; i < Number(svg.dataset.stars); i++) {
            el("circle", {
                cx: (Math.random() * 100).toFixed(2) + "%",
                cy: (Math.random() * 100).toFixed(2) + "%",
                r: (0.5 + Math.random()).toFixed(2),
                fill: "#FFFFFF",
                opacity: (0.1 + Math.random() * 0.35).toFixed(2),
            }, svg);
        }
    });
}

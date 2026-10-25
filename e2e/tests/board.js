import { readFileSync } from "node:fs";

const catalog = JSON.parse(readFileSync(
    new URL("../../src/main/resources/levels/pictures.json", import.meta.url), "utf8"));

/**
 * Where each part of a level belongs, worked out from the catalog the way the
 * server does: the part's centre rounded to 0.01, unturned, and flipped if the
 * part is handed out mirrored. The game page itself never gets the solution.
 */
export function targetsOf(levelId) {
    const level = catalog.levels.find((l) => l.id === levelId);
    const round = (n) => Math.round(n * 100) / 100;
    return level.parts.map((part) => ({
        x: round(part.outline.reduce((s, v) => s + v[0], 0) / part.outline.length),
        y: round(part.outline.reduce((s, v) => s + v[1], 0) / part.outline.length),
        startRotation: part.startRotation,
        mirrored: part.mirrored === true,
    }));
}

/** The offset from silhouette coordinates to board coordinates, read off the page. */
function boardOffset(page) {
    return page.evaluate(() => {
        const shapes = JSON.parse(document.getElementById("game").dataset.shapes);
        const drawn = Array.from(document.querySelectorAll("#board-svg clipPath polygon"))
            .flatMap((polygon) => Array.from(polygon.points));
        const given = shapes.silhouette.flatMap((part) => part.vertices);
        const min = (points, axis) => Math.min(...points.map((p) => p[axis]));
        return { x: min(drawn, "x") - min(given, "x"), y: min(drawn, "y") - min(given, "y") };
    });
}

/**
 * A point to grab the part by (inside its shape, near the middle) on the
 * screen, the part's origin in board units, and the board-to-screen scale.
 */
function handleOf(page, index) {
    return page.evaluate((i) => {
        const svg = document.getElementById("board-svg");
        // data-index names the part: the selected part moves to the end of the SVG.
        const piece = svg.querySelector(`g.piece[data-index="${i}"]`);
        const polygon = piece.querySelector("polygon");
        const box = polygon.getBBox();
        let best = null;
        let bestDistance = Infinity;
        for (let gx = 1; gx < 20; gx++) {
            for (let gy = 1; gy < 20; gy++) {
                const point = new DOMPoint(box.x + (box.width * gx) / 20, box.y + (box.height * gy) / 20);
                const distance = Math.hypot(point.x - box.x - box.width / 2, point.y - box.y - box.height / 2);
                if (polygon.isPointInFill(point) && distance < bestDistance) {
                    best = point;
                    bestDistance = distance;
                }
            }
        }
        const toScreen = piece.getScreenCTM();
        const board = svg.getScreenCTM();
        const grab = best.matrixTransform(toScreen);
        // getCTM() would be in pixels; through the screen and back gives board units.
        const origin = new DOMPoint(0, 0).matrixTransform(toScreen).matrixTransform(board.inverse());
        return { grab: { x: grab.x, y: grab.y }, origin: { x: origin.x, y: origin.y }, scale: board.a };
    }, index);
}

/** Turns and flips a part into its final orientation with the buttons, as a player would. */
async function orient(page, index, target) {
    const { grab } = await handleOf(page, index);
    await page.mouse.click(grab.x, grab.y);
    for (let r = target.startRotation; r % 360 !== 0; r += 45) {
        await page.getByRole("button", { name: "Turn 45° clockwise" }).click();
    }
    if (target.mirrored) {
        await page.getByRole("button", { name: "Flip left to right" }).click();
    }
}

/** Drags a part so its origin lands on a board position. */
async function drag(page, index, to) {
    const { grab, origin, scale } = await handleOf(page, index);
    await page.mouse.move(grab.x, grab.y);
    await page.mouse.down();
    await page.mouse.move(grab.x + scale * (to.x - origin.x), grab.y + scale * (to.y - origin.y), { steps: 8 });
    await page.mouse.up();
}

/** Builds the picture on the open game page: all of it, or only its first parts. */
export async function buildPicture(page, levelId, parts) {
    await page.locator("#board-svg g.piece").first().waitFor();
    const offset = await boardOffset(page);
    const targets = targetsOf(levelId).slice(0, parts);
    for (let i = 0; i < targets.length; i++) {
        await orient(page, i, targets[i]);
        await drag(page, i, { x: targets[i].x + offset.x, y: targets[i].y + offset.y });
    }
}

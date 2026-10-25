import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { expect, Page } from '@playwright/test';

type Vertex = [number, number];
type Part = { name: string; outline: Vertex[]; startRotation: number; mirrored?: boolean };
type Catalog = { levels: { id: string; parts: Part[] }[] };

const catalog: Catalog = JSON.parse(
  readFileSync(join(__dirname, '../../src/main/resources/levels/pictures.json'), 'utf8'));

/**
 * Where each part of a level belongs, worked out from the catalog the way the
 * server does: the part's centre rounded to 0.01, unturned, and flipped if the
 * part is handed out mirrored. The game page itself never gets the solution.
 */
export function targetsOf(levelId: string) {
  const level = catalog.levels.find((l) => l.id === levelId)!;
  const round = (n: number) => Math.round(n * 100) / 100;
  return level.parts.map((part) => ({
    x: round(part.outline.reduce((s, v) => s + v[0], 0) / part.outline.length),
    y: round(part.outline.reduce((s, v) => s + v[1], 0) / part.outline.length),
    startRotation: part.startRotation,
    mirrored: part.mirrored === true,
  }));
}

/** The offset from silhouette coordinates to board coordinates, read off the page. */
async function boardOffset(page: Page) {
  return page.evaluate(() => {
    const shapes = JSON.parse((document.getElementById('game') as HTMLElement).dataset.shapes!);
    const minX = (points: { x: number }[]) => Math.min(...points.map((p) => p.x));
    const minY = (points: { y: number }[]) => Math.min(...points.map((p) => p.y));
    const drawn = Array.from(document.querySelectorAll('#board-svg clipPath polygon'))
      .flatMap((polygon) => Array.from((polygon as SVGPolygonElement).points));
    const given = shapes.silhouette.flatMap((part: { vertices: { x: number; y: number }[] }) => part.vertices);
    return { x: minX(drawn) - minX(given), y: minY(drawn) - minY(given) };
  });
}

/**
 * A point the mouse can grab the part by (inside its shape, near the middle),
 * the part's origin on the board, and the board-to-screen transform.
 */
async function handleOf(page: Page, index: number) {
  return page.evaluate((i) => {
    const svg = document.getElementById('board-svg') as unknown as SVGSVGElement;
    const piece = svg.querySelector(`g.piece[data-index="${i}"]`) as SVGGElement;
    const polygon = piece.querySelector('polygon') as SVGPolygonElement;
    const box = polygon.getBBox();
    let best: DOMPoint | null = null;
    let bestDistance = Infinity;
    for (let gx = 1; gx < 20; gx++) {
      for (let gy = 1; gy < 20; gy++) {
        const point = new DOMPoint(box.x + (box.width * gx) / 20, box.y + (box.height * gy) / 20);
        const distance = Math.hypot(point.x - (box.x + box.width / 2), point.y - (box.y + box.height / 2));
        if (polygon.isPointInFill(point) && distance < bestDistance) {
          best = point;
          bestDistance = distance;
        }
      }
    }
    const toScreen = piece.getScreenCTM()!;
    const grab = best!.matrixTransform(toScreen);
    const board = svg.getScreenCTM()!;
    // The part's origin in board units: its local (0, 0) through the screen and back.
    const origin = new DOMPoint(0, 0).matrixTransform(toScreen).matrixTransform(board.inverse());
    return {
      grab: { x: grab.x, y: grab.y },
      origin: { x: origin.x, y: origin.y },
      board: { a: board.a, b: board.b, c: board.c, d: board.d, e: board.e, f: board.f },
    };
  }, index);
}

/** Turns and flips a part into its final orientation with the buttons, as a player would. */
async function orient(page: Page, index: number, target: ReturnType<typeof targetsOf>[number]) {
  const handle = await handleOf(page, index);
  await page.mouse.click(handle.grab.x, handle.grab.y);
  for (let r = target.startRotation; r % 360 !== 0; r += 45) {
    await page.getByRole('button', { name: 'Turn 45° clockwise' }).click();
  }
  if (target.mirrored) {
    await page.getByRole('button', { name: 'Flip left to right' }).click();
  }
}

/** Drags a part so its origin lands on a board position. */
async function drag(page: Page, index: number, to: { x: number; y: number }) {
  const { grab, origin, board } = await handleOf(page, index);
  const dx = to.x - origin.x;
  const dy = to.y - origin.y;
  const screenX = grab.x + board.a * dx + board.c * dy;
  const screenY = grab.y + board.b * dx + board.d * dy;
  await page.mouse.move(grab.x, grab.y);
  await page.mouse.down();
  await page.mouse.move(screenX, screenY, { steps: 8 });
  await page.mouse.up();
}

/** Builds the whole picture on the open game page. */
export async function buildPicture(page: Page, levelId: string) {
  await expect(page.locator('#board-svg g.piece').first()).toBeVisible();
  const offset = await boardOffset(page);
  const targets = targetsOf(levelId);
  for (let i = 0; i < targets.length; i++) {
    await orient(page, i, targets[i]);
    await drag(page, i, { x: targets[i].x + offset.x, y: targets[i].y + offset.y });
  }
}

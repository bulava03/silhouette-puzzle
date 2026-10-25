// Renders the README screenshots from a running app (docker compose up),
// with players it registers itself: node scripts/screenshots.js
import { fileURLToPath } from "node:url";
import { chromium } from "@playwright/test";
import { buildPicture } from "../tests/board.js";

const baseURL = process.env.BASE_URL ?? "http://localhost:8080";
const shots = fileURLToPath(new URL("../../docs/screenshots/", import.meta.url));
const playerName = () => "player" + (1000 + Math.floor(Math.random() * 9000));

const browser = await chromium.launch();
const page = await browser.newPage({ baseURL, viewport: { width: 1280, height: 900 } });

async function register(name) {
    await page.goto("/login?mode=register");
    await page.getByLabel("Username").fill(name);
    await page.getByLabel("Password").fill("correct-horse");
    await page.getByRole("button", { name: "Register" }).click();
    await page.locator(".account__name").waitFor();
}

async function solve(levelId, thinkMs) {
    await page.goto("/play/" + levelId);
    await page.waitForTimeout(thinkMs);
    await buildPicture(page, levelId);
    await page.getByRole("button", { name: "Check picture" }).click();
    await page.getByText("Picture complete!").waitFor();
}

for (const thinkMs of [9000, 15000, 21000]) {
    await register(playerName());
    await solve("bus", thinkMs);
    await page.getByRole("button", { name: "Log out" }).click();
}
await register(playerName());
await solve("house", 4000);
await solve("rocket", 6000);
await solve("bus", 5000);

await page.goto("/");
await page.screenshot({ path: shots + "levels.png" });

await page.goto("/play/bus");
await buildPicture(page, "bus", 11);
await page.locator('#board-svg g.piece[data-index="11"]').click();
await page.waitForTimeout(500);
await page.screenshot({ path: shots + "game.png" });

await page.goto("/play/bus");
await page.waitForTimeout(3000);
await buildPicture(page, "bus");
await page.getByRole("button", { name: "Check picture" }).click();
await page.getByText("Picture complete!").waitFor();
await page.screenshot({ path: shots + "solved.png" });

await page.goto("/leaderboard?level=bus");
await page.screenshot({ path: shots + "leaderboard.png" });

await browser.close();

import { expect, test } from "@playwright/test";
import { buildPicture } from "./board.js";

test("a guest builds the house and gets a verdict without a place on the board", async ({ page }) => {
    await page.goto("/play/house");

    await buildPicture(page, "house");
    await page.getByRole("button", { name: "Check picture" }).click();

    await expect(page.getByText("Picture complete!")).toBeVisible();
    await expect(page.getByText("Log in to put your times on the leaderboard.")).toBeVisible();
});

test("a guest moves between pictures, back and forth, without the page reloading", async ({ page }) => {
    await page.goto("/play/bus");
    // A reload would wipe this mark.
    await page.evaluate(() => { window.samePage = true; });
    const title = page.locator("#game h1");

    await page.getByRole("link", { name: "02 House" }).click();
    await expect(page).toHaveURL(/\/play\/house$/);
    await expect(title).toHaveText("House");
    await expect(page).toHaveTitle("House · Silhouette");
    await buildPicture(page, "house");
    await page.getByRole("button", { name: "Check picture" }).click();
    await expect(page.getByText("Picture complete!")).toBeVisible();

    await page.getByRole("link", { name: "Next picture" }).click();
    await expect(page).toHaveURL(/\/play\/rocket$/);
    await expect(title).toHaveText("Rocket");
    await expect(page.getByText("Picture complete!")).toBeHidden();
    await expect(page.locator("#moves")).toHaveText("0");

    await page.goBack();
    await expect(page).toHaveURL(/\/play\/house$/);
    await expect(title).toHaveText("House");
    await page.locator('#board-svg g.piece[data-index="0"]').click();
    await page.getByRole("button", { name: "Turn 45° clockwise" }).click();
    await expect(page.locator("#moves")).toHaveText("1");
    await page.getByRole("button", { name: "Restart level" }).click();
    await expect(page.locator("#moves")).toHaveText("0");

    expect(await page.evaluate(() => window.samePage)).toBe(true);
});

test("a picture that is not built yet is not accepted", async ({ page }) => {
    await page.goto("/play/bus");

    await page.getByRole("button", { name: "Check picture" }).click();

    await expect(page.getByText("Not quite")).toBeVisible();
    await expect(page.getByText(/% of the silhouette is off/)).toBeVisible();
});

test("turning and flipping change the selected part and count as moves", async ({ page }) => {
    await page.goto("/play/rocket");
    const turn = page.getByRole("button", { name: "Turn 45° clockwise" });
    await expect(turn).toBeDisabled();

    const body = page.locator('#board-svg g.piece[data-index="5"]');
    await body.click();
    await expect(page.locator("#selection-name")).toHaveText("Body");
    const before = await body.getAttribute("transform");
    await page.getByRole("button", { name: "Flip left to right" }).click();
    await turn.click();

    await expect(body).not.toHaveAttribute("transform", before);
    await expect(page.locator("#moves")).toHaveText("2");
});

test("a registered player builds a picture and sees it on the levels page and the leaderboard", async ({ page }) => {
    const name = "p" + Date.now().toString(36);
    await page.goto("/");
    await page.locator(".site-header").getByRole("link", { name: "Register" }).click();
    await page.getByLabel("Username").fill(name);
    await page.getByLabel("Password").fill("correct-horse");
    await page.getByRole("button", { name: "Register" }).click();
    await expect(page.locator(".account__name")).toContainText(name);

    // Opened from another picture's tabs, the house gets its timed attempt from the API.
    await page.goto("/play/bus");
    await page.getByRole("link", { name: "02 House" }).click();
    await expect(page.locator("#game h1")).toHaveText("House");
    await buildPicture(page, "house");
    await page.getByRole("button", { name: "Check picture" }).click();
    await expect(page.getByText("Picture complete!")).toBeVisible();
    await expect(page.getByText(/Your best puts you #\d+ on the leaderboard\./)).toBeVisible();

    await page.goto("/");
    // By the exact heading: "House" is also part of "Lighthouse".
    const house = page.locator("article", { has: page.getByRole("heading", { name: "House", exact: true }) });
    await expect(house.getByText("BUILT", { exact: true })).toBeVisible();
    await page.goto("/leaderboard?level=house");
    await expect(page.locator("tr.is-me")).toContainText(name);
});

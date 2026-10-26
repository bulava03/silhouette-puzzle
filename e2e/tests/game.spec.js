import { expect, test } from "@playwright/test";
import { buildPicture } from "./board.js";

test("a guest builds the house and gets a verdict without a place on the board", async ({ page }) => {
    await page.goto("/play/house");

    await buildPicture(page, "house");
    await page.getByRole("button", { name: "Check picture" }).click();

    await expect(page.getByText("Picture complete!")).toBeVisible();
    await expect(page.getByText("Log in to put your times on the leaderboard.")).toBeVisible();
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

    await page.goto("/play/house");
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

import { expect, test } from "@playwright/test";

const PASSWORD = "correct-horse";

const uniqueName = () => "u_" + Date.now().toString(36) + Math.floor(Math.random() * 1000);
const header = (page) => page.locator(".site-header");

async function register(page, username, password) {
    await page.goto("/login?mode=register");
    await page.getByLabel("Username").fill(username);
    await page.getByLabel("Password").fill(password);
    await page.getByRole("button", { name: "Register" }).click();
}

async function logIn(page, username, password) {
    await page.goto("/login");
    await page.getByLabel("Username").fill(username);
    await page.getByLabel("Password").fill(password);
    await page.getByRole("button", { name: "Log in" }).click();
}

test("the login page switches between logging in and registering", async ({ page }) => {
    await page.goto("/login");
    await expect(page.getByRole("heading", { name: "Welcome back" })).toBeVisible();

    await page.getByRole("navigation", { name: "Mode" }).getByRole("link", { name: "Register" }).click();
    await expect(page.getByRole("heading", { name: "Create an account" })).toBeVisible();

    await page.getByRole("navigation", { name: "Mode" }).getByRole("link", { name: "Log in" }).click();
    await expect(page.getByRole("heading", { name: "Welcome back" })).toBeVisible();
});

test("registration explains what is wrong and keeps the guest logged out", async ({ page }) => {
    await register(page, "a!", PASSWORD);
    await expect(page.getByText("must be 3 to 20 Latin letters, digits or underscores")).toBeVisible();

    await register(page, uniqueName(), "short");
    await expect(page.getByText("size must be between 8 and 72")).toBeVisible();

    await page.goto("/");
    await expect(header(page).getByRole("link", { name: "Log in" })).toBeVisible();
});

test("a name is taken regardless of case", async ({ page }) => {
    const name = uniqueName();
    await register(page, name, PASSWORD);
    await expect(header(page)).toContainText(name);
    await header(page).getByRole("button", { name: "Log out" }).click();

    await register(page, name.toUpperCase(), PASSWORD);

    await expect(page.getByRole("alert")).toHaveText("Username is taken: " + name.toUpperCase());
});

test("a wrong password fails, the right one works in any case, and logging out ends it", async ({ page }) => {
    const name = uniqueName();
    await register(page, name, PASSWORD);
    await header(page).getByRole("button", { name: "Log out" }).click();
    await expect(header(page).getByRole("link", { name: "Register" })).toBeVisible();

    await logIn(page, name, "wrong-horse");
    await expect(page.getByRole("alert")).toHaveText("Wrong username or password.");

    await logIn(page, name.toUpperCase(), PASSWORD);
    await expect(page).toHaveURL(/\/$/);
    await expect(header(page)).toContainText(name);

    await header(page).getByRole("button", { name: "Log out" }).click();
    await expect(header(page).getByRole("link", { name: "Log in" })).toBeVisible();
    await expect(header(page).getByRole("link", { name: "Register" })).toBeVisible();
});

import { defineConfig, devices } from "@playwright/test";

// The tests drive a running instance of the app: locally the one started
// with `docker compose up`, in CI the same compose stack. Every test creates
// its own players. Results go wherever --output points, so a local run can
// keep them out of synced folders (OneDrive cannot always delete them).
export default defineConfig({
    testDir: "./tests",
    workers: 1,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 1 : 0,
    reporter: process.env.CI ? [["list"], ["html", { open: "never" }]] : "list",
    use: {
        baseURL: process.env.BASE_URL ?? "http://localhost:8080",
        trace: "retain-on-failure",
        screenshot: "only-on-failure",
    },
    projects: [
        { name: "chromium", use: { ...devices["Desktop Chrome"], viewport: { width: 1280, height: 900 } } },
    ],
});

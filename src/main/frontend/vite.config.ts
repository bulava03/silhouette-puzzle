import { fileURLToPath } from "node:url";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

const source = (path: string) => fileURLToPath(new URL(path, import.meta.url));

// Builds the two page scripts straight into the classes Spring serves as
// static resources, next to the stylesheet and fonts Maven copies there.
// The names carry no hash: the templates link /js/game.js and /js/levels.js.
export default defineConfig({
    plugins: [react()],
    build: {
        outDir: source("../../../target/classes/static"),
        emptyOutDir: false,
        modulePreload: false,
        rollupOptions: {
            input: {
                game: source("game/main.tsx"),
                levels: source("levels.ts"),
            },
            output: {
                entryFileNames: "js/[name].js",
                chunkFileNames: "js/[name].js",
            },
        },
    },
    test: {
        include: ["**/*.test.ts"],
        exclude: ["node_modules/**"],
    },
});

// The game page: Thymeleaf renders the page around the game and hands over
// the opening level in data attributes; React renders the game itself.
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import type { Level, LevelTab } from "../types";
import { Game, type GamePage } from "./Game";

const root = document.getElementById("game")!;
const data = root.dataset;
const page: GamePage = {
    level: JSON.parse(data.level!) as Level,
    levels: JSON.parse(data.levels!) as LevelTab[],
    attemptId: data.attempt || null,
    guest: data.guest === "true",
};

createRoot(root).render(
    <StrictMode>
        <Game page={page} />
    </StrictMode>,
);

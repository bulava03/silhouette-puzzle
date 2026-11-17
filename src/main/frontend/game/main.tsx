// The game page: Thymeleaf renders the page around the game and hands over
// the level in data attributes; React takes the side panel and the board.
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import type { Level } from "../types";
import { Game, type GamePage } from "./Game";

const root = document.getElementById("game")!;
const data = root.dataset;
const page: GamePage = {
    levelId: data.level!,
    name: data.name!,
    number: data.number!,
    level: JSON.parse(data.shapes!) as Level,
    attemptId: data.attempt || null,
    guest: data.guest === "true",
    accent: data.accent!,
    nextUrl: data.next!,
    leaderboardUrl: data.leaderboard!,
};

createRoot(document.getElementById("game-side")!).render(
    <StrictMode>
        <Game page={page} boardCard={document.getElementById("board")!} />
    </StrictMode>,
);

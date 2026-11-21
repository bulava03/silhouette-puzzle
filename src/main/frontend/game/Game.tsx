import { useCallback, useEffect, useReducer, useRef, useState } from "react";
import { ApiError, checkPicture, getLevel, startAttempt } from "../api";
import { toPlacement } from "../geometry";
import type { Level, LevelTab } from "../types";
import { Board } from "./Board";
import { ResultBanner, StatusPanel, ToolPanel } from "./Panels";
import { initialState, reduce } from "./state";
import { LevelTabs, playUrl } from "./Tabs";

const TICK_MS = 500;

/** What the server put on the page for the game. */
export interface GamePage {
    /** The level the page opened with. */
    level: Level;
    levels: LevelTab[];
    /** A logged-in player's timed attempt on that level, started as the page was rendered. */
    attemptId: string | null;
    guest: boolean;
}

/** How a level change shows in the address bar. */
type History = "push" | "keep";

/**
 * The game page: the level tabs, the board and the panel beside it. Moving
 * to another level or starting over happens in place: the level comes from
 * the API and, for a logged-in player, so does a fresh timed attempt.
 */
export function Game({ page }: { page: GamePage }) {
    const [state, dispatch] = useReducer(reduce, undefined, () => initialState(page.level, page.attemptId, Date.now()));
    const [now, setNow] = useState(() => Date.now());
    // The latest state, for answers that arrive after a few renders.
    const latest = useRef(state);
    latest.current = state;
    const loads = useRef(0);

    useEffect(() => {
        if (state.stoppedAt !== null) return;
        setNow(Date.now());
        const timer = setInterval(() => setNow(Date.now()), TICK_MS);
        return () => clearInterval(timer);
    }, [state.stoppedAt, state.startedAt]);

    useEffect(() => {
        document.title = state.level.name + " · Silhouette";
    }, [state.level.name]);

    /** Starts a level: a new one from the API, or the current one again. */
    const play = useCallback(async (levelId: string, history: History) => {
        const ticket = ++loads.current;
        const round = latest.current.round;
        try {
            const level = levelId === latest.current.level.id ? latest.current.level : await getLevel(levelId);
            // A session that ended meanwhile makes the player a guest; the game goes on untimed.
            const attemptId = page.guest ? null
                : await startAttempt(levelId).catch((error) => {
                    if (error instanceof ApiError && error.status === 401) return null;
                    throw error;
                });
            if (ticket !== loads.current) return; // the player has already asked for another level
            if (history === "push") window.history.pushState(null, "", playUrl(levelId));
            dispatch({ type: "start", level, attemptId, at: Date.now() });
        } catch (error) {
            if (ticket !== loads.current) return;
            dispatch({ type: "failed", round, message: error instanceof Error ? error.message : String(error) });
        }
    }, [page.guest]);

    // Back and forward move between the levels played on this page.
    useEffect(() => {
        const onPopState = () => {
            const levelId = decodeURIComponent(window.location.pathname.split("/").pop() ?? "");
            if (levelId && levelId !== latest.current.level.id) void play(levelId, "keep");
        };
        window.addEventListener("popstate", onPopState);
        return () => window.removeEventListener("popstate", onPopState);
    }, [play]);

    const open = (levelId: string) => {
        if (levelId !== state.level.id) void play(levelId, "push");
    };
    const restart = () => void play(state.level.id, "keep");

    async function check() {
        if (state.solved || state.busy) return;
        const { round } = state;
        dispatch({ type: "checking" });
        try {
            const result = await checkPicture(state.level.id, {
                attemptId: state.attemptId,
                placements: state.pieces.map((p) => toPlacement(p, state.layout)),
            });
            dispatch({ type: "checked", round, result, at: Date.now() });
        } catch (error) {
            if (error instanceof ApiError && (error.status === 410 || error.status === 404)) {
                // The attempt outlived its day (410) or is already gone (404).
                dispatch({ type: "expired", round, at: Date.now() });
            } else {
                dispatch({ type: "failed", round, message: error instanceof Error ? error.message : String(error) });
            }
        }
    }

    return (
        <>
            <section className="game__board" aria-label="Board">
                <LevelTabs levels={page.levels} current={state.level.id} onOpen={open} />
                <div className={state.solved ? "board card is-solved" : "board card"} id="board">
                    <Board state={state} dispatch={dispatch} />
                </div>
                <p className="hint">Drag the parts onto the silhouette. Some lie turned or mirrored: select one to turn
                    or flip it. Corners snap together when they come close.</p>
            </section>
            <aside className="game__side" aria-label="Status and controls">
                <StatusPanel name={state.level.name} number={state.level.number} parts={state.level.pieces.length}
                             timeMs={state.stoppedAt ?? Math.max(0, now - state.startedAt)} moves={state.moves} />
                <ToolPanel piece={state.selected === null ? null : state.level.pieces[state.selected]}
                           solved={state.solved} busy={state.busy} dispatch={dispatch} onCheck={check}
                           onRestart={restart} />
                <ResultBanner result={state.result} guest={page.guest} level={state.level} onOpen={open}
                              onRestart={restart} />
            </aside>
        </>
    );
}

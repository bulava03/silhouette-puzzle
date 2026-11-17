import { useEffect, useMemo, useReducer, useState } from "react";
import { createPortal } from "react-dom";
import { ApiError, checkPicture } from "../api";
import { layOut, toPlacement } from "../geometry";
import type { Level } from "../types";
import { Board } from "./Board";
import { ResultBanner, StatusPanel, ToolPanel } from "./Panels";
import { initialState, reduce } from "./state";

const TICK_MS = 500;

/** What the server put on the page for the game. */
export interface GamePage {
    levelId: string;
    name: string;
    /** Two digits, as the page shows it. */
    number: string;
    level: Level;
    /** A logged-in player's timed attempt, started as the page was rendered. */
    attemptId: string | null;
    guest: boolean;
    accent: string;
    nextUrl: string;
    leaderboardUrl: string;
}

/**
 * The game: renders the side panel where it is mounted and the board into
 * the page's board card. Starting over means a fresh page and so, for a
 * logged-in player, a fresh timed attempt.
 */
export function Game({ page, boardCard }: { page: GamePage; boardCard: HTMLElement }) {
    const layout = useMemo(() => layOut(page.level), [page.level]);
    const [state, dispatch] = useReducer(reduce, undefined, () => initialState(page.level, layout, page.attemptId));
    const [startedAt] = useState(() => Date.now());
    const [now, setNow] = useState(startedAt);

    useEffect(() => {
        if (state.stoppedAt !== null) return;
        const timer = setInterval(() => setNow(Date.now()), TICK_MS);
        return () => clearInterval(timer);
    }, [state.stoppedAt]);

    useEffect(() => {
        boardCard.classList.toggle("is-solved", state.solved);
    }, [boardCard, state.solved]);

    const restart = () => window.location.reload();

    async function check() {
        if (state.solved || state.busy) return;
        dispatch({ type: "checking" });
        try {
            const result = await checkPicture(page.levelId, {
                attemptId: state.attemptId,
                placements: state.pieces.map((p) => toPlacement(p, layout)),
            });
            dispatch({ type: "checked", result, elapsedMs: Date.now() - startedAt });
        } catch (error) {
            if (error instanceof ApiError && (error.status === 410 || error.status === 404)) {
                // The attempt outlived its day (410) or is already gone (404).
                dispatch({ type: "expired", elapsedMs: Date.now() - startedAt });
            } else {
                dispatch({ type: "failed", message: error instanceof Error ? error.message : String(error) });
            }
        }
    }

    return (
        <>
            {createPortal(<Board state={state} dispatch={dispatch} name={page.name} accent={page.accent} />, boardCard)}
            <StatusPanel name={page.name} number={page.number} parts={page.level.pieces.length}
                         timeMs={state.stoppedAt ?? now - startedAt} moves={state.moves} />
            <ToolPanel piece={state.selected === null ? null : page.level.pieces[state.selected]}
                       solved={state.solved} busy={state.busy} dispatch={dispatch} onCheck={check} onRestart={restart} />
            <ResultBanner result={state.result} guest={page.guest} nextUrl={page.nextUrl}
                          leaderboardUrl={page.leaderboardUrl} onRestart={restart} />
        </>
    );
}

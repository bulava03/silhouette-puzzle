// The side of the game page: the level and its meters, the tools for the
// selected part, and what the last check said.
import type { Dispatch, ReactNode } from "react";
import { clock } from "../geometry";
import type { Piece } from "../types";
import type { Action, Result } from "./state";

export function StatusPanel({ name, number, parts, timeMs, moves }: {
    name: string;
    number: string;
    parts: number;
    timeMs: number;
    moves: number;
}) {
    return (
        <div className="card panel">
            <span className="panel__eyebrow">Level {number} · <span id="part-count">{parts}</span> parts</span>
            <h1 className="title">{name}</h1>
            <div className="meters">
                <div className="meter">
                    <span className="meter__label">Time</span>
                    <span className="meter__value meter__value--time" id="clock" role="timer" aria-live="off">{clock(timeMs)}</span>
                </div>
                <div className="meter">
                    <span className="meter__label">Moves</span>
                    <span className="meter__value meter__value--moves" id="moves">{moves}</span>
                </div>
            </div>
        </div>
    );
}

const ICON = {
    width: 22, height: 22, viewBox: "0 0 24 24", fill: "none", stroke: "currentColor", strokeWidth: 2,
    strokeLinecap: "round", strokeLinejoin: "round", "aria-hidden": true,
} as const;

function Tool({ id, label, disabled, onClick, children }: {
    id: string;
    label: string;
    disabled: boolean;
    onClick: () => void;
    children: ReactNode;
}) {
    return (
        <button className="tool" type="button" id={id} aria-label={label} disabled={disabled} onClick={onClick}>
            <svg {...ICON}>{children}</svg>
        </button>
    );
}

export function ToolPanel({ piece, solved, busy, dispatch, onCheck, onRestart }: {
    piece: Piece | null;
    solved: boolean;
    busy: boolean;
    dispatch: Dispatch<Action>;
    onCheck: () => void;
    onRestart: () => void;
}) {
    const off = piece === null || solved;
    return (
        <div className="card panel">
            <div className="selection" aria-live="polite">
                <span className="selection__swatch" id="selection-swatch" style={{ background: piece?.color ?? "" }}></span>
                <span id="selection-name">{piece ? piece.name : "Select a part"}</span>
            </div>
            <div className="tools">
                <Tool id="rotate-left" label="Turn 45° counter-clockwise" disabled={off}
                      onClick={() => dispatch({ type: "turn", direction: -1 })}>
                    <path d="M3 12a9 9 0 1 0 3-6.7"></path><polyline points="3 3 3 9 9 9"></polyline>
                </Tool>
                <Tool id="rotate-right" label="Turn 45° clockwise" disabled={off}
                      onClick={() => dispatch({ type: "turn", direction: 1 })}>
                    <path d="M21 12a9 9 0 1 1-3-6.7"></path><polyline points="21 3 21 9 15 9"></polyline>
                </Tool>
                <Tool id="flip-horizontal" label="Flip left to right" disabled={off}
                      onClick={() => dispatch({ type: "flip", axis: "horizontal" })}>
                    <line x1="12" y1="3" x2="12" y2="21" strokeDasharray="2 2"></line>
                    <polygon points="9 7 3 17 9 17"></polygon><polygon points="15 7 21 17 15 17"></polygon>
                </Tool>
                <Tool id="flip-vertical" label="Flip top to bottom" disabled={off}
                      onClick={() => dispatch({ type: "flip", axis: "vertical" })}>
                    <line x1="3" y1="12" x2="21" y2="12" strokeDasharray="2 2"></line>
                    <polygon points="7 9 17 3 17 9"></polygon><polygon points="7 15 17 21 17 15"></polygon>
                </Tool>
            </div>
            <button className="button button--pink check" type="button" id="check" disabled={solved || busy}
                    onClick={onCheck}>Check picture</button>
            <button className="button button--ghost" type="button" id="restart" onClick={onRestart}>Restart level</button>
        </div>
    );
}

export function ResultBanner({ result, guest, nextUrl, leaderboardUrl, onRestart }: {
    result: Result | null;
    guest: boolean;
    nextUrl: string;
    leaderboardUrl: string;
    onRestart: () => void;
}) {
    switch (result?.kind) {
        case "solved": {
            const time = clock(result.timeMs);
            const note = result.rank ? "Your best puts you #" + result.rank + " on the leaderboard."
                : guest ? "Log in to put your times on the leaderboard." : "";
            return (
                <div className="result result--solved" id="result-solved" role="status">
                    <span className="result__title">Picture complete!</span>
                    <span>Built in <strong className="mono" id="solved-time">{time}</strong> with <strong
                        className="mono" id="solved-moves">{result.moves}</strong> moves.</span>
                    <span className="result__note" id="solved-note">{note}</span>
                    <div className="result__actions">
                        <a className="button button--green" href={nextUrl}>Next picture</a>
                        <a href={leaderboardUrl}>Leaderboard →</a>
                    </div>
                </div>
            );
        }
        case "missed":
            return (
                <div className="result result--missed" id="result-missed" role="status">
                    <span className="result__title">Not quite</span>
                    <span id="missed-text">{result.percentOff}% of the silhouette is off. Keep going.</span>
                </div>
            );
        case "expired":
            return (
                <div className="result result--missed" id="result-expired" role="alert">
                    <span className="result__title">Time ran out</span>
                    <span>This attempt started more than a day ago, so its time no longer counts. Start the picture again.</span>
                    <div className="result__actions">
                        <button className="button button--pink" type="button" id="start-again" onClick={onRestart}>Start again</button>
                    </div>
                </div>
            );
        case "error":
            return (
                <div className="result result--error" id="result-error" role="alert">
                    <span className="result__title">Something went wrong</span>
                    <span id="error-text">{result.message}</span>
                </div>
            );
        default:
            return null;
    }
}

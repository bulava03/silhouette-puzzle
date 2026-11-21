// The game's calls to the server, with the CSRF token Spring Security keeps in a cookie.
import type { AttemptStarted, Level, SubmitRequest, SubmitResult } from "./types";

/** A failed call, with the HTTP status and the problem detail the server gave. */
export class ApiError extends Error {
    constructor(message: string, readonly status: number) {
        super(message);
    }
}

function csrfToken(): string {
    const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : "";
}

async function call<T>(method: "GET" | "POST", url: string, body?: unknown): Promise<T> {
    const response = await fetch(url, {
        method,
        credentials: "same-origin",
        headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": csrfToken() },
        body: body === undefined ? undefined : JSON.stringify(body),
    });
    if (!response.ok) {
        const problem: { detail?: string } = await response.json().catch(() => ({}));
        throw new ApiError(problem.detail || "The server answered " + response.status, response.status);
    }
    return response.json() as Promise<T>;
}

const levelUrl = (levelId: string) => "/api/levels/" + encodeURIComponent(levelId);

export function getLevel(levelId: string): Promise<Level> {
    return call("GET", levelUrl(levelId));
}

/** Starts the clock on a level; only for a logged-in player. */
export async function startAttempt(levelId: string): Promise<string> {
    return (await call<AttemptStarted>("POST", levelUrl(levelId) + "/attempts")).attemptId;
}

/** Sends every part's placement; the server decides whether the picture is complete. */
export function checkPicture(levelId: string, request: SubmitRequest): Promise<SubmitResult> {
    return call("POST", levelUrl(levelId) + "/check", request);
}

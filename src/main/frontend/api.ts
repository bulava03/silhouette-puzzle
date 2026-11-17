// The one call the game makes, with the CSRF token Spring Security keeps in a cookie.
import type { SubmitRequest, SubmitResult } from "./types";

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

async function post<T>(url: string, body: unknown): Promise<T> {
    const response = await fetch(url, {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": csrfToken() },
        body: JSON.stringify(body),
    });
    if (!response.ok) {
        const problem: { detail?: string } = await response.json().catch(() => ({}));
        throw new ApiError(problem.detail || "The server answered " + response.status, response.status);
    }
    return response.json() as Promise<T>;
}

/** Sends every part's placement; the server decides whether the picture is complete. */
export function checkPicture(levelId: string, request: SubmitRequest): Promise<SubmitResult> {
    return post("/api/levels/" + encodeURIComponent(levelId) + "/check", request);
}

import type { CSSProperties, MouseEvent } from "react";
import type { LevelTab } from "../types";

export const playUrl = (levelId: string) => "/play/" + encodeURIComponent(levelId);

/** Two digits, as the pages number the levels. */
export const twoDigits = (n: number) => String(n).padStart(2, "0");

/**
 * Opens a level in place on a plain click; a click meant for a new tab or
 * window is left to the browser, so the link still works as a link.
 */
export function inPlace(event: MouseEvent, open: () => void): void {
    if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    event.preventDefault();
    open();
}

export function LevelTabs({ levels, current, onOpen }: {
    levels: LevelTab[];
    current: string;
    onOpen: (levelId: string) => void;
}) {
    return (
        <nav className="tabs" aria-label="Levels">
            {levels.map((tab) => (
                <a key={tab.id} className="tab" href={playUrl(tab.id)}
                   style={{ "--accent": tab.accent } as CSSProperties}
                   aria-current={tab.id === current ? "page" : undefined}
                   onClick={(event) => inPlace(event, () => onOpen(tab.id))}>
                    {twoDigits(tab.number) + " " + tab.name}
                </a>
            ))}
        </nav>
    );
}

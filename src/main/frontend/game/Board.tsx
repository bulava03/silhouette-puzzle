import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, type Dispatch, type PointerEvent } from "react";
import type { Vec } from "../geometry";
import { draw, type Silhouette } from "../void";
import { Piece } from "./Piece";
import type { Action, Flash, GameState } from "./state";

const SOLVED_ACCENT = "#80DBA2";
const FLASH_MS = 360;

interface Props {
    state: GameState;
    dispatch: Dispatch<Action>;
    name: string;
    accent: string;
}

/** The board: the silhouette on the left, the parts in the tray to its right. */
export function Board({ state, dispatch, name, accent }: Props) {
    const { layout, level } = state;
    const svg = useRef<SVGSVGElement>(null);
    const voidLayer = useRef<SVGGElement>(null);
    const silhouette = useRef<Silhouette | null>(null);
    const points = useMemo(() => layout.shapes.map((shape) => shape.map((v) => v.join(",")).join(" ")), [layout]);

    // The starry void is drawn by the same code as on the levels page, into its own layer.
    useLayoutEffect(() => {
        const layer = voidLayer.current!;
        silhouette.current = draw(layer, layout.silhouette, accent);
        return () => layer.replaceChildren();
    }, [layout, accent]);

    useEffect(() => {
        silhouette.current?.setAccent(state.solved ? SOLVED_ACCENT : accent);
    }, [state.solved, accent]);

    const boardPoint = useCallback((event: PointerEvent): Vec => {
        const matrix = svg.current?.getScreenCTM();
        if (!matrix) return [0, 0];
        const point = new DOMPoint(event.clientX, event.clientY).matrixTransform(matrix.inverse());
        return [point.x, point.y];
    }, []);

    const grab = useCallback((i: number, event: PointerEvent<SVGGElement>) => {
        event.stopPropagation();
        svg.current?.setPointerCapture(event.pointerId);
        dispatch({ type: "grab", i, at: boardPoint(event) });
    }, [dispatch, boardPoint]);

    const drop = () => dispatch({ type: "drop" });

    return (
        <svg id="board-svg" ref={svg} role="img" aria-label={name + " silhouette and its parts"}
             viewBox={"0 0 " + layout.width.toFixed(3) + " " + layout.height.toFixed(3)}
             onPointerDown={() => dispatch({ type: "select", i: null })}
             onPointerMove={(event) => state.drag && dispatch({ type: "drag", at: boardPoint(event) })}
             onPointerUp={drop} onPointerCancel={drop}>
            <defs>
                <pattern id="dots" width={1} height={1} patternUnits="userSpaceOnUse">
                    <circle cx={0.5} cy={0.5} r={0.025} fill="#252C4D" />
                </pattern>
                <filter id="part-glow" x="-50%" y="-50%" width="200%" height="200%">
                    <feGaussianBlur stdDeviation={0.04} result="blur" />
                    <feMerge>
                        <feMergeNode in="blur" />
                        <feMergeNode in="SourceGraphic" />
                    </feMerge>
                </filter>
                <filter id="flash" x="-80%" y="-80%" width="260%" height="260%">
                    <feGaussianBlur stdDeviation={0.16} />
                </filter>
            </defs>
            <rect x={0} y={0} width={layout.width} height={layout.height} fill="url(#dots)" />
            <g ref={voidLayer} />
            {state.order.map((i) => (
                <Piece key={i} index={i} pose={state.pieces[i]} points={points[i]} colour={level.pieces[i].color}
                       active={i === state.selected} solved={state.solved} dragging={state.drag?.i === i}
                       onGrab={grab} />
            ))}
            {state.flashes.map((flash) => (
                <SnapFlash key={flash.id} flash={flash} onDone={() => dispatch({ type: "fade", id: flash.id })} />
            ))}
        </svg>
    );
}

function SnapFlash({ flash, onDone }: { flash: Flash; onDone: () => void }) {
    useEffect(() => {
        const timer = setTimeout(onDone, FLASH_MS);
        return () => clearTimeout(timer);
    }, []);
    const [cx, cy] = flash.at;
    return (
        <>
            <circle cx={cx} cy={cy} r={0.28} fill={flash.colour} opacity={0.7} filter="url(#flash)" />
            <circle cx={cx} cy={cy} r={0.06} fill="#FFFFFF" />
        </>
    );
}

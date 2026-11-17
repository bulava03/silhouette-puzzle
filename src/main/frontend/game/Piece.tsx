import { memo, type PointerEvent } from "react";
import { transformOf, type Pose } from "../geometry";

interface Props {
    index: number;
    pose: Pose;
    /** The polygon's points attribute, around the part's own origin. */
    points: string;
    colour: string;
    active: boolean;
    solved: boolean;
    dragging: boolean;
    onGrab: (i: number, event: PointerEvent<SVGGElement>) => void;
}

/** One part on the board. Only the part being dragged redraws on every move. */
export const Piece = memo(function Piece({ index, pose, points, colour, active, solved, dragging, onGrab }: Props) {
    return (
        // data-index names the part, since the selected part moves to the end to draw on top.
        <g className={dragging ? "piece is-dragging" : "piece"} data-index={index} transform={transformOf(pose)}
           onPointerDown={(event) => onGrab(index, event)}>
            <polygon points={points} fill={colour} stroke={colour} strokeLinejoin="round" filter="url(#part-glow)"
                     fillOpacity={active ? 0.55 : solved ? 0.65 : 0.38} strokeWidth={active ? 0.07 : 0.04} />
        </g>
    );
});

// The game: lays out the level the page carries, lets the player drag, turn
// and flip its parts, and sends the placements to the server, which decides
// whether the picture is complete and, for a logged-in player, keeps the time.
(function () {
    'use strict';

    const SNAP = 0.25;
    const STEP = 45;
    const MARGIN = 1;
    const TRAY_WIDTH = 11.2;
    const TRAY_GAP = 0.4;

    const root = document.getElementById('game');
    const svg = document.getElementById('board-svg');
    const board = document.getElementById('board');
    const { el, draw } = window.SilhouetteVoid;
    const ui = {
        clock: document.getElementById('clock'),
        moves: document.getElementById('moves'),
        parts: document.getElementById('part-count'),
        swatch: document.getElementById('selection-swatch'),
        name: document.getElementById('selection-name'),
        rotateLeft: document.getElementById('rotate-left'),
        rotateRight: document.getElementById('rotate-right'),
        flipHorizontal: document.getElementById('flip-horizontal'),
        flipVertical: document.getElementById('flip-vertical'),
        check: document.getElementById('check'),
        restart: document.getElementById('restart'),
        solved: document.getElementById('result-solved'),
        solvedTime: document.getElementById('solved-time'),
        solvedMoves: document.getElementById('solved-moves'),
        solvedNote: document.getElementById('solved-note'),
        missed: document.getElementById('result-missed'),
        missedText: document.getElementById('missed-text'),
        expired: document.getElementById('result-expired'),
        startAgain: document.getElementById('start-again'),
        error: document.getElementById('result-error'),
        errorText: document.getElementById('error-text')
    };

    const game = {
        level: null,     // silhouette and pieces, as the page carries them
        layout: null,    // board size, silhouette offset, tray
        pieces: [],      // per part: { x, y, r, f } in board coordinates
        nodes: [],       // per part: its <g> element
        selected: null,
        drag: null,
        moves: 0,
        attemptId: null,
        startedAt: 0,
        solved: false,
        busy: false,
        timer: null
    };

    // Same order as the server and the SVG transform: mirror, rotate
    // (clockwise on screen), move.
    function toWorld(point, p) {
        const x = p.f ? -point[0] : point[0];
        const a = p.r * Math.PI / 180;
        return [x * Math.cos(a) - point[1] * Math.sin(a) + p.x, x * Math.sin(a) + point[1] * Math.cos(a) + p.y];
    }

    function verticesOf(outline) {
        return outline.vertices.map((v) => [v.x, v.y]);
    }

    function boundsOf(points) {
        const xs = points.map((p) => p[0]);
        const ys = points.map((p) => p[1]);
        return { minX: Math.min(...xs), minY: Math.min(...ys), maxX: Math.max(...xs), maxY: Math.max(...ys) };
    }

    function clock(ms) {
        const s = Math.floor(ms / 1000);
        return String(Math.floor(s / 60)).padStart(2, '0') + ':' + String(s % 60).padStart(2, '0');
    }

    function csrfToken() {
        const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
        return match ? decodeURIComponent(match[1]) : '';
    }

    async function api(method, url, body) {
        const response = await fetch(url, {
            method,
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrfToken() },
            body: body === undefined ? undefined : JSON.stringify(body)
        });
        if (!response.ok) {
            const problem = await response.json().catch(() => ({}));
            const error = new Error(problem.detail || 'The server answered ' + response.status);
            error.status = response.status;
            throw error;
        }
        return response.json();
    }

    // Parts start at their start rotation, unflipped, packed onto shelves in
    // the tray to the right of the silhouette, tallest first.
    function layOut(level) {
        const silhouette = level.silhouette.map(verticesOf);
        const box = boundsOf(silhouette.flat());
        const shapes = level.pieces.map((piece) => verticesOf(piece.outline));
        const trayX = MARGIN + (box.maxX - box.minX) + 1.2;
        const boxes = shapes.map((shape, i) => {
            const turned = boundsOf(shape.map((v) => toWorld(v, { x: 0, y: 0, r: level.pieces[i].startRotation, f: false })));
            return { i, ...turned, w: turned.maxX - turned.minX, h: turned.maxY - turned.minY };
        }).sort((a, b) => b.h - a.h);
        const width = Math.max(TRAY_WIDTH, ...boxes.map((b) => b.w + TRAY_GAP));
        const start = [];
        let cx = 0;
        let cy = 0;
        let rowH = 0;
        boxes.forEach((b) => {
            if (cx > 0 && cx + b.w > width) {
                cx = 0;
                cy += rowH + TRAY_GAP;
                rowH = 0;
            }
            start[b.i] = { x: cx - b.minX, y: cy - b.minY, r: level.pieces[b.i].startRotation, f: false };
            cx += b.w + TRAY_GAP;
            rowH = Math.max(rowH, b.h);
        });
        const trayHeight = cy + rowH;
        const height = Math.max(box.maxY - box.minY, trayHeight) + 2 * MARGIN;
        const offset = { x: MARGIN - box.minX, y: (height - (box.maxY - box.minY)) / 2 - box.minY };
        const trayY = (height - trayHeight) / 2;
        return {
            width: trayX + width + MARGIN - TRAY_GAP,
            height,
            offset,
            silhouette: silhouette.map((polygon) => polygon.map((p) => [p[0] + offset.x, p[1] + offset.y])),
            shapes,
            start: start.map((p) => ({ ...p, x: p.x + trayX, y: p.y + trayY }))
        };
    }

    function render() {
        game.pieces.forEach((p, i) => {
            game.nodes[i].setAttribute('transform',
                'translate(' + p.x + ' ' + p.y + ') rotate(' + p.r + ')' + (p.f ? ' scale(-1 1)' : ''));
            const active = i === game.selected;
            const polygon = game.nodes[i].firstChild;
            polygon.setAttribute('fill-opacity', active ? 0.55 : (game.solved ? 0.65 : 0.38));
            polygon.setAttribute('stroke-width', active ? 0.07 : 0.04);
        });
        if (game.selected !== null) svg.appendChild(game.nodes[game.selected]);
        const piece = game.selected === null ? null : game.level.pieces[game.selected];
        ui.name.textContent = piece ? piece.name : 'Select a part';
        ui.swatch.style.background = piece ? piece.color : '';
        [ui.rotateLeft, ui.rotateRight, ui.flipHorizontal, ui.flipVertical].forEach((button) => {
            button.disabled = piece === null || game.solved;
        });
        ui.check.disabled = game.solved || game.busy;
        ui.moves.textContent = game.moves;
        board.classList.toggle('is-solved', game.solved);
    }

    function hideResults() {
        ui.solved.hidden = true;
        ui.missed.hidden = true;
        ui.expired.hidden = true;
        ui.error.hidden = true;
    }

    function showError(message) {
        hideResults();
        ui.errorText.textContent = message;
        ui.error.hidden = false;
    }

    function boardPoint(event) {
        const matrix = svg.getScreenCTM();
        if (!matrix) return { x: 0, y: 0 };
        const point = svg.createSVGPoint();
        point.x = event.clientX;
        point.y = event.clientY;
        return point.matrixTransform(matrix.inverse());
    }

    function flash(at, colour) {
        const glow = el('circle', { cx: at[0], cy: at[1], r: 0.28, fill: colour, opacity: 0.7, filter: 'url(#flash)' }, svg);
        const dot = el('circle', { cx: at[0], cy: at[1], r: 0.06, fill: '#FFFFFF' }, svg);
        setTimeout(() => {
            glow.remove();
            dot.remove();
        }, 360);
    }

    // Pulls the part so its nearest corner lands on a nearby corner of the
    // silhouette or another part, then counts the move.
    function commit(i) {
        const p = game.pieces[i];
        const targets = game.layout.silhouette.flat();
        game.pieces.forEach((other, j) => {
            if (j !== i) game.layout.shapes[j].forEach((v) => targets.push(toWorld(v, other)));
        });
        let best = null;
        game.layout.shapes[i].forEach((v) => {
            const w = toWorld(v, p);
            targets.forEach((t) => {
                const d = Math.hypot(t[0] - w[0], t[1] - w[1]);
                if (d < SNAP && (best === null || d < best.d)) best = { d, dx: t[0] - w[0], dy: t[1] - w[1], at: t };
            });
        });
        if (best !== null) {
            p.x += best.dx;
            p.y += best.dy;
            flash(best.at, game.level.pieces[i].color);
        }
        game.moves++;
        hideResults();
        render();
    }

    function turn(change) {
        if (game.selected === null || game.solved) return;
        Object.assign(game.pieces[game.selected], change(game.pieces[game.selected]));
        commit(game.selected);
    }

    function onPieceDown(i, event) {
        event.stopPropagation();
        if (game.solved) return;
        svg.setPointerCapture(event.pointerId);
        const at = boardPoint(event);
        game.selected = i;
        game.drag = { i, dx: at.x - game.pieces[i].x, dy: at.y - game.pieces[i].y, moved: false };
        game.nodes[i].classList.add('is-dragging');
        render();
    }

    function onMove(event) {
        if (game.drag === null) return;
        const at = boardPoint(event);
        const p = game.pieces[game.drag.i];
        p.x = Math.min(game.layout.width - 0.2, Math.max(0.2, at.x - game.drag.dx));
        p.y = Math.min(game.layout.height - 0.2, Math.max(0.2, at.y - game.drag.dy));
        game.drag.moved = true;
        render();
    }

    function onUp() {
        if (game.drag === null) return;
        const { i, moved } = game.drag;
        game.nodes[i].classList.remove('is-dragging');
        game.drag = null;
        if (moved) commit(i);
    }

    function build() {
        const layout = game.layout;
        svg.replaceChildren();
        svg.setAttribute('viewBox', '0 0 ' + layout.width.toFixed(3) + ' ' + layout.height.toFixed(3));
        const defs = el('defs', {}, svg);
        const dots = el('pattern', { id: 'dots', width: 1, height: 1, patternUnits: 'userSpaceOnUse' }, defs);
        el('circle', { cx: 0.5, cy: 0.5, r: 0.025, fill: '#252C4D' }, dots);
        const glow = el('filter', { id: 'part-glow', x: '-50%', y: '-50%', width: '200%', height: '200%' }, defs);
        el('feGaussianBlur', { stdDeviation: 0.04, result: 'blur' }, glow);
        const merge = el('feMerge', {}, glow);
        el('feMergeNode', { in: 'blur' }, merge);
        el('feMergeNode', { in: 'SourceGraphic' }, merge);
        const flashBlur = el('filter', { id: 'flash', x: '-80%', y: '-80%', width: '260%', height: '260%' }, defs);
        el('feGaussianBlur', { stdDeviation: 0.16 }, flashBlur);
        el('rect', { x: 0, y: 0, width: layout.width, height: layout.height, fill: 'url(#dots)' }, svg);

        game.edge = draw(svg, layout.silhouette, root.dataset.accent);

        game.nodes = game.level.pieces.map((piece, i) => {
            const g = el('g', { class: 'piece' }, svg);
            el('polygon', {
                points: layout.shapes[i].map((v) => v.join(',')).join(' '),
                fill: piece.color, stroke: piece.color, 'stroke-linejoin': 'round', filter: 'url(#part-glow)'
            }, g);
            g.addEventListener('pointerdown', (event) => onPieceDown(i, event));
            return g;
        });
    }

    // The page started a timed attempt for a logged-in player; starting over
    // means a fresh page and so a fresh attempt.
    function start() {
        game.pieces = game.layout.start.map((p) => ({ ...p }));
        game.startedAt = Date.now();
        game.timer = setInterval(() => {
            ui.clock.textContent = clock(Date.now() - game.startedAt);
        }, 500);
        render();
    }

    async function check() {
        if (game.solved || game.busy) return;
        game.busy = true;
        game.selected = null;
        render();
        const placements = game.pieces.map((p) => ({
            x: p.x - game.layout.offset.x, y: p.y - game.layout.offset.y, rotation: p.r, flipped: p.f
        }));
        try {
            const result = await api('POST', '/api/levels/' + encodeURIComponent(root.dataset.level) + '/check',
                { attemptId: game.attemptId, placements });
            hideResults();
            if (result.solved) {
                game.solved = true;
                clearInterval(game.timer);
                // A player's time comes from the server; a guest only has the page's clock.
                const time = clock(result.timeMs ?? Date.now() - game.startedAt);
                ui.clock.textContent = time;
                ui.solvedTime.textContent = time;
                ui.solvedMoves.textContent = game.moves;
                if (result.rank) {
                    ui.solvedNote.textContent = 'Your best puts you #' + result.rank + ' on the leaderboard.';
                }
                game.edge.setAccent('#80DBA2');
                ui.solved.hidden = false;
            } else {
                const off = Math.max(1, Math.round(result.off * 100));
                ui.missedText.textContent = off + '% of the silhouette is off. Keep going.';
                ui.missed.hidden = false;
            }
        } catch (error) {
            if (error.status === 410 || error.status === 404) {
                // The attempt outlived its day (410) or is already gone (404).
                hideResults();
                clearInterval(game.timer);
                game.attemptId = null;
                ui.expired.hidden = false;
            } else {
                showError(error.message);
            }
        } finally {
            game.busy = false;
            render();
        }
    }

    function init() {
        game.level = JSON.parse(root.dataset.shapes);
        game.attemptId = root.dataset.attempt || null;
        game.layout = layOut(game.level);
        ui.parts.textContent = game.level.pieces.length;
        build();

        svg.addEventListener('pointerdown', () => {
            game.selected = null;
            render();
        });
        svg.addEventListener('pointermove', onMove);
        svg.addEventListener('pointerup', onUp);
        svg.addEventListener('pointercancel', onUp);
        ui.rotateLeft.addEventListener('click', () => turn((p) => ({ r: (p.r + 360 - STEP) % 360 })));
        ui.rotateRight.addEventListener('click', () => turn((p) => ({ r: (p.r + STEP) % 360 })));
        // Mirrors on screen, whatever way the part is turned: mirroring the
        // part itself reverses its turn, and top to bottom is left to right
        // followed by half a turn.
        ui.flipHorizontal.addEventListener('click', () => turn((p) => ({ r: (360 - p.r) % 360, f: !p.f })));
        ui.flipVertical.addEventListener('click', () => turn((p) => ({ r: (540 - p.r) % 360, f: !p.f })));
        ui.check.addEventListener('click', check);
        ui.restart.addEventListener('click', () => window.location.reload());
        ui.startAgain.addEventListener('click', () => window.location.reload());
        start();
    }

    init();
})();

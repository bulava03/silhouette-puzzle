# Silhouette Puzzle

A picture-building puzzle game in the browser: every level is a picture (a
bus, a house, a rocket) cut into its own parts, and the player drags, turns
and flips the parts until they fill the picture's silhouette. All geometry
lives on the server: a solution
is accepted only when the server has checked that the placed pieces cover the
silhouette without overlapping, so records cannot be faked from the client.

> Work in progress. The sections below grow together with the project.

## Background

The project started as a school assignment in 8th grade: a "constructor" in
Pascal where coloured shapes were dragged across the screen to build a
picture. Later it was rewritten in Java Swing; that version is kept as is in
[`legacy/`](legacy/). This repository turns the idea into a web application
with a proper backend.

## Tech stack

Java 21 · Spring Boot 4 (Web MVC, Security, Validation) · MongoDB 7 ·
Spring Data MongoDB · Liquibase · JTS (Java Topology Suite) · Thymeleaf · SVG and
vanilla JavaScript · JUnit 5 · Testcontainers · springdoc-openapi ·
Checkstyle · GitHub Actions · Docker

## Pages

| Path                  | Page                                                         |
|-----------------------|--------------------------------------------------------------|
| `/`                   | Every picture, with the player's best time and moves         |
| `/play/{id}`          | The game                                                     |
| `/leaderboard`        | Podium and table of best runs per picture (`?level=`)        |
| `/login`              | Log in or register (`?mode=register`)                        |

Levels, leaderboards and the login page are rendered on the server with
Thymeleaf. The game page is an SVG board driven by a small script with no
build step: it loads the level from the API, lets the player drag, turn and
flip the parts with corners snapping together, and sends the placements to
the server, which decides whether the picture is complete and keeps the time.
Fonts are served by the application itself, so pages load nothing from
other sites.

## Levels

Built-in levels live in
[`src/main/resources/levels/pictures.json`](src/main/resources/levels/pictures.json)
and are written to MongoDB on every start. A level is the finished picture:
each part is drawn where it belongs, with a name and the natural colour of the
real thing (glass windows, chrome bumper, terracotta roof). From that the
server derives what the game needs:

- each part's own shape, taken around its centre; parts marked `mirrored` are
  handed out as their mirror image, so the player has to flip them;
- how each part lies in the tray at the start (`startRotation`, in 45° steps);
- the reference solution, which puts every part back in place;
- the silhouette, the union of the placed parts built with JTS, which rejects
  overlapping parts and holes.

Every level is therefore solvable by construction. The reference solution
stays on the server and is never sent to clients. The first picture, the bus,
is built from the parts of the original Java version.

## API

Interactive documentation is served by the running application at
http://localhost:8080/swagger-ui.html, and the OpenAPI document at
http://localhost:8080/v3/api-docs.

| Method | Path                           | Description                            |
|--------|--------------------------------|----------------------------------------|
| GET    | `/api/levels`                  | Levels in play order                   |
| GET    | `/api/levels/{id}`             | Silhouette and pieces of one level     |
| POST   | `/api/levels/{id}/attempts`    | Start a timed attempt                  |
| POST   | `/api/attempts/{id}/submit`    | Submit piece placements for checking   |
| GET    | `/api/levels/{id}/leaderboard` | Best run of each player (`?limit=`)    |
| POST   | `/api/players`                 | Register                               |
| POST   | `/login`, `/logout`            | Log in (form) and log out              |
| GET    | `/api/me`                      | Current player (login required)        |
| GET    | `/api/me/progress`             | Personal bests per level (login required) |

Errors are returned as RFC 9457 problem details.

### Accounts

Authentication is session-based with Spring Security: players log in with a
form and stay logged in through the session cookie; passwords are hashed with
BCrypt. State-changing requests need a CSRF token, which the server puts in
the `XSRF-TOKEN` cookie and scripts send back in the `X-XSRF-TOKEN` header.
Usernames are unique regardless of case, enforced by a unique index.

Anyone can play without an account. Only attempts of logged-in players count
for leaderboards and progress, and a player's attempt can be submitted only
by that player.

### Solving a level

The server starts the clock when an attempt begins and stops it when it
accepts a solution; the client never reports a time. A submission carries a
placement per piece (position, rotation in 45° steps, mirrored or not) and a
move count:

```json
{"placements": [{"x": 2, "y": 2, "rotation": 90, "flipped": false}, ...], "moves": 12}
```

The server places the pieces, unites them with JTS and accepts the solution
when the overlap between pieces and the difference from the silhouette both
stay within 1% of the silhouette area. A rejected submission keeps the
attempt open. An accepted one is recorded by a conditional update, so of two
concurrent correct submissions exactly one counts. An attempt can be
submitted for a day; after that the server answers 410 and the game starts
the picture again.

## Data model

Three MongoDB collections:

```
levels    { _id, name, number, silhouette, pieces: [{ name, color, outline, startRotation }], solution }
players   { _id, username, usernameKey, passwordHash, createdAt,
            bests: [{ levelId, timeMs, moves, solvedAt, solves }] }
attempts  { _id, levelId, playerId, startedAt, solvedAt, timeMs, moves }
```

Players and levels are many-to-many, and the link carries data: a player's
best time and moves on a level. It is embedded in the player as `bests`, one
entry per solved level, rather than kept as a log of every run:

- a player's progress is read together with the player;
- the array is bounded by the number of levels, so documents stay small;
- the leaderboard of a level is an aggregation over the players that have
  it (`$match` on an index of `bests.levelId`, `$unwind`, `$sort`), ranked
  by time, then moves, then who was first.

A separate results collection would pay off with millions of players per
level; at this scale embedding keeps the model and the queries simpler.
Bests are updated with conditional updates only: a solve is counted on the
level's entry, a missing entry is pushed only if it is still missing, and a
run replaces the best only if it is faster, or as fast with fewer moves. A
test fires concurrent solves at one player to check that.

Attempts exist because the server, not the client, keeps the time: an
attempt holds its start until it is solved or abandoned. They are temporary,
and a TTL index removes them two days after they start. If a level were ever
retired, one `$pull` would remove it from every player's bests.

### Migrations

MongoDB has no schema to create, but indexes and changes to the shape of
stored data still need to be versioned. They are a Liquibase changelog in
[`src/main/resources/db/changelog`](src/main/resources/db/changelog), using
the MongoDB extension: `createIndex` for indexes and `runCommand` for any
MongoDB command. On startup, before the application serves anything, the
change sets not yet applied run in order. Liquibase records them with
checksums in `DATABASECHANGELOG`, refuses to start if an applied change set
was edited, and holds a lock in `DATABASECHANGELOGLOCK` so several instances
starting at once apply each change once. Spring Boot's Liquibase support
needs a JDBC data source, so a small runner (`DatabaseMigrations`) starts it
for MongoDB; Liquibase's anonymous usage reporting is switched off.

| Change set | What it does |
|------------|--------------|
| `001-player-indexes` | unique lower-cased usernames, `bests.levelId` for leaderboards |
| `002-move-results-into-players` | moves results from solved attempts into the players' `bests` (an aggregation with `$merge`), drops the indexes and fields the old attempt-based model used |
| `003-expire-old-attempts` | TTL index on attempts; only after 002, so no attempt expires before its result has moved |

## Getting started

The only prerequisite is Docker:

```bash
docker compose up --build
```

To run against your own MongoDB instead, start it on `localhost:27017` and
run `./mvnw spring-boot:run`. The connection string can be overridden with
the `MONGODB_URI` environment variable.

## Tests

```bash
./mvnw verify
```

## License

[MIT](LICENSE)

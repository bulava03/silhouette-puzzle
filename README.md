# Silhouette Puzzle

A picture-building puzzle game in the browser: every level is a picture (a
bus, a house, a rocket) cut into its own parts, and the player drags, turns
and flips the parts until they fill the picture's silhouette. All geometry
lives on the server: a picture counts only when the server has checked that
the placed parts cover the silhouette without overlapping, and the time of a
record is measured by the server, so records cannot be faked from the client.

> Work in progress. The sections below grow together with the project.

## Tech stack

Java 21 · Spring Boot 4 (Web MVC, Security, Validation) · MongoDB 7 ·
Spring Data MongoDB · Liquibase · JTS (Java Topology Suite) · Thymeleaf ·
SVG and vanilla JavaScript · JUnit 5 · Testcontainers · Checkstyle ·
GitHub Actions · Docker

## Structure

```
config/      game beans (geometry, clock) and the Liquibase runner
security/    Spring Security setup and the logged-in player
entity/      Level, Piece, Player, Attempt: what is stored
dto/         what crosses the boundary: the picture check and the registration form
repository/  Spring Data repositories
service/     levels, players and their results, attempts and checking
controller/  the pages and the one JSON endpoint
geometry/    the puzzle geometry on JTS, free of Spring
```

## Pages and the one endpoint

| Path                                | What it is                                                |
|-------------------------------------|-----------------------------------------------------------|
| `GET /`                             | Every picture, with the player's best time                |
| `GET /play/{id}`                    | The game                                                  |
| `GET /leaderboard?level=`           | Podium and table of each player's best time               |
| `GET /login`, `POST /register`      | Log in or register; registering logs the player in        |
| `POST /login`, `POST /logout`       | Spring Security's form login and logout                   |
| `POST /api/levels/{id}/check`       | Checks a picture; the only call the game page makes       |

Everything is rendered on the server with Thymeleaf except the game board,
an SVG driven by a small script with no build step. The game page carries
what the script needs and nothing more: the silhouette and the parts of the
level (never the reference solution) and, for a logged-in player, the id of
a timed attempt the server has just started. The script sends one request
when the player asks to check the picture:

```json
{"attemptId": "6ade4119671b03f9be65ff14", "placements": [{"x": 2, "y": 2, "rotation": 90, "flipped": false}, ...]}
```

and gets the verdict, plus the server-measured time and the player's place
when a timed picture is solved:

```json
{"solved": true, "off": 0.0, "timeMs": 67000, "rank": 2}
```

Errors are RFC 9457 problem details. Fonts are served by the application,
so pages load nothing from other sites.

## What a request can change

| Input                       | Changes                         | Guarded by                                               |
|-----------------------------|---------------------------------|----------------------------------------------------------|
| Registration form           | creates a player                | validation, unique name regardless of case, BCrypt, CSRF |
| Login and logout            | the session only                | Spring Security, CSRF                                    |
| Opening a game page         | a temporary attempt, for logged-in players only | expires after a day, removed after two     |
| Checking a picture          | **the player's best time**, the only thing that matters | time measured by the server; the attempt must be the player's own, open and not expired; it counts once; geometry checked with JTS |

The game also counts moves, but only for the player to see: the server
cannot verify them, so they are never sent and never ranked.

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

Every level is therefore solvable by construction.

A picture counts when the overlap between parts and the difference from the
silhouette both stay within 1% of the silhouette area.

## Data model

Three MongoDB collections:

```
levels    { _id, name, number, silhouette, pieces: [{ name, color, outline, startRotation }], solution }
players   { _id, username, usernameKey, passwordHash, createdAt,
            bests: [{ _id, levelId, startedAt, solvedAt, timeMs }] }
attempts  { _id, levelId, playerId, startedAt, solvedAt, timeMs }
```

A player's best on a level is simply their best solved attempt, so there is
one type for both. Open attempts live in their own collection only to hold
the server-side start time; a TTL index removes them two days after they
start. The best one per level is embedded in the player:

- a player's results are read together with the player;
- the array is bounded by the number of levels, so documents stay small;
- the leaderboard of a level is an aggregation over the players that have
  it (`$match` on an index of `bests.levelId`, `$unwind`, `$sort`), ranked by
  time, then by who was first.

Both changes that matter are conditional updates. An attempt is marked
solved only while it is still open, so of concurrent correct checks exactly
one counts; a best is added only while the level is missing and replaced
only by a better attempt. Tests fire concurrent requests at both.

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
| `002-move-results-into-players` | moves results from solved attempts into the players' `bests` (an aggregation with `$merge`), drops the indexes and fields the first, attempt-based model used |
| `003-expire-old-attempts` | TTL index on attempts; only after 002, so no attempt expires before its result has moved |
| `004-bests-are-attempts` | turns each best into a plain attempt (its start is the solve time minus the time taken) and drops moves, which no longer count |

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

runs the unit and integration tests against MongoDB in Testcontainers.

Browser tests build pictures by dragging, turning and flipping parts as a
player would. They run with Playwright against a separate stack with a
throwaway database on port 8081, built from the same image:

```bash
docker compose -f docker-compose.e2e.yml up -d --build
cd e2e && npm ci && npx playwright install chromium
npx playwright test --output <folder outside synced storage>
```

CI runs both on every push.

## License

[MIT](LICENSE)

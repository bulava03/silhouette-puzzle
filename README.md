# Silhouette Puzzle

A tangram-style puzzle game in the browser: drag, rotate and flip pieces
until they fill the silhouette. All geometry lives on the server: a solution
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

Java 21 · Spring Boot 4 (Web MVC, Validation) · MongoDB 7 ·
Spring Data MongoDB · JTS (Java Topology Suite) · JUnit 5 ·
Testcontainers · Checkstyle · GitHub Actions · Docker

## Levels

Built-in levels live in
[`src/main/resources/levels/tangram.json`](src/main/resources/levels/tangram.json)
and are stored in MongoDB on startup. A level does not spell out its
silhouette: it gives a reference placement of the pieces, and the server
unites them into the silhouette with JTS, rejecting overlaps and holes. Every
level is therefore solvable by construction. The reference solution stays on
the server and is never sent to clients.

## API

| Method | Path               | Description                              |
|--------|--------------------|------------------------------------------|
| GET    | `/api/levels`      | Levels in play order                     |
| GET    | `/api/levels/{id}` | Silhouette and pieces of one level       |

Errors are returned as RFC 9457 problem details.

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

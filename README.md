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

Java 21 · Spring Boot 4 (Web MVC, Validation) · MongoDB 8 ·
Spring Data MongoDB · Checkstyle · GitHub Actions · Docker

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

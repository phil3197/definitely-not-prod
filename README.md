# Definitely Not Prod

> A tiny Spring Boot mock server for APIs you definitely, absolutely, pinky-promise would never run in production.

`Definitely Not Prod` loads API definitions from JSON files and serves mocked HTTP responses without requiring you to write a controller for every fake endpoint.

It is built for local development, integration tests, demos, and those moments when the real upstream API is down, slow, expensive, or emotionally unavailable.

## What it does

- Loads mock definitions from `*.json` files
- Validates them on startup and reload
- Stores active mocks in a thread-safe in-memory registry
- Dispatches requests through a central catch-all controller
- Matches requests by:
  - HTTP method
  - exact path
  - optional query parameters
  - optional headers
  - optional JSON body equality
- Returns configurable status codes, headers, content types, and JSON bodies
- Supports manual reload via admin endpoint
- Logs which mock matched, so debugging stays mildly tolerable

## What it does **not** do

At least not yet:

- no path variables or wildcard routes
- no stateful workflows
- no OpenAPI import
- no auth for admin endpoints
- no file watcher / hot reload
- no "AI-powered enterprise synergy layer" 

## Why this exists

Because sometimes you just want this:

1. write a JSON file
2. start the app
3. fake an external API
4. move on with your life

## Tech stack

- Java 25
- Spring Boot 4.1
- Spring MVC
- Gradle Wrapper
- Jackson
- Bean Validation

## Getting started

### Prerequisites

- Java 25

### Run the app

```bash
./gradlew bootRun
```

By default, the app loads mock definitions from:

```text
./definitions
```

## Configuration

Configuration lives in `application.yml`.

```yaml
app:
  mock-definitions:
    path: definitions
```

All `*.json` files under that directory are loaded recursively.

## Definition files and Git

All JSON files inside `definitions/` are ignored by Git **except**:

```text
definitions/example.json
```

That file is included as a reference definition you can copy, rename, and adapt locally.

In other words: your real mocks can stay private, messy, experimental, or slightly cursed.

## Example definition

The repository ships with:

```text
definitions/example.json
```

It describes a small customer API with:

- `GET /api/customers/42`
- `GET /api/customers/search?status=active`
- `POST /api/customers/create`

You can also keep additional local mock files in `definitions/`, for example a DIVERA mock such as `definitions/divera-alarm-api.json`.
Those files are intentionally ignored by Git unless you explicitly whitelist them.

## Definition format

Each JSON file represents one API definition.

### Top-level structure

```json
{
  "apiName": "customer-api",
  "version": "v1",
  "basePath": "/api/customers",
  "description": "Example customer API mocks",
  "endpoints": []
}
```

### Endpoint structure

```json
{
  "name": "get-customer-by-id",
  "description": "Returns a mocked customer",
  "enabled": true,
  "priority": 10,
  "method": "GET",
  "path": "/42",
  "queryParams": {
    "status": "active"
  },
  "headers": {
    "X-Env": "dev"
  },
  "requestBodyMatch": {
    "name": "Grace Hopper"
  },
  "delayMs": 0,
  "tags": ["example"],
  "response": {
    "status": 200,
    "contentType": "application/json",
    "headers": {
      "X-Mock-Source": "customer-api"
    },
    "body": {
      "id": 42,
      "name": "Ada Lovelace"
    }
  }
}
```

## Matching rules

The MVP uses deterministic matching:

1. exact path match on `basePath + endpoint.path`
2. exact HTTP method match
3. if configured: exact query parameter match
4. if configured: exact header match
5. if configured: exact JSON body equality
6. if multiple endpoints match: higher `priority` wins
7. if priority is equal: earlier loaded definition wins

This is intentionally simple. If your mock routing starts to resemble a rules engine from 2007, that is probably a sign to stop and breathe.

## Response behavior

Each endpoint can return:

- HTTP status
- custom headers
- content type
- JSON body
- optional delay via `delayMs`

## Error behavior

- `404 Not Found` if no mock matches
- `405 Method Not Allowed` if path exists but method does not match
- `400 Bad Request` if body matching is configured and the request body contains invalid JSON
- `500 Internal Server Error` for actual server-side failures

## Admin endpoints

The app exposes a tiny admin surface:

- `GET /admin/registry` — returns the current registry snapshot
- `POST /admin/reload` — reloads and revalidates all definitions

Example:

```bash
curl -X POST http://localhost:8080/admin/reload
```

## Example requests

Run tests:

```bash
./gradlew test
```

Call the sample endpoints:

```bash
curl -i -H 'X-Env: dev' http://localhost:8080/api/customers/42
curl -i 'http://localhost:8080/api/customers/search?status=active'
curl -i -X POST \
  -H 'Content-Type: application/json' \
  -d '{"name":"Grace Hopper"}' \
  http://localhost:8080/api/customers/create
```

## Project structure

```text
src/main/java/com/definitelynotprod/
├── config/
├── controller/
├── domain/
├── exception/
└── service/
```

Rough responsibilities:

- `DefinitionLoader` — reads JSON files
- `DefinitionValidator` — validates structure and conflicts
- `DefinitionRegistry` — stores the active in-memory snapshot
- `RequestMatcher` — decides which mock wins
- `ResponseResolver` — turns a mock definition into an HTTP response
- `MockDispatcherController` — catch-all request entry point
- `ReloadService` / `AdminController` — manual reload support

## Running on GitHub

This README is meant for GitHub, so here is the honest summary:

- the project is intentionally small
- the behavior is intentionally predictable
- the name is intentionally a warning label

If you publish this and someone still points it at production, the README has done all it can.

## Current limitations

- exact paths only
- no path templates
- no partial JSON matching
- no authentication or authorization
- no persistent storage
- no hot reload

## Possible next steps

- path variables like `/customers/{id}`
- smarter body matching
- OpenAPI import
- UI for browsing loaded mocks
- auth for admin endpoints
- file watching for automatic reload

## License

This project is licensed under the **MIT License**.

See [`LICENSE`](./LICENSE) for details.

# esi (Sierra-Lima slice)

Sierra-Lima's contribution to Group 7 of MTAT.03.229 (2026 ESI
QuickBite). This branch (`sten`) is the Sierra-Lima counterpart to
Alfa-Kilo's `anup` branch and contains two Spring Boot microservices:

| Service | Requirement | Tests | Java package |
|---|---|---|---|
| `menu-service/` | R21 add/update/remove menu items, R22 browse menu | 47 | `ee.ut.esi.quickbite.menu` |
| `restaurant-service/` | R19 register/manage restaurant, R20 update open/closed and operating hours | 33 | `ee.ut.esi.quickbite.restaurant` |

Maven `groupId` for both services is `ee.ut.esi.quickbite`. Each
service is a self-contained Maven project (its own `pom.xml`,
`Dockerfile`, Flyway migrations, and per-service PostgreSQL
database).

For per-service detail, layout, and API surface see
[`menu-service/README.md`](menu-service/README.md) and
[`restaurant-service/README.md`](restaurant-service/README.md).

## Run each service locally

```bash
( cd menu-service       && mvn clean test )    # 47/47 pass
( cd restaurant-service && mvn clean test )    # 33/33 pass
```

Each service ships with an `application.properties` (default profile)
and an `application-docker.properties` (container hostname overrides),
plus Flyway migrations under `src/main/resources/db/migration/`.
A team-shared Docker Compose stack does not live in this branch --
add the two services to whatever compose file lives on `develop`
(or the group's integration branch) once this branch is merged.

## For the team lead (merging `sten` -> `develop`)

This branch was started from a personal repository whose history does
not share an ancestor with `develop`. When merging, you will need to
allow unrelated histories:

```bash
git fetch origin
git checkout develop
git merge --allow-unrelated-histories origin/sten
```

(GitHub's web "Create a merge commit" flow handles this automatically
in the PR review screen.)

Two files will conflict:

- **`.gitignore`** -- `sten`'s version is a strict superset of
  `develop`'s. It adds `.idea/` (covers the four `.idea/*` entries
  on `develop`), `.claude/`, `*.iml`, `target/`, `.env.local` (and
  recursive variant), and `HELP.md`. Recommended resolution: keep
  `sten`'s version.
- **`README.md`** -- this file. After merge, you will likely want a
  unified group-wide README rather than this Sierra-Lima-only
  handover doc. Replace or merge at your discretion.

The two service directories themselves (`menu-service/` and
`restaurant-service/`) sit at paths that do not exist on `develop`,
so they will land cleanly without conflict alongside `order-service/`
and `user-service/` from the `anup` merge.

## Conventions in this slice

- Java 17, Spring Boot, Maven.
- Java package root: `ee.ut.esi.quickbite.<service>`.
- One PostgreSQL database per service (no shared DB).
- JWT auth (issuer-pinned HS256), bearer token in `Authorization`.
- Flyway migrations, append-only.
- Errors flow through a per-service `GlobalExceptionHandler` with a
  shared error-envelope shape.
- Cross-service references are by UUID; no cross-service foreign
  keys.

## Owner

Sierra-Lima (Sten-Qy-Li, MSc Computer Science, University of Tartu,
Group 7).

Source of this branch: commit
[`44fffd3`](https://github.com/Sten-Qy-Li/2026-esi-quickbite-personal/commit/44fffd3)
of `https://github.com/Sten-Qy-Li/2026-esi-quickbite-personal`,
restructured to mirror the layout of the `anup` branch (services at
the repository root). The full design history (decisions, audits,
gap analyses, chat archives) lives in the personal repository under
`dev-docs/` and is intentionally not part of this branch.

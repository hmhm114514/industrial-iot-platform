# AGENTS.md

## Repo shape

- Course-practice prototype, not a production platform: core IoT loop is real; video, screen, firmware, and service-monitor modules are partly demo implementations.
- Main roots: `backend/` Spring Boot API, `frontend/` Vue3/Vite SPA, `database/` MySQL handoff scripts, `docs/` course reports.
- There is no top-level build orchestrator; run backend and frontend commands from their own directories.

## Commands that matter

- Backend verify: `cd backend && mvn -q -DskipTests package`.
- Backend dev server: `cd backend && mvn spring-boot:run` on `http://localhost:8080`.
- Frontend verify: `cd frontend && npm install && npm run build`.
- Frontend dev server: `cd frontend && npm install && npm run dev` on `http://localhost:5173`; Vite proxies `/api` to `http://localhost:8080`.
- Use `npm`, not pnpm/yarn, unless intentionally changing the project; README and prior validation use `npm install` despite a stale `pnpm-lock.yaml` being present.
- No lint/typecheck/test scripts are defined. Build is the practical verification step.
- After verification, remove generated artifacts before handoff: `backend/target/`, `backend/data/`, `frontend/node_modules/`, `frontend/dist/`, and any `backend/bin/`.

## Runtime defaults

- Default login: `admin / 123456`; demo token is `panda-iot-demo-token`.
- Backend protects `/api/**` with the simple token except login/H2 console.
- Default DB is H2 file mode: `jdbc:h2:file:./data/iot-platform-db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE`.
- H2 Console is at `http://localhost:8080/h2-console`; `backend/data/` is runtime state and should not be submitted.
- MySQL is optional handoff only unless explicitly requested: scripts live in `database/schema-mysql.sql` and `database/data-mysql.sql`; backend already has `mysql-connector-j`.

## Backend wiring notes

- Java package is `com.practice`; many package-private controllers/entities/repos are intentionally consolidated into a few files:
  - `Controllers.java`: REST endpoints and generic CRUD/toggle logic.
  - `PlatformService.java`: login, telemetry simulation, rule evaluation, alarm handling, task switching, logs, dashboard data.
  - `Entities.java` / `Repositories.java`: JPA model and repositories.
  - `DataInitializer.java`: seed data used by demos.
- Most resource controllers extend `CrudController<T>` and support list/get/create/update/delete plus `POST /api/{resource}/{id}/toggle`.
- Rule execution uses `rules.findByEnabledTrue()`. If changing rule toggles or status handling, keep `Rule.enabled` synchronized with `status`, or closed rules will still fire alarms.
- Task state uses both `TaskJob.running` and `status` (`RUNNING`/`STOPPED`); update both together.

## Frontend wiring notes

- Routes are generated from `frontend/src/config/menu.js`; most menu items render `GenericResource.vue` using field/API metadata from `frontend/src/config/resources.js`.
- Special pages are wired in `frontend/src/router/index.js` (`Dashboard`, `Rules`, `HistoryData`, `HistoryAlarms`, `Tasks`, `DeviceMap`, `VisualScreens`, `VideoSquare`).
- API paths and response adaptation live mainly in `frontend/src/api/platform.js`; keep this in sync with backend `/api` paths in `Controllers.java`.
- Frontend has fallback demo data for some pages when backend is unavailable; do not treat fallback-only behavior as proof an API works.

## Core demo flow to preserve

- Login → dashboard → device management → `POST /api/telemetry/simulate` with high temperature (e.g. `88.6`) → enabled rule creates `RuleAudit` + `Alarm` → historical data/alarms show changes → handle alarm → logs/dashboard update.
- Regression check worth doing after backend/API changes: with the temperature rule enabled, high temp creates an alarm; after toggling the rule off, the same high temp must not create a new alarm.

## Documentation constraints

- `docs/` contains the course deliverables; keep docs aligned with actual code and the “core real / extensions demo” scope.
- Do not leave internal workflow terms, TODO placeholders, or build-output references in student-facing docs/README.

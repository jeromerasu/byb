# TASK-BE-012 — API/DB - Persist User Theme Preferences (Accent + Panel Style)

## Goal
Add backend persistence for user theme customization, including DB schema updates and profile endpoints for cross-device sync.

## Priority
High

## Scope
Repo: `byb`
Area: user table schema, entity/model, DTOs, controller/service for theme preference read/write

## In Scope
- Add DB column(s) to `user` table to support theme customization (explicit migration required), e.g.:
  - `theme_accent` (varchar)
  - `panel_style` (varchar)
- Update `User` model/entity and related DTO mapping
- Add auth-scoped endpoints:
  - `GET /api/v1/user/theme-preferences`
  - `POST /api/v1/user/theme-preferences`
- Validate allowed values and provide safe defaults for existing users
- Ensure deterministic JSON responses
- Include backward compatibility strategy for users without preferences

## Out of Scope
- Frontend UI implementation
- Analytics/reporting

## Constraints
- User can only read/write their own preferences
- Migration must be explicit and reversible
- Must satisfy TESTING_REQUIREMENTS.md runtime proof before DONE

## Acceptance Criteria
1. User table contains new theme preference column(s) with migration applied.
2. Authenticated user can read and update theme preferences via API.
3. Existing users receive defaults without breaking profile reads.
4. Invalid values are rejected with clear errors.

## Test Steps
1. Start backend with test profile and migration path.
2. Register/login user and fetch default theme preferences.
3. Update preferences and re-fetch to verify persistence.
4. Validate unauthorized/invalid payload behavior.

## Deliverables
- Commit hash
- Changed files
- Migration files
- Request/response examples
- Runtime proof block per TESTING_REQUIREMENTS.md

## Status
READY

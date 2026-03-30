# TASK-P1-006 — Frontend Auth Token Secure Storage

## Goal
Replace in-component JWT state with `expo-secure-store` so the token persists across app restarts, enabling auto-login for returning users and a proper token refresh and logout flow.

## Priority
High

## Scope
Repo: `byb-frontend`
Area: authentication, token storage, session management

## In Scope
- Move JWT access token (and refresh token if present) from component/context state to `expo-secure-store`
- Token persists across app restarts — user remains logged in without re-entering credentials
- On app launch, check `expo-secure-store` for a valid token; if found, auto-login and navigate to home screen
- Token validation on launch: check expiry from JWT payload; if expired, attempt refresh or redirect to login
- Token refresh flow: use stored refresh token to obtain a new access token when the current one expires
- Logout clears both access and refresh tokens from `expo-secure-store`
- Consistent auth state exposed via a React context or hook (e.g., `useAuth`)
- All API calls read the token from the central auth context (not from local component state)

## Out of Scope
- Backend auth changes
- Biometric authentication
- Multi-account support

## Dependencies
- None — fully independent frontend task

## Acceptance Criteria
1. JWT token is written to `expo-secure-store` on successful login, not held in component state.
2. App restart with a valid stored token results in automatic navigation to the home screen (no login required).
3. App restart with an expired or missing token navigates to the login screen.
4. Token refresh is attempted when the access token expires during an active session; on refresh failure, the user is redirected to login.
5. Logout removes all tokens from `expo-secure-store` and navigates to the login screen.
6. All outgoing API calls attach the Bearer token from the central auth context.

## Files Likely Affected
- `byb-frontend/src/auth/useAuth.ts` (new or refactor existing)
- `byb-frontend/src/auth/AuthContext.tsx` (new or refactor existing)
- `byb-frontend/src/api/apiClient.ts` (update to read token from auth context/store)
- Login screen / component (update to write token to secure store on success)
- App entry point / navigation root (add auto-login check on startup)
- `package.json` (add `expo-secure-store` dependency if not already present)

## Test Steps
1. Log in with valid credentials; verify token is written to `expo-secure-store` (not just component state).
2. Close and reopen the app; verify the home screen loads without prompting for credentials.
3. Manually expire the stored token (or test with a very short expiry); verify redirect to login or successful refresh.
4. Tap logout; verify token is cleared from `expo-secure-store` and login screen is shown.
5. Reopen the app after logout; verify login screen is shown (no auto-login).
6. Make an authenticated API call; verify `Authorization: Bearer <token>` header is present.

## Deliverables
- Commit hash
- Changed files
- Description of the storage key names used in `expo-secure-store`
- Notes on token expiry detection approach (decode JWT payload, compare `exp` to `Date.now()`)

## Status
`READY`

## Notes
- Independent of all backend P1 tasks — can be developed in parallel
- TASK-P1-008 depends on this task
- Use `expo-secure-store` keys like `auth_access_token` and `auth_refresh_token` (or similar) — document the keys chosen
- Token expiry check: decode the JWT payload (base64), read `exp` field, compare to current timestamp; no crypto verification needed on the client

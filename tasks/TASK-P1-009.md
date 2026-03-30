# TASK-P1-009 — Fix/Delete 8 Excluded Broken Test Files

## Goal
Resolve the 8 test files currently excluded from compilation and test execution so that `mvn test` runs clean with no exclusion hacks in `pom.xml`.

## Priority
High

## Scope
Repo: `byb`
Area: test files, `pom.xml` exclusion configuration

## In Scope
Triage and fix or delete each of the following excluded test files:

| Test File | Action |
|---|---|
| `ExerciseTest` | Fix or delete |
| `WorkoutPlanTest` | Fix or delete |
| `UserProfileTest` | Fix or delete |
| `DietPlanTest` | Fix or delete |
| `WorkoutPlanControllerTest` | Fix or delete |
| `OpenAIServiceTest` | Fix or delete |
| `DietProfileTest` | Fix or delete |
| `DatabaseConfigTest` | Fix or delete |

For each file:
- Read the test to understand its intent
- Determine whether it tests logic that still exists in the current codebase
- If yes: update the test to compile and pass against the current codebase
- If no (tests deleted/replaced logic): delete the file
- Document the decision (fix or delete) and brief reason in deliverables

Remove exclusions from `pom.xml`:
- `maven-compiler-plugin` `<excludes>` list
- `maven-surefire-plugin` `<excludes>` list

Run `mvn test` and confirm a clean build with all remaining tests passing.

## Out of Scope
- Writing new tests for untested functionality (separate tasks)
- Changing production code solely to make old tests pass — if the test is stale, delete it

## Dependencies
- None — independent backend task

## Acceptance Criteria
1. All 8 test files are either fixed (compile and pass) or deleted with documented rationale.
2. `pom.xml` contains no exclusions in `maven-compiler-plugin` or `maven-surefire-plugin` for these files.
3. `mvn test` runs to completion with zero failures and zero compilation errors.
4. No new test failures introduced in previously passing tests.

## Files Likely Affected
- `src/test/java/.../ExerciseTest.java`
- `src/test/java/.../WorkoutPlanTest.java`
- `src/test/java/.../UserProfileTest.java`
- `src/test/java/.../DietPlanTest.java`
- `src/test/java/.../WorkoutPlanControllerTest.java`
- `src/test/java/.../OpenAIServiceTest.java`
- `src/test/java/.../DietProfileTest.java`
- `src/test/java/.../DatabaseConfigTest.java`
- `pom.xml`

## Test Steps
1. Read each excluded test file and document its intent.
2. Check whether the tested class/method still exists in the current codebase.
3. For each file: fix imports and update assertions to match current API, or delete if stale.
4. Remove exclusion entries from `pom.xml`.
5. Run `mvn clean test`; confirm zero failures.
6. Run `mvn clean compile` as a sanity check.

## TDD + Unit Test Coverage (required)
- N/A — this task is test cleanup, not new feature work
- Ensure no coverage regression: fixed tests should contribute to, not reduce, overall coverage

## Mandatory Local Testing Verification (required)
- Run `mvn clean test` and capture full output
- Confirm zero test failures and zero compilation errors
- Confirm the excluded lists in `pom.xml` are empty or removed

## Deliverables
- Commit hash
- Changed files
- Decision log: for each of the 8 files, "fixed" or "deleted" with a one-line reason
- `mvn clean test` output showing clean result

## Status
`READY`

## Notes
- Independent of all other P1 tasks — can run in parallel
- Prefer deleting stale tests over writing large amounts of new test code to resurrect them — the goal is a clean build, not inflating test count with low-value tests
- If a test requires mocking a dependency that no longer exists, delete the test rather than adding the old dependency back

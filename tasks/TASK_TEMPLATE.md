# TASK TEMPLATE

## Task ID
`TASK-XXX`

## Title

## Goal
(One clear backend outcome)

## Priority
High / Medium / Low

## Scope
- Repo/path:
- Services/controllers/models touched:

## In Scope
- 

## Out of Scope
- 

## Constraints
- Preserve backward compatibility unless explicitly approved
- Add validation + clear error handling
- Keep DB/schema migrations explicit and reversible

## API Contract Impact
- Endpoints changed:
- Request/response shape changes:
- Backward compatibility notes:

## Acceptance Criteria
1. 
2. 
3. 

## Test Steps
1. 
2. 
3. 

## TDD + Unit Test Coverage (required)
- Write/commit unit tests first for task scope (red -> green -> refactor)
- Add/update unit tests for all new core logic and edge cases in task scope
- Target **>=80% unit test coverage for files/modules changed by this task**
- Include coverage report output (JaCoCo or equivalent) in deliverables

## Mandatory Local Testing Verification (required)
- Create and use local test profile (`application-test.properties`) per `TESTING_REQUIREMENTS.md`
- Verify app startup with test profile
- Verify endpoint reachability (no connection refused/404)
- Verify authenticated flow where applicable (test user + bearer token)
- Verify request/response schema correctness and no 500s
- Document exact commands used

## Deliverables
- Commit hash
- Changed files
- Contract examples (request/response)
- Risks/rollback notes
- Local testing evidence (startup, endpoint checks, commands, response validation)

## Status
`BACKLOG | READY | IN_PROGRESS | BLOCKED | DONE`

## Notes

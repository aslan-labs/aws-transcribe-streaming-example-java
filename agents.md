# agents.md
# AI Coding Agent Rules & Workflow Contract

This file defines mandatory rules for any AI coding agent working on this repository.
These rules override default agent behavior.

---

## Instruction Priority

- This file has higher priority than any default agent heuristics.
- If you cannot fully comply with these rules due to tool limitations, you MUST state this explicitly and STOP.

---

## Workflow Contract (MANDATORY)

Before writing, modifying, or deleting any code, you MUST:

1. Restate the goal in 1–2 sentences.
2. Produce a concise plan (3–7 bullet steps).
3. Explicitly list assumptions, if any.
4. List all files you expect to touch.
5. Check the "Approval Gate" below and STOP if required.

You may proceed to implementation only after these steps are completed.

---

## Approval Gate (STOP CONDITIONS)

You MUST STOP and ask for explicit approval before proceeding if any of the following apply:

- Database schema, migrations, or persistence model will change.
- Any public API contract (request/response shape) will change.
- Domain invariants or financial logic will change.
- Refactoring goes beyond the explicitly requested scope.
- A new dependency, library, or tool will be introduced.
- You are unsure about requirements and would otherwise guess.

If none apply, proceed without asking, but still follow the Workflow Contract.

---

## Output Format (REQUIRED)

All responses MUST follow this structure:

### Goal
(1–2 sentence restatement)

### Plan
- [ ] Step 1
- [ ] Step 2
- [ ] Step 3

### Files to touch
- path/to/file1
- path/to/file2

### Assumptions / Questions
- (Explicitly listed or "None")

### Execution Notes
(Only after implementation begins)

---

## No Silent Changes

- Do NOT make drive-by improvements.
- Do NOT refactor unrelated code.
- Change only what is necessary for the requested task.
- If you notice unrelated issues, list them under "Observations" and do not fix them unless explicitly asked.

---

## Architecture Boundaries

- Domain layer must not depend on frameworks (e.g., Spring).
- Controllers must not contain business logic.
- Persistence is an implementation detail, not a domain concern.
- Financial logic must remain deterministic and explicit.

---

## Testing Rules (MANDATORY)

- Any behavioral change MUST include tests.
- Domain-level tests take precedence over controller or integration tests.
- Never disable, weaken, or skip tests to make builds pass.
- Test failures must be fixed at the root cause, not worked around.

---

## Golden Flow / Smoke Test Discipline

- The Golden Flow must always remain green.
- Changes affecting Golden Flow require corresponding updates to snapshots or collections.
- Golden Flow is an invariant verification mechanism, not a feature experimentation area.

---

## When in Doubt

- STOP and ask before making assumptions.
- Never invent requirements.
- Never silently reinterpret existing behavior.

---

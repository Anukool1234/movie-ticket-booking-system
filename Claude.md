# Claude.md

Companion notes for AI-assisted development of this project (same content focus as `Agents.md`).

## Prompting approach

- Start from the PDF requirements; ask for a **simple, explainable** Spring Boot design.
- Prefer readable packages (`entity`, `service`, `controller`) over frameworks that hide behavior.
- Request comments on non-obvious rules (holds, refunds, optimistic locking).
- Ask for tests covering the happy path + double-booking + admin RBAC.
- Keep generating in small layers and verify with Maven after each major step.

## Useful follow-up prompts (for future changes)

- “Add Postgres profile without changing the domain model”
- “Add an integration test for expired hold release”
- “Expose an admin endpoint to list all bookings”

## Do not over-build

If extending this project, keep the same principle: **one clear flow, few moving parts, easy to demo**.

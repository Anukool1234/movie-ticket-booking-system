# Agents.md — AI-assisted development notes

This file documents how AI assistance was used while building the Movie Ticket Booking System (as required by the take-home).

## Goal

Implement a Spring Boot movie ticket booking backend that matches the PDF requirements, while keeping the code **simple enough to explain in a short video**.

## AI workflow used

1. **Requirement extraction** — Read the assignment PDF and listed in-scope items (REST, DB, RBAC, validation, holds, pricing, payment, refunds, async notifications, tests).
2. **Scoping decisions** — Chose a single-module monolith with H2, HTTP Basic RBAC, optimistic locking, and simulated payment/notifications. Documented assumptions in `README.md`.
3. **Incremental generation** — Built in layers:
   - Maven + config
   - Entities & repositories
   - Security (database-backed HTTP Basic)
   - Services (admin, pricing, booking, notification)
   - Controllers
   - Seeder + scheduler
   - Unit & integration tests
   - README / this Agents file
4. **Simplification pass** — Avoided extra layers (no CQRS, no event bus, no microservices). Comments explain *why* each important piece exists.
5. **Verification** — Ran `mvn test` / `mvn spring-boot:run` to validate the core booking flow.

## Skills / practices applied during development

| Skill | How it was used |
|-------|-----------------|
| Domain modeling | City → Theater → Seat → Show → ShowSeat → Booking |
| Spring Boot REST | Controllers + DTOs + validation annotations |
| JPA / transactions | `@Transactional` on hold/pay/cancel |
| Concurrency | `@Version` optimistic locking on `ShowSeat` |
| Security | HTTP Basic + `ROLE_ADMIN` / `ROLE_CUSTOMER` |
| Scheduling | Release expired holds; queue reminders |
| Async messaging (lite) | `@Async` notification service |
| Testing | Mockito unit tests + `@SpringBootTest` integration tests |
| Clear documentation | README assumptions + commented code for walkthrough |

## What was intentionally left simple

- No frontend / UI
- No Docker / CI/CD
- No real payment gateway or email provider
- No OAuth / SSO / MFA
- No distributed locking / Redis (optimistic DB locking is enough for this scope)

## Raw inputs used

- Assignment PDF: *SDE-2 Take-Home — Movie Ticket Booking System*
- This `Agents.md` file and the project `README.md` as living docs during development

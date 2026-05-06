# Enterprise System Integration in a Food-Delivery App

Project spec: https://courses.cs.ut.ee/2026/esi/spring/Main/Lectures?action=download&upname=Project2026.pdf

For grading of project work by Group 7 at the Checkpoint 2 stage.

## Grading rubrics for Checkpoint 2

**Total: 8 points.** Deadline: last commit at **12 May 2026, 14:00 (Estonian time)**; team discussions start the same day at 14:15.

**Goal:** Complete backend responsibilities and start system integration. Security is not assessed at this checkpoint (it is graded at Checkpoint 3).

**Per-student requirement:** Each student must show their **second service** implemented, OR their **integration/resilience component** implemented and usable. Tests and documentation are **not** required for this second responsibility — only points A, B, and D from Checkpoint 1 apply (Running Service, API Implementation, Persistence).

### Deliverables (per student)

- [ ] **A. Second Responsibility — 4 pts:** second service OR integration/resilience component runs
    - [ ] Running Service: service starts and endpoints are accessible
    - [ ] API Implementation: all endpoints from Assignment 3 implemented
    - [ ] Persistence: database connected; data stored and retrieved (services only)
    - [ ] Layered structure respected: Controller → DTO → Service → Repository → Domain
    - [ ] ~5–8 REST endpoints exposed (services only)
- [ ] **B. Basic Integration — 2 pts:** at least one working interaction between two implemented services demonstrated via a **real call** (not mocked)
- [ ] **C. Initial Frontend — 2 pts:** frontend exists, calls at least one backend endpoint per student, and displays real data

### Grading summary

| # | Criterion                       | Points |
|---|---------------------------------|--------|
| A | Second service/component runs   | 4      |
| B | Basic integration (real call)   | 2      |
| C | Initial frontend                | 2      |
|   | **Total**                       | **8**  |

### Demonstration

- [ ] API demo of already implemented endpoints via Postman or the frontend (no Swagger required for the second responsibility)

## To course instructors: How to quickly verify that code repository meets grading rubrics for Checkpoint 2



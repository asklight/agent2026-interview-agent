# Interview Training Upgrade Design

## Goal

Expand the interview question bank from Xiaolin Coding topic coverage without copying source prose, reconnect the TJU EasyConnect VPN every hour, rebuild the training UI, and add the missing post-session report.

## Question Bank

The ingestion process discovers the public topic hierarchy and page titles from Xiaolin Coding. It does not store article paragraphs, diagrams, or code samples. A checked-in generator converts the topic list into independently authored `question_card` records with the existing fields: module, difficulty, main question, key points, mistakes, follow-ups, scenario follow-ups, scoring rubric, and tags.

Coverage includes Java foundations and concurrency, MySQL, Redis, Spring, computer networks, and operating systems. Each card is source-attributed at the collection level in documentation, while all training prompts are newly written for this product.

## VPN Reliability

The EasyConnect entrypoint runs its login, route-repair, and tinyproxy processes under a one-hour supervisor. At the interval boundary it stops those child processes and repeats the normal initialization sequence. The container remains alive, so Docker health checks and the server dependency continue to work without relying on an external cron job.

## Report

`interview_report` stores one report per session. The report is generated idempotently when a session finishes, using saved answer scores and evaluation records. It includes total score, score band, answered count, strengths, weaknesses, and actionable recommendations. `GET /api/interview-sessions/{id}/report` returns the persisted result.

## UI

The single-page frontend becomes an interview command center: a concise setup panel before the session, a prominent question and answer workspace during it, a visible progression rail, structured evaluation cards, and a full report dashboard after completion. It preserves current endpoints and progressively loads the new report endpoint after finishing.

## Validation

The implementation is validated through backend tests for report generation, type-check/build for the Vue application, a generated-card count check, and shell syntax validation for the VPN entrypoint.

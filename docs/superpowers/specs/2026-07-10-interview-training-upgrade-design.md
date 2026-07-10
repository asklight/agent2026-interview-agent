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

### Compact Training Workspace

On desktop widths of 1080px and above, the active training state occupies exactly the viewport height. The document body does not scroll. The layout is a compact three-column grid: a 248px control rail, a flexible question-and-answer workspace, and a 320px feedback rail. The middle workspace places the question in a short fixed-height card and gives all remaining space to the answer editor. The editor body, feedback list, and feedback summary are independently scrollable, so long text never increases the page height.

The report and welcome states retain ordinary vertical scrolling because their content is read rather than actively composed. Below 1080px, the application switches to its existing single-column responsive layout and restores document scrolling; no mobile content is clipped. The finish and submit controls remain visible at the bottom of the answer workspace on desktop.

This change does not alter API calls, component state, question flow, or report generation.

### Tablet and Phone Adaptation

At tablet widths from 641px through 1080px, active training keeps a compact two-column viewport: a 220px configuration rail and a flexible question-and-answer workspace. AI feedback is removed from the permanent layout and appears as a user-controlled floating panel, so it does not reduce editor width or force document scrolling.

At phone widths of 640px and below, training uses a single-column document flow. The feedback control is a fixed bottom action that opens a modal bottom sheet with a backdrop; the sheet owns its own scrolling content and can be dismissed by the close action or backdrop. The desktop feedback rail remains unchanged above 1080px. Report pages continue to use normal scrolling across all breakpoints.

## Validation

The implementation is validated through backend tests for report generation, type-check/build for the Vue application, a generated-card count check, and shell syntax validation for the VPN entrypoint.

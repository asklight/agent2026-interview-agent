# Java Interview Agent Design

## Project Summary

The project is a Java backend technical interview training agent for Tianjin University students preparing for software engineering internships and full-time recruitment.

Recommended repository name: `agent2026-java-interview-agent`

Recommended product name: `北洋 Java 面试官`

The agent should not behave like a generic chatbot or simple question bank. It should simulate a Java backend technical interviewer, ask one focused question at a time, evaluate the user's response, decide whether to follow up, and generate a structured review report after the session.

## Competition Fit

This design is optimized for the technical implementation track of the Tianjin University AI Agent competition.

Key rule alignment:

- The repository name should use the required `agent2026-` prefix.
- All model calls must use the competition-specific `tju-llm` API endpoint so platform usage can be counted.
- The API key must only be stored in backend environment variables and must not be committed to Git.
- The application should be stable enough for ordinary campus users to try before the public evaluation period.
- The repository should include `README.md`, `DESIGN.md`, screenshots or a demo video, and the copyright compliance commitment file.
- The project should use original question cards and examples, not copied commercial interview-bank content.
- Resume or project text input should warn users to remove sensitive personal information.
- Raw audio should not be stored if voice input is implemented.

The competition deadline is 2026-09-30 22:00. A usable internal release should be ready before 2026-09-01 to collect campus user feedback and platform evaluation data.

## Target Users

Primary users:

- Tianjin University students preparing for Java backend internships.
- Students preparing for spring recruitment, autumn recruitment, or technical interviews.
- Students who need repeated practice on Java backend knowledge, project explanation, and algorithm thinking.

Out of scope for the first release:

- Non-Java positions.
- Full resume parsing from PDF or images.
- Real-time voice-call style interviews.
- Online judge execution for algorithm code.
- Multi-user ranking or social features.

## Core Product Modes

### 1. Java Core Knowledge Interview

This replaces the earlier "八股追问" naming. It is a formal Java backend knowledge interview mode.

Purpose:

- Train high-frequency Java backend interview knowledge.
- Diagnose weak modules.
- Practice answering with structure and depth.

Initial modules:

- Java basics and collections
- JVM
- Java concurrency
- MySQL
- Redis
- Spring and Spring Boot
- Computer networks
- Operating systems

Interaction model:

1. The user selects modules, difficulty, and training intensity.
2. The agent asks a main question.
3. The user answers by typing or voice-to-text.
4. The evaluator scores the answer against structured key points.
5. The agent either asks a follow-up, gives a small hint, or moves to the next question.
6. At the end, the report generator summarizes the user's strengths, weak points, and next recommended practice.

Training intensities:

- Quick check: broad coverage, at most one follow-up per question.
- Standard interview: two or three follow-ups for important questions.
- Deep dive: multiple follow-ups around one topic until the user reaches scenario-level understanding.

### 2. Project Experience Deep Dive

This mode starts from the user's own project or resume fragment.

Purpose:

- Simulate how interviewers question real project experience.
- Test project authenticity, technical ownership, design reasoning, and trade-off awareness.
- Help users prepare better descriptions of their own work.

Input format:

- Project name
- Technology stack
- User responsibility
- Key features
- Performance, reliability, or engineering highlights

The first version should use pasted text, not PDF upload.

Example input:

> 基于 Spring Boot、MySQL 和 Redis 的校园二手交易平台，负责商品发布、搜索、登录鉴权和热门商品缓存。

Example follow-ups:

- Why did this project need Redis?
- How did you design cache keys?
- How do you keep cache and database data consistent?
- How did you design the login and permission model?
- What bottleneck would appear first if traffic increased?
- Which part was implemented by you personally?

Boundary with Java Core Knowledge Interview:

- Java Core Knowledge Interview starts from standardized knowledge points.
- Project Experience Deep Dive starts from the user's project context.
- Both can reuse the same follow-up engine, but the user-facing modes must remain separate.

### 3. Algorithm Reasoning Interview

This mode trains algorithm communication for Java backend interviews.

First-release scope:

- Oral algorithm reasoning
- Complexity analysis
- Boundary case analysis
- Java pseudocode or pasted Java code review

Out of scope:

- Running untrusted user code
- Full online judge functionality
- Large algorithm problem library

The agent should ask one common interview-style problem, request the user's approach, ask for time and space complexity, probe edge cases, and then provide feedback.

### 4. Comprehensive Mock Interview

This mode combines project, Java core knowledge, and algorithm reasoning into a complete technical first-round interview.

Recommended structure:

- 2 minutes: self-introduction or project entry
- 5 minutes: project deep dive
- 8 minutes: Java backend core knowledge
- 5 minutes: one algorithm reasoning problem
- 2 minutes: closing summary

The actual duration can be shortened for demos. The important behavior is that the agent can plan a session, maintain state, and choose follow-ups based on previous answers.

### 5. Voice-to-Text Answering

Voice should improve interview realism without becoming the main technical risk.

First-release behavior:

- The user can answer by typing.
- The user can optionally click or hold a microphone button to dictate.
- The recognized text is shown in the answer box.
- The user can edit the transcript before submitting.
- The agent evaluates only the confirmed text.

Privacy and safety:

- Do not store raw audio.
- Store only confirmed answer text when needed for history or reports.
- Warn users not to speak private personal information.
- Always keep typed input as a fallback.

### 6. Structured Review Report

Every training mode should end with a structured report.

Report fields:

- Overall score
- Module scores
- Strong points
- Missing key points
- Incorrect statements
- Interview-risk expressions
- Recommended next training modules
- Three suggested follow-up practice questions

The report is important for user retention and competition demonstration. It shows that the agent is not only asking questions but also diagnosing ability.

## System Architecture

Recommended architecture:

- Frontend: React or Vue with TypeScript
- Backend: Spring Boot or FastAPI
- Database: SQLite for simple local deployment, or MySQL if the deployment environment is stable
- Model: competition-specific `tju-llm` endpoint

Spring Boot is preferred if the team wants the implementation itself to align with Java backend positioning. FastAPI is acceptable if development speed is the main priority.

Backend modules:

- Interview Planner: builds the session plan from selected mode, modules, difficulty, and project text.
- Question Engine: selects main questions and follow-up questions.
- Answer Evaluator: compares user answers with key points and returns structured scores.
- Follow-up Decider: decides whether to probe deeper, give a hint, move on, or close the session.
- Report Generator: creates the final structured review.
- Session Manager: stores session state and message history.
- LLM Client: wraps all `tju-llm` API calls, rate-limit handling, retries, and error mapping.

## Question Card Structure

The first release should use structured local question cards instead of relying entirely on model-generated questions.

Suggested fields:

- `id`
- `module`
- `difficulty`
- `main_question`
- `key_points`
- `common_mistakes`
- `followups`
- `scenario_followups`
- `scoring_rubric`

Example:

```json
{
  "id": "redis-cache-consistency",
  "module": "Redis",
  "difficulty": "medium",
  "main_question": "缓存和数据库如何保持一致？",
  "key_points": ["更新数据库后删除缓存", "延迟双删", "binlog/Canal", "最终一致性"],
  "common_mistakes": ["先更新缓存再更新数据库", "认为强一致性总是必要"],
  "followups": ["为什么不推荐先删缓存再更新数据库？", "延迟双删有什么问题？"],
  "scenario_followups": ["高并发商品详情页如何避免缓存击穿？"],
  "scoring_rubric": ["方案准确性", "场景意识", "一致性理解", "风险表达"]
}
```

## Data Flow

Typical interview flow:

1. User selects a mode.
2. Frontend sends session settings to the backend.
3. Interview Planner creates a session plan.
4. Question Engine returns the first question.
5. User submits an answer.
6. Answer Evaluator calls `tju-llm` and returns structured evaluation data.
7. Follow-up Decider chooses the next action.
8. Frontend displays the next question, hint, or transition.
9. After the planned session ends, Report Generator creates the review report.

Model outputs should be requested in structured JSON where possible so the UI can reliably render scores, missing points, and next actions.

## UI Plan

The first screen should be the usable training interface, not a marketing landing page.

Main screens:

- Mode selection: Java Core Knowledge Interview, Project Experience Deep Dive, Algorithm Reasoning Interview, Comprehensive Mock Interview.
- Session setup: modules, difficulty, duration, voice input toggle, optional project text.
- Interview screen: current question, answer box, microphone button, progress, current module, and action buttons.
- Report screen: scores, analysis, missing points, recommended next training.
- History screen: optional for first release; useful if time allows.

Action buttons:

- Submit answer
- I need a hint
- Skip this question
- Explain after this round
- End interview and generate report

## Scoring Model

Each answer can be scored from 0 to 5 on:

- Correctness
- Completeness
- Depth
- Structure
- Scenario awareness

Project mode adds:

- Ownership clarity
- Technical trade-off reasoning
- Project authenticity

Algorithm mode adds:

- Problem decomposition
- Complexity analysis
- Boundary case handling

The final report should aggregate these scores into module-level and session-level results.

## Release Plan

### Phase 1: Core MVP

- Java Core Knowledge Interview
- Basic question cards for Java, MySQL, Redis, and Spring
- One-question-at-a-time interview flow
- Dynamic follow-up
- Structured report
- Text input
- Competition API wrapper

### Phase 2: Differentiation

- Project Experience Deep Dive
- Comprehensive Mock Interview
- More modules: JVM, concurrency, networks, operating systems
- Session history
- Better report formatting

### Phase 3: Experience Polish

- Voice-to-text input
- Algorithm Reasoning Interview
- More stable prompts and JSON output validation
- Demo data and guided sample sessions
- Screenshots and demo video

### Phase 4: Competition Finalization

- `README.md`
- `DESIGN.md`
- Demo video or screenshots
- Copyright compliance commitment
- Internal visibility settings
- API key safety check
- Final test before 2026-09-30 22:00

## Key Risks and Mitigations

Risk: Scope becomes too large.

Mitigation: Build Java Core Knowledge Interview and report first. Add voice, algorithm, and history only after the main flow is stable.

Risk: Model evaluations are unstable.

Mitigation: Use structured question cards, fixed scoring rubrics, and JSON-format model responses.

Risk: Project deep dive becomes generic.

Mitigation: Extract the user's tech stack and responsibilities first, then generate questions tied directly to those details.

Risk: Voice input blocks progress.

Mitigation: Make voice optional and keep typed answers as the primary fallback.

Risk: API key leaks.

Mitigation: Only call the model from the backend and load secrets from environment variables.

Risk: Content copyright issues.

Mitigation: Write original question cards and avoid copying commercial interview-bank explanations.

## Success Criteria

The project is successful for the competition if:

- A student can complete a Java backend mock interview without manual setup.
- The agent can ask meaningful follow-ups based on user answers.
- The final report is specific enough to guide the next practice session.
- The UI is simple enough for campus users to try repeatedly.
- The codebase demonstrates clear agent architecture rather than a single generic chat prompt.
- The submission materials are complete and compliant with competition rules.

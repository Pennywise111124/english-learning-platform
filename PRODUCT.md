# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

Spring Boot 4.1.1 (Java 21) backend with PostgreSQL, Redis, WebSocket (STOMP). Frontend: HTML/CSS/JavaScript + Tailwind CSS via CDN. Established architecture—not a greenfield choice.

## Users

**Primary users:** Vietnamese English learners seeking to improve conversational fluency and grammar accuracy. Range from beginners to advanced learners. Use the platform in two contexts: structured study sessions at home (desktop/laptop) and quick practice on-the-go (mobile).

**Secondary users:** Administrators who manage learning content (topics, flashcards, quizzes, dictation lessons).

## Product Purpose

LinguistAI is an AI-integrated English learning platform that combines free-form conversational practice with structured learning materials. Learners chat with an AI tutor that corrects grammar in real-time, study organized topics with flashcards, test themselves with quizzes, and track progress across their learning journey. Success means learners achieve measurable improvement in English fluency and grammar accuracy through consistent, personalized practice.

## Positioning

**The integrated approach:** Most English learning platforms offer either AI conversation practice or structured curriculum content—rarely both in one cohesive experience. LinguistAI uniquely bridges free-form AI chat (instant feedback, natural conversation) with traditional topic-based learning (vocabulary, quizzes, progress tracking), letting learners switch fluidly between exploration and structure based on their immediate learning need.

## Operating Context

- **Structured study sessions:** Learners dedicate focused time at home on desktop/laptop, working through topics systematically, completing quizzes, reviewing flashcards with deliberate practice
- **Mobile quick practice:** During commute or breaks, learners open the app on mobile to chat with the AI for a few minutes, review a flashcard set, or check progress
- **Workflow transitions:** A learner might discover unfamiliar vocabulary during AI chat, then navigate to the related topic to study those words systematically through flashcards and quizzes

The platform must support both deep-focus workflows (quiz-taking, progress review) and interruptible micro-sessions (quick chat exchange, single flashcard review).

## Capabilities and Constraints

**Core capabilities (MVP—M1-M9 complete):**
- JWT-authenticated user accounts with USER/ADMIN roles
- Real-time AI chat with grammar correction and explanation, streamed over WebSocket
- Topic-based learning modules with flashcards (word, meaning, example, image, audio URL)
- Multiple-choice quizzes with automatic scoring and attempt history
- Personal progress tracking per topic (status, percentage, quiz performance)
- Image upload for flashcards, topics, and user avatars
- Admin content management interface
- Redis caching for frequently-accessed content

**Planned capabilities (v1.1—M10-M12):**
- Search, filter, and sorting across topic library
- Dictation exercises with accuracy scoring
- Spaced repetition system (SRS) for vocabulary review

**Technical constraints:**
- Backend uses UTC timestamps (Instant type) throughout
- Pagination defaults: page 0, size 20, max 100
- Image uploads limited to JPEG/PNG, max 5MB
- WebSocket streaming for AI responses (chunks arrive incrementally)
- Ownership validation: users can only access their own conversations, progress, and quiz attempts (404 returned for unauthorized access)
- Content deletion blocked when historical user data depends on it (409 conflict response)

**Terminology:**
- "Conversation" = a chat thread with the AI
- "Topic" = a learning module (e.g., "Travel", "Business English")
- "Flashcard" = vocabulary card within a topic
- "Quiz Attempt" = one submission of a quiz (history preserved, not overwritten)
- "User Progress" = current state summary for a topic (updated in place)

## Brand Commitments

**Name:** LinguistAI (confirmed, not a placeholder)

**Visual identity:** Material Design 3-inspired token system already established in Tailwind config:
- Primary: `#004ac6` (blue)
- Secondary: `#006a61` (teal)
- Tertiary: `#ad0033` (red)
- Typography: Plus Jakarta Sans (display/headlines), Inter (body/labels)
- Design tokens include surface variants, outline styles, and semantic color roles

**Voice:** Technical documentation uses Vietnamese, but product interface and AI interactions are English-first (supporting English learners). Tone is supportive and educational—focused on improvement, not judgment.

## Evidence on Hand

**Documentation:**
- `Requirements.md` (51.7KB): comprehensive technical requirements, entity design, API contracts, milestone history through M8
- `FE_Handoff_Brief.md` (15.6KB): frontend implementation guidelines, API shapes, mock data patterns
- Backend codebase: M1-M8 complete (Auth, Topics, Flashcards, Quiz, AI integration, Redis cache, WebSocket streaming, file upload, unit tests)

**Visual assets:**
- Established design token system in `frontend/js/tailwind-config.js`
- Custom component styles in `frontend/css/style.css`
- Working frontend pages: index, login/register, chat, topics, topic-detail, quiz, progress, profile, admin

**Absence to note:** No actual flashcard imagery library yet (placeholder URLs in mock data). No real user testimonials or learning outcome metrics. No third-party pedagogical validation.

## Product Principles

1. **Seamless mode switching:** Learners transition between conversational exploration (AI chat) and structured reinforcement (topics, quizzes) without friction. The interface makes both modes feel like parts of one learning journey, not separate products.

2. **Real-time feedback builds fluency:** Instant grammar correction during conversation practice accelerates learning more than delayed batch feedback. The streaming WebSocket experience makes corrections feel like a natural coaching dialogue.

3. **Progress must be honest and earned:** Quiz scores, progress percentages, and completion statuses reflect actual performance verified server-side. No client-submitted scores, no participation trophies. Trust in the progress tracker depends on it reflecting real achievement.

4. **Multi-context design:** Every feature must work in both focused-study mode (desktop, uninterrupted time) and quick-practice mode (mobile, interruptible). A quiz interface that only works in 20-minute blocks fails mobile commuters; a flashcard review that requires constant interaction fails focused study sessions.

5. **Administrative efficiency enables content quality:** Admins manage large content libraries (hundreds of topics, thousands of flashcards). Batch operations, clear validation feedback, and workflow efficiency in the admin interface directly affect how much quality content learners receive.

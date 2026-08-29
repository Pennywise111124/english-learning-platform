# Frontend Implementation Plan — LinguistAI English Learning Platform

## Overview
Build the complete frontend for an AI-powered English learning platform.
All files go inside `frontend/` only — do NOT touch any other directory.
Use Tailwind CSS (via CDN play script) with the shared design token config.
All pages use mock data from `js/mockApi.js`; no real API calls yet.

## Global Constraints
- **Framework:** Tailwind CSS via `<script src="https://cdn.tailwindcss.com?plugins=forms,container-queries">`
- **Fonts:** Plus Jakarta Sans + Inter + Material Symbols Outlined (Google Fonts CDN)
- **All output in `frontend/` only — never touch src/, pom.xml, or any backend file**
- **Mock data:** every page imports and calls ONLY functions from `js/mockApi.js`
- **JWT:** stored in `localStorage` key `"linguistai_token"` — `auth.js` handles read/write/check
- **Design tokens** (shared across ALL pages — copy verbatim into every tailwind.config):
  ```js
  colors: {
    "primary": "#004ac6", "primary-container": "#2563eb",
    "primary-fixed": "#dbe1ff", "primary-fixed-dim": "#b4c5ff",
    "on-primary": "#ffffff", "on-primary-fixed": "#00174b",
    "on-primary-fixed-variant": "#003ea8", "inverse-primary": "#b4c5ff",
    "secondary": "#006a61", "secondary-container": "#86f2e4",
    "secondary-fixed": "#89f5e7", "secondary-fixed-dim": "#6bd8cb",
    "on-secondary": "#ffffff", "on-secondary-container": "#006f66",
    "on-secondary-fixed": "#00201d", "on-secondary-fixed-variant": "#005049",
    "tertiary": "#ad0033", "tertiary-container": "#d22348",
    "tertiary-fixed": "#ffdadb", "tertiary-fixed-dim": "#ffb2b7",
    "on-tertiary": "#ffffff", "on-tertiary-container": "#ffecec",
    "on-tertiary-fixed": "#40000d", "on-tertiary-fixed-variant": "#92002a",
    "background": "#f8f9ff", "surface": "#f8f9ff",
    "surface-dim": "#cbdbf5", "surface-bright": "#f8f9ff",
    "surface-tint": "#0053db", "surface-variant": "#d3e4fe",
    "surface-container-lowest": "#ffffff", "surface-container-low": "#eff4ff",
    "surface-container": "#e5eeff", "surface-container-high": "#dce9ff",
    "surface-container-highest": "#d3e4fe",
    "on-background": "#0b1c30", "on-surface": "#0b1c30",
    "on-surface-variant": "#434655", "on-error": "#ffffff",
    "error": "#ba1a1a", "error-container": "#ffdad6", "on-error-container": "#93000a",
    "outline": "#737686", "outline-variant": "#c3c6d7",
    "inverse-surface": "#213145", "inverse-on-surface": "#eaf1ff",
    "surface-container-low": "#eff4ff"
  },
  borderRadius: { "DEFAULT":"0.25rem","lg":"0.5rem","xl":"0.75rem","full":"9999px" },
  spacing: {
    "unit":"8px","gutter":"24px","container-max":"1280px",
    "stack-sm":"12px","stack-md":"24px","stack-lg":"48px",
    "margin-mobile":"16px","margin-desktop":"40px"
  },
  fontFamily: {
    "headline-lg":["Plus Jakarta Sans"],"headline-md":["Plus Jakarta Sans"],
    "display-lg":["Plus Jakarta Sans"],"body-lg":["Inter"],
    "body-md":["Inter"],"label-md":["Inter"],"label-sm":["Inter"]
  },
  fontSize: {
    "display-lg":["48px",{"lineHeight":"1.2","letterSpacing":"-0.02em","fontWeight":"700"}],
    "headline-lg":["32px",{"lineHeight":"1.3","fontWeight":"600"}],
    "headline-md":["24px",{"lineHeight":"1.4","fontWeight":"600"}],
    "body-lg":["18px",{"lineHeight":"1.6","fontWeight":"400"}],
    "body-md":["16px",{"lineHeight":"1.5","fontWeight":"400"}],
    "label-md":["14px",{"lineHeight":"1","letterSpacing":"0.01em","fontWeight":"500"}],
    "label-sm":["12px",{"lineHeight":"1","fontWeight":"600"}]
  }
  ```
- **Material Symbols** class setup:
  ```css
  .material-symbols-outlined {
    font-family: 'Material Symbols Outlined';
    font-weight: normal; font-style: normal; font-size: 24px;
    line-height: 1; letter-spacing: normal; text-transform: none;
    display: inline-block; white-space: nowrap; word-wrap: normal;
    direction: ltr; -webkit-font-smoothing: antialiased;
  }
  ```
- **Navigation guard pattern** (for protected pages): all protected pages include at top of their inline `<script>`:
  ```js
  import { checkAuth } from './js/auth.js';
  checkAuth(); // redirects to login.html if no JWT
  ```
- **mockApi.js import pattern**: each page imports with:
  ```js
  import * as api from './js/mockApi.js';
  ```
  (use `type="module"` on script tags)

---

## Task 1: Core JS files (mockApi.js, auth.js, api.js, style.css)

Create `frontend/js/mockApi.js`, `frontend/js/auth.js`, `frontend/js/api.js`, `frontend/css/style.css`.

### mockApi.js
Export these functions (all return `Promise`, simulate 300-500ms delay with `setTimeout`):

```js
// Auth
export async function login(username, password) // returns login response DTO
export async function register(email, username, password) // returns {message: "registered"}

// Topics
export async function getTopics({ keyword='', level='', page=0, size=20 } = {}) // returns paged DTO
export async function getTopicDetail(id) // returns topic DTO

// Flashcards
export async function getFlashcards(topicId) // returns array

// Quizzes
export async function getQuizzesByTopic(topicId) // returns array [{id, title}]
export async function getQuizDetail(quizId) // returns quiz detail DTO (no correctAnswer)
export async function submitQuiz(quizId, answers) // returns result DTO
export async function getQuizAttempts(quizId, page=0, size=20) // returns paged attempts

// Progress
export async function getProgress() // returns array of progress items

// Conversations
export async function getConversations(page=0, size=20) // returns paged conversations
export async function getConversation(id) // returns conversation DTO
export async function getMessages(conversationId) // returns messages array
export async function createConversation() // returns {id, title, updatedAt}
export async function sendMessage(conversationId, content) // returns AI message DTO

// Profile
export async function getProfile() // returns profile DTO
export async function uploadAvatar(formData) // returns {avatarUrl}

// Admin
export async function adminGetTopics() // returns paged topics
export async function adminCreateTopic(data) // returns topic DTO
export async function adminUpdateTopic(id, data) // returns updated DTO
export async function adminDeleteTopic(id) // returns {message: "deleted"}
```

**Mock data** must use the exact DTO shapes from Brief section 8. Include at least:
- 8 topics with varied titles/levels (BEGINNER/INTERMEDIATE/ADVANCED), descriptions, plausible imageUrl (use `https://picsum.photos/seed/<word>/400/240`)
- 5 flashcards per topic (word, meaning in Vietnamese, example, imageUrl)
- 2 quizzes per topic, each with 5 questions and 4 options each
- 3 conversation history items, messages with USER/AI turns
- 1 user profile (username: "john_doe", email: "john@linguistai.com")
- Progress for 5 topics

### auth.js
```js
const TOKEN_KEY = 'linguistai_token';
const USER_KEY = 'linguistai_user';

export function saveAuth(loginResponse)   // saves accessToken + user object
export function getToken()                // returns token string or null
export function getUser()                 // returns user object or null
export function clearAuth()               // removes both keys
export function isLoggedIn()              // returns boolean
export function checkAuth()              // if !isLoggedIn(), redirect to login.html
export function checkAdmin()             // if !isLoggedIn() OR user.role !== 'ADMIN', redirect
```

### api.js
```js
const BASE_URL = 'http://localhost:8080';

// Generic fetch wrapper — adds Authorization header if token exists
// Throws on 401 (clears auth, redirects to login), throws Error with .message on other errors
export async function apiFetch(path, options = {})

// Re-export all mockApi functions (for future swap: replace this file's internals with real fetch calls)
export * from './mockApi.js';
```

### style.css
Minimal custom styles not achievable with Tailwind alone:
- `.chat-messages` scrollable container (height: calc(100vh - 200px))
- `.flashcard` 3D flip animation (preserve-3d, backface-hidden)
- `.quiz-option.selected` highlight style (border-primary, bg-primary/10)
- `.quiz-option.correct` / `.quiz-option.incorrect` result colors
- Scrollbar styling for chat (thin, primary-colored thumb)

---

## Task 2: login.html and register.html

Reference designs: `design-reference/login.html` and `design-reference/registration.html`

### login.html
- Centered card (max-w-md), white bg, rounded-xl, subtle shadow
- Logo: "LinguistAI" in primary blue, headline-lg font
- Subtitle: "Welcome back. Please login to your account."
- Fields: Username or Email, Password (with "Forgot password?" link right-aligned)
- Submit button: coral red `#F43F5E`, full-width, "Login" + login icon
- "Don't have an account? Sign up" link → `register.html`
- Footer: LinguistAI brand + nav links + copyright
- **JS behavior**: on submit call `api.login(username, password)` → `auth.saveAuth()` → redirect to `chat.html`; show error toast if login fails

### register.html
- Same centered card layout, "Create your account" heading
- Fields: Username, Email address, Password, Confirm password
- Submit button: teal `#0D9488` full-width "Sign Up"
- Confirm password validation (client-side match check)
- "Already have an account? Login" link → `login.html`  
- Same footer
- **JS behavior**: on submit call `api.register(email, username, password)` → redirect to `login.html` with `?registered=1`; login.html shows success message if param present

---

## Task 3: index.html (Landing Page)

Reference design: `design-reference/landing_page.html`

Read the full design file before implementing. Key sections:
- **NavBar**: LinguistAI logo left, nav links (Features, Topics, About), CTA buttons "Login" + "Get Started" (primary blue)
- **Hero**: large headline, subtitle, two CTA buttons, hero image/illustration area
- **Features section**: 3-column card grid (AI Chat, Flashcards, Quizzes)  
- **Topics preview**: grid of topic cards (pull from mockApi topics, display 6)
- **Footer**: brand + links + copyright

No auth check (public page). On "Get Started" → `register.html`. On "Login" → `login.html`.

---

## Task 4: chat.html

Reference design: `design-reference/AI_chat.html`

Read the full design file before implementing.
- **Auth guard**: call `checkAuth()` on load
- **Left sidebar**: conversation list (from `api.getConversations()`), "New Chat" button
- **Main area**: message thread (from `api.getMessages(conversationId)`)
- **Message input**: textarea + send button
- **Message bubbles**: USER right-aligned (primary blue bg), AI left-aligned (surface-container)
- AI messages may include `correction` and `explanation` fields — display them as collapsible sub-sections below the message
- On send: call `api.sendMessage(conversationId, content)`, append both user and AI messages to UI
- On "New Chat": call `api.createConversation()`, add to sidebar, load empty thread

---

## Task 5: topics.html

Reference design: `design-reference/topic.html`

Read the full design file before implementing.
- **Auth guard**: call `checkAuth()` on load
- **Search bar** + **Level filter** (ALL / BEGINNER / INTERMEDIATE / ADVANCED) + sort dropdown
- **Topic card grid** (3 cols desktop, 2 tablet, 1 mobile): image, title, level badge, description snippet
- **Pagination**: prev/next + page numbers, calls `api.getTopics({keyword, level, page})`
- On click topic card → `topic-detail.html?id=<topicId>`

---

## Task 6: topic-detail.html

Reference design: `design-reference/topic_detail.html`

Read the full design file before implementing.
- **Auth guard**: call `checkAuth()` on load
- Read `?id=` from URL, call `api.getTopicDetail(id)`
- **Header**: topic image, title, level badge, description
- **Two entry point cards**: 
  - "Flashcards" → calls `api.getFlashcards(id)` and shows inline flashcard section (or navigates within page)
  - "Quiz" → links to `quiz.html?topicId=<id>`
- **Flashcard section** (toggled): flip cards with word/meaning, prev/next navigation
- Reference design: `design-reference/flashcard.html` for flashcard UI

---

## Task 7: quiz.html

Reference designs: `design-reference/quiz.html` and `design-reference/quiz_result.html`

Read both design files before implementing.
- **Auth guard**: call `checkAuth()` on load
- Read `?topicId=` OR `?quizId=` from URL
- If `topicId`: call `api.getQuizzesByTopic(topicId)`, show quiz selection list
- If `quizId`: call `api.getQuizDetail(quizId)`, show quiz UI
- **Quiz UI**: question counter, question text, 4 option buttons (selectable, one at a time)
- **Navigation**: prev/next question, submit button on last question
- On submit: call `api.submitQuiz(quizId, answers)`, show result screen
- **Result screen** (from quiz_result.html): score circle/bar, correct/total, completedAt, "Try Again" + "Back to Topic" buttons

---

## Task 8: progress.html

Reference design: `design-reference/progress.html`

Read the full design file before implementing.
- **Auth guard**: call `checkAuth()` on load
- Call `api.getProgress()` 
- **Stats summary** at top: total topics, completed, in-progress counts
- **Progress list**: each item shows topic title, status badge (COMPLETED/IN_PROGRESS/NOT_STARTED), progress bar (progressPercent), updatedAt
- **Quiz history section**: call `api.getQuizAttempts(quizId)` for recent attempts — pick a sample quizId from mock data

---

## Task 9: profile.html

Reference design: `design-reference/profile.html`

Read the full design file before implementing.
- **Auth guard**: call `checkAuth()` on load
- Call `api.getProfile()` to load user info
- **Avatar**: circular image with "Change avatar" button (triggers file input → `api.uploadAvatar()`)
- **Profile info**: username, email, role badge
- **Change password form**: current password, new password, confirm new password (mock: show success toast, no real API)
- **Stats section**: mock numbers for quizzes taken, topics completed

---

## Task 10: admin.html

- **Auth guard**: call `checkAdmin()` on load (checks JWT for role=ADMIN)
- **Tab-based layout**: Topics | Flashcards | Quizzes tabs
- **Topics tab**: table listing topics (from `api.adminGetTopics()`), with Edit/Delete actions, "Add Topic" button → modal form
- **Modal form**: create/edit topic (title, description, level select, image URL)
- On save: call `api.adminCreateTopic()` or `api.adminUpdateTopic()`
- On delete: call `api.adminDeleteTopic()` with confirmation dialog
- Simple, functional admin UI — no specific design reference, use the project's design tokens

---

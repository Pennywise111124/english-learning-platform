# FRONTEND — TÀI LIỆU TRA CỨU NHANH KHI CODE THÊM TRANG
*(Trước đây là brief build UI bằng mock data cho M1-M8 — nay Frontend đã nối Backend thật + redesign hoàn chỉnh ở M9, tiếp tục nối thêm M10 (Search/Filter/Pagination) + M11 (Vocabulary/SRS). File này dùng khi build tiếp trang mới ở M12+, tra nhanh contract API + quy ước design system, không cần đọc lại toàn bộ Requirements.md mỗi lần. Cập nhật lần gần nhất: 29/09/2026, sau M11.)*

**Repo:** `github.com/Pennywise111124/english-learning-platform`, nhánh `main`
**Vai trò:** code trong thư mục `frontend/`. Không động vào Backend Java (thư mục gốc, ngoài `frontend/`).

---

## 1. Ràng buộc bắt buộc

- HTML/CSS/JavaScript thuần + Tailwind CSS qua CDN (`cdn.tailwindcss.com`, config ở `js/tailwind-config.js`). Không React/Vue/build tool.
- Gọi REST qua `js/api.js` (đã có sẵn `request()` engine — tự gắn JWT, tự refresh token khi 401, tự gắn header `ngrok-skip-browser-warning` cho môi trường dev qua tunnel). **Không tự viết `fetch()` riêng trong từng trang** — luôn thêm hàm mới vào `api.js` rồi import.
- WebSocket qua `js/chat.js` (SockJS + STOMP) — chỉ dùng cho trang Chat, không dùng lại pattern này cho trang khác trừ khi thật sự cần realtime.
- `localStorage` dùng cho: JWT (`linguistai_token`/`linguistai_refresh_token`), user cache (`linguistai_user`), theme (`linguistai_theme`), trạng thái sidebar thu gọn (`linguistai_sidebar_collapsed`).

## 2. Cấu trúc thư mục hiện tại

```
frontend/
├── index.html (landing page công khai — có footer riêng, không dùng footer.js)
├── chat.html (yêu cầu đăng nhập)
├── topics.html (PUBLIC — khách xem được; M10: nối thật Search/Level/Sort)
├── topic-detail.html (PUBLIC — khách xem được; M11: thêm nút Save flashcard)
├── quiz.html (yêu cầu đăng nhập — checkAuth() ngay đầu)
├── progress.html (yêu cầu đăng nhập)
├── profile.html (yêu cầu đăng nhập)
├── vocabulary.html (yêu cầu đăng nhập — mới hoàn toàn ở M11, tab "Due Today"/"All Words")
├── admin.html (yêu cầu đăng nhập + role ADMIN — checkAdmin())
├── test-websocket.html (công cụ test riêng, không tính vào bộ chính)
├── js/
│ ├── api.js (fetch engine + toàn bộ hàm gọi API — xem mục 4)
│ ├── auth.js (JWT storage, checkAuth()/checkAdmin(), getUser()/setUser())
│ ├── config.js (API_BASE_URL — SỬA DUY NHẤT Ở ĐÂY khi đổi domain Backend; imageSrc(path))
│ ├── navbar.js (sidebar trái thu gọn được + topbar — trang đã đăng nhập; topbar rút gọn — khách)
│ ├── footer.js (initFooter() — dùng chung mọi trang app, TRỪ index.html)
│ ├── authModal.js (modal Login/Sign Up dùng chung — thay login.html/register.html cũ, đã bỏ hẳn)
│ ├── toast.js (showToast(message, type))
│ ├── theme.js (dark/light mode)
│ ├── chat.js (STOMP/SockJS, chỉ chat.html dùng)
│ └── tailwind-config.js (design token — xem mục 5)
└── css/
└── style.css (CSS custom property màu + component class + skeleton/animation)
```

**Không còn `login.html`/`register.html`, không còn `mockApi.js`** — đã xoá hẳn ở M9.

## 3. Base URL & Auth

- `API_BASE_URL` khai báo **duy nhất** ở `js/config.js` — đổi 1 chỗ này khi backend đổi domain (VD: URL ngrok mới mỗi phiên dev).
- Login nhận **username hoặc email** cùng 1 field (Backend tự thử cả 2).
- `api.js` tự động: gắn `Authorization: Bearer <token>`, tự gọi `/api/auth/refresh` khi gặp `401` rồi thử lại request gốc 1 lần, nếu vẫn thất bại mới `clearAuth()` + redirect `index.html?auth=login`.
- Route bị chặn bởi `checkAuth()`/`checkAdmin()` khi chưa đăng nhập → redirect `index.html?auth=login` (tự mở modal Login qua query param). Logout chủ động → redirect `index.html` **trơn**, không kèm query param.

## 4. Danh sách API Endpoint thật (xác nhận qua Swagger `/swagger-ui.html`, tính đến M11)

### Auth

```
POST /api/auth/register {username, email, password} → 200, không có body
POST /api/auth/login {username, password} → {accessToken, refreshToken, user}
POST /api/auth/refresh {refreshToken} → {accessToken}
```

### Users/me — cần JWT

```
GET /api/users/me → User object (mục 6)
POST /api/users/me/avatar multipart, field "file" → User object
POST /api/users/me/password {currentPassword, newPassword} → 200, không có body
GET /api/users/me/progress → mảng UserProgress (mục 6)
GET /api/users/me/quiz-attempts?page=&size= → PageResponse<QuizAttempt> — TỔNG HỢP mọi Quiz
```

### Topics — **PUBLIC**, không cần JWT

```
GET /api/topics?page=&size=&keyword=&level=&sort= → PageResponse<Topic> — ĐẦY ĐỦ từ M10
keyword: tối đa 50 ký tự, tìm trong title+description, không phân biệt hoa/thường
level: BEGINNER | INTERMEDIATE | ADVANCED, không phân biệt hoa/thường
sort: newest (mặc định) | popular | title — SAI giá trị trả 400, không có sort theo level
GET /api/topics/{id} → Topic
GET /api/topics/{id}/flashcards → mảng Flashcard (không phân trang)
GET /api/topics/{id}/quizzes → mảng { id, title, questionCount } — PUBLIC, chỉ tên+số câu hỏi, KHÔNG phải nội dung
```

### Quizzes — cần JWT (khác GET /api/topics/{id}/quizzes ở trên)

```
GET /api/quizzes/{id} → { id, title, questions: [{id, question, options}] } — KHÔNG có correctAnswer
POST /api/quizzes/{id}/submit {answers: [{questionId, answer}]} → QuizResult (mục 6)
GET /api/quizzes/{id}/attempts?page=&size= → PageResponse<QuizAttempt> — CHỈ riêng Quiz này
```

### Vocabulary/SRS — cần JWT (mới hoàn toàn ở M11)

```

POST /api/vocabulary/{flashcardId}/save → VocabularyResponse (201). 409 nếu đã lưu rồi
PATCH /api/vocabulary/{id}/review {remembered: boolean} → VocabularyResponse
id ở đây là id của bản ghi UserVocabulary (lấy từ response của /save), KHÔNG PHẢI flashcardId — nhầm 2 cái này ra 404
GET /api/vocabulary/today?page=&size= → PageResponse<VocabularyResponse> — chỉ từ đến hạn (nextReviewAt <= now), sort difficulty DESC rồi nextReviewAt ASC
GET /api/vocabulary?page=&size= → PageResponse<VocabularyResponse> — TOÀN BỘ sổ từ đã lưu, không lọc theo hạn, mới lưu gần nhất trước
GET /api/vocabulary/topics/{topicId}/saved-ids → mảng flashcardId (Set<Long>, không phân trang) — dùng để hiện đúng trạng thái nút Save khi vào lại 1 Topic
DELETE /api/vocabulary/{id} → 204. Ownership qua id của UserVocabulary, sai chủ trả 404 (không phải của người khác cũng không phải không tồn tại — đồng nhất)
```

### Conversations — cần JWT

```
POST /api/conversations (không cần body) → Conversation
GET /api/conversations?page=&size= → PageResponse<Conversation>
GET /api/conversations/{id} → Conversation
GET /api/conversations/{id}/messages → mảng Message (không phân trang)
POST /api/conversations/{id}/messages {content} → Message (đây là USER message vừa lưu — KHÔNG phải câu trả lời AI, câu trả lời AI luôn qua WebSocket, xem mục 5)
```

### WS `/ws/chat` (STOMP qua SockJS)
Không đổi từ M6 — chi tiết đầy đủ trong `js/chat.js` (đã code sẵn, dùng lại `connectChat()`/`subscribeConversation()`/`sendChatMessage()`/`unsubscribeConversation()`, không viết lại từ đầu).

### Admin — cần JWT + role ADMIN

```
POST/PUT/DELETE /api/admin/topics, /api/admin/topics/{id} (KHÔNG có field imageUrl trong body — ảnh set riêng)
POST /api/admin/topics/{id}/image multipart "file" → Topic
POST /api/admin/topics/{id}/flashcards
PUT/DELETE /api/admin/flashcards/{id} (KHÔNG có field imageUrl trong body)
POST /api/admin/flashcards/{id}/image multipart "file" → Flashcard
POST/PUT/DELETE /api/admin/topics/{id}/quizzes, /api/admin/quizzes/{id} (title phải duy nhất trong cùng Topic, không phân biệt hoa/thường — 409 nếu trùng)
GET /api/admin/quizzes/{quizId}/questions → mảng { id, question, options, correctAnswer } — CÓ đáp án đúng, chỉ dùng điền sẵn form sửa
POST/PUT/DELETE /api/admin/quizzes/{id}/questions, /api/admin/questions/{id}
```

*(Dictation — chưa có, chờ M12. Vocabulary-SRS và Search nâng cao đã xong ở M10/M11, xem 2 khối phía trên.)*

## 5. Quy ước chung

- **Pagination:** `page` (mặc định 0), `size` (mặc định 20, tối đa 100 — vượt quá trả `400`, không tự cắt). Response `{content, totalPages, totalElements, page, size}`.
- **Lỗi:** `{message, status, timestamp}` — hiện `message` qua `showToast()` (hành động tức thời) hoặc inline text tại khối nội dung (khi cả 1 khu vực load thất bại — không dùng toast vì tự biến mất sau 3s, người dùng dễ bỏ lỡ).
- **Ownership sai** → luôn `404`, không `403` (không tiết lộ resource có tồn tại hay không).
- **Chat:** câu trả lời AI KHÔNG đến qua response của `POST /api/conversations/{id}/messages` — luôn qua kênh WebSocket. Đừng nhầm request REST đó với luồng nhận trả lời.

## 6. Shape dữ liệu thật (xác nhận qua Swagger, không phải bản nháp)

```json
// User object — trả về lồng trong AuthResponse.user (login), hoặc trực tiếp (GET /api/users/me, upload avatar).
// Riêng register KHÔNG trả object này (response rỗng, 200 không có body)
{ "id": 1, "username": "john", "email": "john@mail.com", "role": "USER", "avatarUrl": "/uploads/avatars/<uuid>.jpg" }
// avatarUrl là đường dẫn TƯƠNG ĐỐI — luôn qua imageSrc() (config.js) trước khi gán vào <img src>, không dùng thẳng

// Topic
{ "id": 1, "title": "Travel", "description": "...", "level": "BEGINNER", "imageUrl": "/uploads/topics/<uuid>.jpg" }
// KHÔNG có createdAt trong response dù entity đã có field này từ M10 — chỉ dùng nội bộ để sort=newest

// Flashcard
{ "id": 1, "word": "airport", "meaning": "sân bay", "example": "...", "imageUrl": "...", "audioUrl": null }

// Quiz — danh sách theo Topic (PUBLIC)
{ "id": 1, "title": "Travel Vocabulary Quiz", "questionCount": 5 }

// Quiz — chi tiết (cần JWT, KHÔNG correctAnswer)
{ "id": 1, "title": "Travel Vocabulary Quiz", "questions": [
  { "id": 1, "question": "What is 'airport'?", "options": ["sân bay", "khách sạn", "nhà ga", "bến xe"] }
]}

// Quiz — câu hỏi dành riêng Admin (CÓ correctAnswer)
{ "id": 1, "question": "...", "options": [...], "correctAnswer": "sân bay" }

// Quiz — kết quả submit (QuizResultResponse, KHÔNG có id)
{ "score": 80, "correctAnswers": 4, "totalQuestions": 5, "completedAt": "2026-09-23T10:00:00Z" }

// Quiz — 1 lần làm bài trong lịch sử (QuizAttemptResponse, CÓ id — dùng ở PageResponse<QuizAttempt> của 2 endpoint attempts)
{ "id": 1, "score": 80, "correctAnswers": 4, "totalQuestions": 5, "completedAt": "2026-09-23T10:00:00Z" }

// UserProgress (mảng, không phân trang)
{ "topicId": 1, "topicTitle": "Travel", "status": "IN_PROGRESS", "progressPercent": 60, "updatedAt": "..." }

// Conversation
{ "id": 1, "title": "Small talk practice", "createdAt": "...", "updatedAt": "..." }
// title có thể null cho conversation vừa tạo, chưa có tin nhắn nào — hiển thị fallback "New conversation"

// Message
{ "id": 1, "sender": "USER", "content": "...", "correction": null, "explanation": null, "createdAt": "..." }

// VocabularyResponse — mới ở M11, dùng chung cho response của save/review/today/list
{ "id": 1, "flashcardId": 17, "word": "Boarding Pass", "meaning": "...", "example": "...", "imageUrl": null, "audioUrl": null,
"status": "LEARNING", "reviewCount": 1, "difficulty": 0, "lastReviewedAt": "2026-09-29T08:06:10Z", "nextReviewAt": "2026-09-30T08:06:10Z" }
// status: NEW | LEARNING | KNOWN. id là id của UserVocabulary, không phải flashcardId — 2 field này khác nhau và đều có mặt trong cùng response

```

## 7. Design System — tóm tắt thực dụng (chi tiết đầy đủ + lý do quyết định: `Requirements.md` mục 10)

**Màu:** token giữ nguyên tên MD3 (`primary`/`secondary`/`tertiary`/`error` + `-container`/`-fixed`/`-fixed-dim`/`on-*`), định nghĩa qua CSS custom property ở đầu `style.css`, đừng bao giờ hardcode mã hex trong HTML/JS — luôn dùng class Tailwind theo token.

**Nút hành động chính — "nút 3D":**
```html
<button class="btn-3d px-6 py-3 bg-primary border-primary-shadow text-on-primary rounded-2xl hover:brightness-105 transition-all active:scale-95">
  Label
</button>
```
Nút phụ/Cancel dùng viền thường (`border-2 border-outline-variant`), không dùng `btn-3d`.

**Dark mode:** mọi trang mới **bắt buộc** có script chống nháy ngay đầu `<head>`, trước `tailwind-config.js`:
```html
<script>
  if (localStorage.getItem('linguistai_theme') === 'dark' ||
      (!localStorage.getItem('linguistai_theme') && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
    document.documentElement.classList.add('dark');
  }
</script>
```
Component dùng thư viện có palette riêng (VD: Tailwind Typography `prose`) phải thêm `dark:` biến thể tường minh.

**Khung sườn trang (app shell) — bắt buộc đúng 3 id cho mọi trang cần sidebar+topbar** (mọi trang trừ `index.html`):
```html
<body class="...">
    <div id="app-sidebar-container"></div>
    <div id="app-content" class="min-h-screen flex flex-col">
        <div id="app-topbar-container"></div>
        <main class="flex-grow pt-stack-md ...">
            <!-- nội dung -->
        </main>
        <div id="app-footer-container"></div>
    </div>

    <script type="module">
        import { checkAuth } from './js/auth.js';       // hoặc checkAdmin cho trang admin
        import { initNavBar } from './js/navbar.js';
        import { initFooter } from './js/footer.js';

        checkAuth();
        initNavBar('<activePage>');  // 'chat'|'topics'|'vocabulary'|'progress'|'admin'|'' 
        initFooter();
        // ... logic riêng của trang
    </script>
</body>
```
Trang PUBLIC (khách xem được, như `topics.html`/`topic-detail.html`) **bỏ** dòng `checkAuth();` — `navbar.js` tự nhận diện không có `user` và render topbar rút gọn cho khách, không cần code riêng.

Cần modal Login/Register trên trang PUBLIC → thêm `import { initAuthModal } from './js/authModal.js';` + gọi `initAuthModal();`. Nút mở modal: thêm attribute `data-open-login`/`data-open-register` vào bất kỳ `<button>` nào, không cần gắn `onclick` tay.

**Skeleton loading:** mọi khu vực chờ API phải render `div.skeleton` (shimmer có sẵn trong `style.css`) ngay khi bắt đầu gọi API, không dùng chữ "Loading..." hay để trắng.

## 8. Checklist khi thêm 1 trang mới

1. Copy đúng khung `<head>` (script chống nháy + font + Tailwind CDN + `tailwind-config.js` + `style.css`) từ 1 trang gần nhất đã có (VD `progress.html`).
2. Copy đúng cấu trúc `<body>` ở mục 7 (3 `<div>` container, không đổi tên id).
3. Import + gọi `checkAuth()`/`checkAdmin()` (trang cần đăng nhập) hoặc bỏ qua (trang public) + `initNavBar('<tên trang>')` + `initFooter()`.
4. Mọi lệnh gọi API mới → thêm hàm vào `api.js` trước, không tự `fetch()` trong trang.
5. Trạng thái loading → skeleton; trạng thái lỗi/rỗng của khối nội dung chính → inline text, không toast.
6. Nút hành động chính → `btn-3d`; test cả light lẫn dark mode trước khi coi là xong.

---
*Nguồn xác nhận cao nhất khi có sai lệch với file này: Swagger UI (`/swagger-ui.html`) cho API, `Requirements.md` mục 10 cho design system đầy đủ.*
# FRONTEND — TÀI LIỆU TRA CỨU NHANH KHI CODE THÊM TRANG
*(Trước đây là brief build UI bằng mock data cho M1-M8 — nay Frontend đã nối Backend thật + redesign hoàn chỉnh ở M9, tiếp tục nối thêm M10 (Search/Filter/Pagination), M11 (Vocabulary/SRS) và M12 (Dictation). File này dùng khi build tiếp trang mới ở M13+, tra nhanh contract API + quy ước design system, không cần đọc lại toàn bộ Requirements.md mỗi lần. Cập nhật lần gần nhất: 03/10/2026, sau M12.)*

**Repo:** `github.com/Pennywise111124/english-learning-platform`, nhánh `main`
**Vai trò:** code trong thư mục `frontend/`. Không động vào Backend Java (thư mục gốc, ngoài `frontend/`).

---

## 1. Ràng buộc bắt buộc

- HTML/CSS/JavaScript thuần + Tailwind CSS qua CDN (`cdn.tailwindcss.com`, config ở `js/tailwind-config.js`). Không React/Vue/build tool.
- Gọi REST qua `js/api.js` (đã có sẵn `request()` engine — tự gắn JWT, tự refresh token khi 401, tự gắn header `ngrok-skip-browser-warning` cho môi trường dev qua tunnel). **Không tự viết `fetch()` riêng trong từng trang** — luôn thêm hàm mới vào `api.js` rồi import.
- WebSocket qua `js/chat.js` (SockJS + STOMP) — chỉ dùng cho trang Chat, không dùng lại pattern này cho trang khác trừ khi thật sự cần realtime.
- `localStorage` dùng cho: JWT (`linguistai_token`/`linguistai_refresh_token`), user cache (`linguistai_user`), theme (`linguistai_theme`), trạng thái sidebar thu gọn (`linguistai_sidebar_collapsed`). `sessionStorage` (M12) chỉ dùng cho Topic Admin đang làm việc (`linguistai_admin_topic`).

## 2. Cấu trúc thư mục hiện tại

```
frontend/
├── index.html (landing page công khai — có footer riêng, không dùng footer.js)
├── chat.html (yêu cầu đăng nhập)
├── topics.html (PUBLIC — khách xem được; M10: nối thật Search/Level/Sort; M12: khách thấy sidebar, mục cần tài khoản mở modal đăng nhập)
├── topic-detail.html (PUBLIC — khách xem được; M11: thêm nút Save flashcard; M12: thêm thẻ Dictation, thẻ Quiz/Dictation/nút Save của khách mở modal đăng nhập kèm redirectTo)
├── quiz.html (yêu cầu đăng nhập — checkAuth() ngay đầu)
├── progress.html (yêu cầu đăng nhập)
├── profile.html (yêu cầu đăng nhập)
├── vocabulary.html (yêu cầu đăng nhập — mới hoàn toàn ở M11, tab "Due Today"/"All Words")
├── dictation.html (yêu cầu đăng nhập — mới hoàn toàn ở M12: danh mục bài có tìm kiếm/lọc/phân trang + màn luyện tập; bộ lọc lưu trên URL, mở trực tiếp được bằng ?topicId=)
├── admin.html (yêu cầu đăng nhập + role ADMIN — checkAdmin(); M12: tab Dictation, ô "Working topic" dùng chung cho Flashcards/Quizzes/Dictation, tab Topics có tìm kiếm/lọc/phân trang)
├── test-websocket.html (công cụ test riêng, không tính vào bộ chính)
├── js/
│ ├── api.js (fetch engine + toàn bộ hàm gọi API — xem mục 4)
│ ├── auth.js (JWT storage, checkAuth()/checkAdmin(), getUser()/setUser())
│ ├── config.js (API_BASE_URL — SỬA DUY NHẤT Ở ĐÂY khi đổi domain Backend; imageSrc(path), audioSrc(path) — M12)
│ ├── navbar.js (sidebar trái thu gọn được + topbar — trang đã đăng nhập; khách vẫn thấy sidebar, mục cần tài khoản có ổ khoá và mở modal đăng nhập kèm redirectTo — M12)
│ ├── footer.js (initFooter() — dùng chung mọi trang app, TRỪ index.html)
│ ├── authModal.js (modal Login/Sign Up dùng chung — thay login.html/register.html cũ, đã bỏ hẳn; M12: nhận detail { mode, redirectTo })
│ ├── toast.js (showToast(message, type))
│ ├── ui.js (M12 — el(), buildLevelBadge, buildPagerButton/buildPageInfo, createSelect, createFilterBar, createSearchBox)
│ ├── topicPicker.js (M12 — combobox chọn Topic có tìm kiếm, gọi GET /api/topics)
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

## 4. Danh sách API Endpoint thật (xác nhận qua Swagger `/swagger-ui.html`, tính đến M12)

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

### Dictation — cần JWT (mới hoàn toàn ở M12)

````
GET /api/topics/{topicId}/dictation → mảng { id, topicId, title, mediaUrl, level } — chỉ bài ĐÃ CÓ audio, KHÔNG có transcript. 404 nếu Topic không tồn tại
KHÁC các GET /api/topics/* ở trên: KHÁCH GỌI SẼ BỊ 401 và api.js tự clearAuth + đá về trang đăng nhập — trang PUBLIC (topic-detail.html) phải kiểm tra getUser() trước khi gọi
GET /api/dictation/lessons?keyword=&topicId=&level=&progress=&sort=&page=&size= → PageResponse<DictationCatalogItem> (danh mục, dùng cho dictation.html)
keyword ≤ 50 ký tự (tìm cả tên bài lẫn tên Topic); level BEGINNER|INTERMEDIATE|ADVANCED (không phân biệt hoa/thường);
progress NEW (chưa làm bao giờ) | PRACTICED (đã làm ≥ 1 lần); sort newest (mặc định) | title | recent (bài vừa luyện lên đầu, bài chưa làm xuống cuối)
Tham số sai kiểu/giá trị → 400. topicId không tồn tại → trang rỗng (KHÔNG 404). Chỉ bài đã có audio, KHÔNG có transcript. size > 100 → 400
POST /api/dictation/{lessonId}/submit {userInput} → DictationSubmitResponse (201)
userInput bắt buộc, ≤ 5000 ký tự, phải có ≥ 1 từ hợp lệ và ≤ 1000 từ, nếu không 400. 404 nếu bài không tồn tại hoặc chưa có audio.
Gửi NGUYÊN BẢN, KHÔNG trim/lowercase ở FE (server tự chuẩn hoá, lưu bản gốc)
GET /api/dictation/{lessonId}/results?page=&size= → PageResponse<DictationResultResponse>, mới nhất trước, chỉ của chính mình. 404 nếu bài không tồn tại
````

Hàm trong `api.js`: `getDictationLessons(topicId)`, `getDictationCatalog({keyword, topicId, level, progress, sort, page, size})`, `submitDictation(lessonId, userInput)`, `getDictationResults(lessonId, page, size)`.

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
GET /api/admin/topics/{id}/dictation → mảng { id, topicId, title, mediaUrl, transcript, level } — CÓ transcript, gồm cả bài chưa có audio (mediaUrl null)
POST /api/admin/topics/{id}/dictation {title, transcript, level} → lesson (200), mediaUrl luôn null — KHÔNG có mediaUrl trong body, audio upload riêng. transcript phải có ≥ 1 từ hợp lệ và ≤ 1000 từ, nếu không 400
PUT /api/admin/dictation/{id} {title, transcript, level} (không đụng mediaUrl); DELETE /api/admin/dictation/{id} → 204, 409 nếu đã có kết quả của User
POST /api/admin/dictation/{id}/audio multipart "file" (MP3/WAV/OGG/M4A, ≤ 10MB) → lesson; 400 nếu sai định dạng/quá lớn/thiếu part "file"/không phải multipart
(api.js: adminGetDictationLessons, adminCreateDictationLesson, adminUpdateDictationLesson, adminDeleteDictationLesson, adminUploadDictationAudio. adminGetTopics({keyword, level, sort, page, size}) giờ nhận tham số, mặc định size 10, dùng chung GET /api/topics)
```

*(Dictation đã xong ở M12 — xem khối Dictation phía trên và các dòng Admin ngay trên. Vocabulary-SRS và Search nâng cao đã xong ở M10/M11.)*

## 5. Quy ước chung

- **Pagination:** `page` (mặc định 0), `size` (mặc định 20, tối đa 100 — vượt quá trả `400`, không tự cắt). Response `{content, totalPages, totalElements, page, size}`.
- **Lỗi:** `{message, status, timestamp}` — hiện `message` qua `showToast()` (hành động tức thời) hoặc inline text tại khối nội dung (khi cả 1 khu vực load thất bại — không dùng toast vì tự biến mất sau 3s, người dùng dễ bỏ lỡ).
- **Ownership sai** → luôn `404`, không `403` (không tiết lộ resource có tồn tại hay không).
- **Chat:** câu trả lời AI KHÔNG đến qua response của `POST /api/conversations/{id}/messages` — luôn qua kênh WebSocket. Đừng nhầm request REST đó với luồng nhận trả lời.
- **Upload file (ảnh, audio):** multipart, part tên `file`. Lỗi trả `400` (sai định dạng, quá lớn, thiếu part, không phải multipart), `405`/`415` cho sai method/Content-Type. Backend nhận dạng định dạng bằng nội dung file, không tin Content-Type hay đuôi file. `mediaUrl`/`imageUrl` là đường dẫn TƯƠNG ĐỐI — luôn qua `audioSrc()`/`imageSrc()` (`config.js`) trước khi gán vào `<audio>`/`<img>`.
- **Trang PUBLIC gọi API cần JWT:** khách sẽ nhận `401`, `api.js` tự `clearAuth()` rồi redirect — kiểm tra `getUser()` trước khi gọi, khách bấm hành động cần tài khoản thì mở `open-auth-modal` kèm `redirectTo` thay vì gọi API.
- **`403` của `/api/admin/**`:** body là JSON mặc định của Spring (không có `message`), `api.js` sẽ hiện `HTTP 403` — chỉ Admin chạm tới, đã ghi backlog.
- **Dữ liệu từ server/Admin/người dùng → DOM:** dùng `textContent` (hoặc `el()` trong `ui.js`), KHÔNG nhét vào `innerHTML`/thuộc tính HTML (tên có dấu `"` hay `<` sẽ vỡ giao diện hoặc thành lỗ hổng XSS).

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

### Dictation (M12)

````json
// DictationLesson — phía User, GET /api/topics/{id}/dictation (KHÔNG có transcript)
{ "id": 1, "topicId": 23, "title": "At the cafe", "mediaUrl": "/uploads/dictation/<uuid>.mp3", "level": "BEGINNER" }

// DictationCatalogItem — GET /api/dictation/lessons. attempts/bestAccuracy/lastAttemptAt là của CHÍNH NGƯỜI ĐANG ĐĂNG NHẬP
{ "id": 1, "topicId": 23, "topicTitle": "M12 Dictation Test", "title": "At the cafe", "mediaUrl": "/uploads/dictation/<uuid>.mp3", "level": "BEGINNER",
  "attempts": 2, "bestAccuracy": 100.0, "lastAttemptAt": "2026-10-01T03:50:44.302226Z" }
// Chưa làm bài này: "attempts": 0, "bestAccuracy": null, "lastAttemptAt": null

// DictationSubmitResponse — POST /api/dictation/{id}/submit (201)
{ "resultId": 2, "lessonId": 1, "accuracy": 62.5, "transcript": "I would like a cup of coffee, please.", "createdAt": "2026-10-01T03:50:44.302225500Z",
  "words": [ { "word": "i", "status": "CORRECT", "expected": null },
             { "word": "will", "status": "WRONG", "expected": "would" },
             { "word": "a", "status": "MISSING", "expected": null },
             { "word": "really", "status": "EXTRA", "expected": null } ] }
// (words trong ví dụ trên chỉ minh hoạ 4 nhãn, không phải kết quả đầy đủ của câu mẫu), words đã chuẩn hoá (chữ thường, không dấu câu), theo thứ tự đọc. status: CORRECT | WRONG | MISSING | EXTRA
// WRONG: word = từ người dùng gõ, expected = từ đúng. MISSING: word = từ transcript mà người dùng bỏ sót. EXTRA: word = từ người dùng gõ thừa

// DictationResultResponse — lịch sử. userInput là nguyên văn người dùng gõ (có thể chứa < > "): chỉ đi qua textContent
{ "id": 2, "lessonId": 1, "userInput": "I will like cup of tea please", "accuracy": 62.5, "createdAt": "2026-10-01T03:50:44.302226Z" }

// Admin — AdminDictationLesson (có transcript)
{ "id": 1, "topicId": 23, "title": "At the cafe", "mediaUrl": null, "transcript": "I would like a cup of coffee, please.", "level": "BEGINNER" }
````

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
        initNavBar('<activePage>');  // 'chat'|'topics'|'vocabulary'|'dictation'|'progress'|'admin'|''
        initFooter();
        // ... logic riêng của trang
    </script>
</body>
```
Trang PUBLIC (khách xem được, như `topics.html`/`topic-detail.html`) **bỏ** dòng `checkAuth();` — `navbar.js` tự nhận diện không có `user`. Nếu trang có đủ 2 khung `app-sidebar-container` + `app-topbar-container` thì khách **vẫn thấy sidebar đầy đủ** (mục cần tài khoản có ổ khoá, bấm vào mở modal đăng nhập kèm `redirectTo`; "Topics" vẫn là link thường); trang không có đủ 2 khung (landing) thì chỉ có topbar rút gọn như cũ.

**Trang PUBLIC có app shell (khách thấy sidebar) BẮT BUỘC gọi `initAuthModal()`** — các mục có ổ khoá trong sidebar và nút "Log In" chỉ phát event `open-auth-modal`; trang chưa gọi `initAuthModal()` thì không có gì lắng nghe, khách bấm vào sẽ không có phản ứng và cũng không có lỗi nào trong console. Cách thêm: `import { initAuthModal } from './js/authModal.js';` + gọi `initAuthModal();`.
Nút mở modal: thêm attribute `data-open-login`/`data-open-register` vào bất kỳ `<button>` nào, không cần gắn `onclick` tay. Muốn đăng nhập xong đi tới đúng trang vừa bấm: `window.dispatchEvent(new CustomEvent('open-auth-modal', { detail: { mode: 'login', redirectTo: 'quiz.html?topicId=23' } }))` — `redirectTo` chỉ nhận tên trang `.html` tương đối kèm query, mỗi lần mở modal đều đặt lại, đóng modal thì bị xoá; mở không kèm `redirectTo` luôn về `chat.html`.

**Component dùng chung (M12) — dùng lại thay vì viết mới:**
- `js/ui.js`: `el(tag, class, text)` (tạo phần tử bằng `textContent`, KHÔNG `innerHTML`), `buildLevelBadge(level)`, `buildPagerButton(label, disabled, onClick)` + `buildPageInfo(text)` (phân trang đúng style `vocabulary.html`), `createSelect({container, label, options, value, icon, onChange})` (dropdown tự vẽ cho bộ lọc, thay `<select>` gốc; vẫn dùng `<select>` gốc trong form có `required`/`reset()`), `createFilterBar({...})` (lọc trong trình duyệt cho danh sách đã tải sẵn), `createSearchBox({container, label, placeholder, onSearch})` (tìm kiếm debounce cho bộ lọc chạy ở server), `LEVEL_FILTER_OPTIONS`.
- `js/topicPicker.js`: `createTopicPicker({container, onChange, placeholder, ariaLabel, inputId})` → `{getValue, setValue, setValueById, focus, destroy}`. Chọn Topic có tìm kiếm qua `GET /api/topics` (không giới hạn 100 Topic); `onChange` chỉ chạy khi NGƯỜI DÙNG chọn/xoá, `setValue()` không kích hoạt nó.
- Mẫu bộ lọc lưu trên URL (`dictation.html`, `admin.html`): đọc `URLSearchParams` lúc khởi động (bỏ qua giá trị sai), `history.replaceState` sau mỗi lần đổi, để F5 và gửi link vẫn quay về đúng trạng thái.

**Skeleton loading:** mọi khu vực chờ API phải render `div.skeleton` (shimmer có sẵn trong `style.css`) ngay khi bắt đầu gọi API, không dùng chữ "Loading..." hay để trắng.

## 8. Checklist khi thêm 1 trang mới

1. Copy đúng khung `<head>` (script chống nháy + font + Tailwind CDN + `tailwind-config.js` + `style.css`) từ 1 trang gần nhất đã có (VD `progress.html`).
2. Copy đúng cấu trúc `<body>` ở mục 7 (3 `<div>` container, không đổi tên id).
3. Import + gọi `checkAuth()`/`checkAdmin()` (trang cần đăng nhập), hoặc bỏ qua (trang public — khi đó **bắt buộc** thêm `initAuthModal()`, xem mục 7) + `initNavBar('<tên trang>')` + `initFooter()`.
4. Mọi lệnh gọi API mới → thêm hàm vào `api.js` trước, không tự `fetch()` trong trang.
5. Trạng thái loading → skeleton; trạng thái lỗi/rỗng của khối nội dung chính → inline text, không toast.
6. Nút hành động chính → `btn-3d`; test cả light lẫn dark mode trước khi coi là xong.
7. Dữ liệu do server/người dùng nhập → DOM bằng `textContent`/`el()` (`ui.js`), không `innerHTML`.
8. Danh sách tải bằng API mà có thể đổi nhanh (gõ tìm kiếm, đổi bộ lọc, đổi Topic): dùng biến đếm `requestId` để bỏ kết quả cũ trả về trễ (`const requestId = ++counter` ở đầu hàm, `if (requestId !== counter) return;` sau mỗi `await`, **và cả trong `catch`**). Cũng bỏ kết quả nếu người dùng đã rời màn hình đó (VD `currentLesson` đã bị `null`).
9. Chọn Topic → `createTopicPicker`; dropdown bộ lọc → `createSelect`; phân trang → `buildPagerButton`/`buildPageInfo`; trang public cần dữ liệu cá nhân → kiểm tra `getUser()` trước khi gọi API.
10. Khối `catch` luôn `console.error('[tênHàm]', error)` và hiện `error.message` (không chỉ một câu cố định), nếu không lỗi thật bị che mất.

---
*Nguồn xác nhận cao nhất khi có sai lệch với file này: Swagger UI (`/swagger-ui.html`) cho API, `Requirements.md` mục 10 cho design system đầy đủ.*
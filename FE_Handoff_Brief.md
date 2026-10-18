# FRONTEND — TÀI LIỆU TRA CỨU NHANH KHI CODE THÊM TRANG
*(Trước đây là brief build UI bằng mock data cho M1-M8 — nay Frontend đã nối Backend thật + redesign hoàn chỉnh ở M9, tiếp tục nối thêm M10 (Search/Filter/Pagination), M11 (Vocabulary/SRS), M12 (Dictation), M13 (hardening: lỗi 401/403 có body, 413), M14 (trạng thái nội dung Draft/Published/Archived), M15 (cờ `topicAvailable`, bỏ `innerHTML` ở progress/topics), M16 (XP, streak, múi giờ, đồng hồ), M17 (bảng xếp hạng tuần, achievement, popup thông báo) và M18 (rate limiting 429, xoá conversation, thanh cuộn chat). File này dùng khi build tiếp trang mới ở M19+, tra nhanh contract API + quy ước design system. Cập nhật lần gần nhất: 13/10/2026, sau M18.)*

**Repo:** `github.com/Pennywise111124/english-learning-platform`, nhánh `main`
**Vai trò:** code trong thư mục `frontend/`. Không động vào Backend Java (thư mục gốc, ngoài `frontend/`).

---

## 1. Ràng buộc bắt buộc

- HTML/CSS/JavaScript thuần + Tailwind CSS qua CDN (`cdn.tailwindcss.com`, config ở `js/tailwind-config.js`). Không React/Vue/build tool.
- Gọi REST qua `js/api.js` (đã có sẵn `request()` engine — tự gắn JWT, tự refresh token khi 401, tự gắn header `ngrok-skip-browser-warning` cho môi trường dev qua tunnel). **Không tự viết `fetch()` riêng trong từng trang** — luôn thêm hàm mới vào `api.js` rồi import.
- WebSocket qua `js/chat.js` (SockJS + STOMP) — chỉ dùng cho trang Chat, không dùng lại pattern này cho trang khác trừ khi thật sự cần realtime.
- `localStorage` dùng cho: JWT (`linguistai_token`/`linguistai_refresh_token`), user cache (`linguistai_user`, từ M16 có thêm `timezone`; `clock.js` đọc múi giờ từ cache này mỗi lần tick, nên `api.updateTimezone` phải cập nhật lại cache), theme (`linguistai_theme`), trạng thái sidebar thu gọn (`linguistai_sidebar_collapsed`). `sessionStorage` (M12) chỉ dùng cho Topic Admin đang làm việc (`linguistai_admin_topic`).
- **Hard refresh sau khi sửa FE** (Ctrl+Shift+R, hoặc DevTools → Network → "Disable cache"): trình duyệt cache JS/CSS/HTML, nên bản cũ có thể chạy tiếp và làm tưởng code mới bị lỗi (đã gặp ở lỗi "Not sent" của chat). Khi sửa trên máy Linux: chỉ `git add frontend/`; `js/config.js` chứa URL ngrok tạm, đừng commit nếu chỉ đổi URL.

## 2. Cấu trúc thư mục hiện tại

```
frontend/
├── index.html (landing page công khai — có footer riêng, không dùng footer.js)
├── chat.html (yêu cầu đăng nhập; M18: nút xoá conversation, tin bị chặn 429 hiện "Not sent", thanh cuộn to khi rê vào thanh)
├── topics.html (PUBLIC — khách xem được; M10: nối thật Search/Level/Sort; M12: khách thấy sidebar; M14: chỉ hiện Topic PUBLISHED, hai ô lọc dùng createSelect; M15: thẻ Topic dựng bằng el(), không còn innerHTML)
├── topic-detail.html (PUBLIC — khách xem được; M11: thêm nút Save flashcard; M12: thêm thẻ Dictation, thẻ Quiz/Dictation/nút Save của khách mở modal đăng nhập kèm redirectTo)
├── quiz.html (yêu cầu đăng nhập — checkAuth() ngay đầu)
├── progress.html (yêu cầu đăng nhập; M15: thẻ Topic đã archive/nháp bị làm mờ + nhãn "Unavailable", không bấm được, dựng bằng el(); M16: thẻ XP/level và thẻ streak hai trạng thái kèm khiên băng, xem mục 7; M17: lưới 5 achievement)
├── leaderboard.html (yêu cầu đăng nhập — mới ở M17: bảng xếp hạng tuần, thẻ hạng của mình, xem mục 7)
├── profile.html (yêu cầu đăng nhập; M16: mục chọn múi giờ; M17: công tắc hiện/ẩn tên trên bảng xếp hạng)
├── vocabulary.html (yêu cầu đăng nhập — mới hoàn toàn ở M11, tab "Due Today"/"All Words")
├── dictation.html (yêu cầu đăng nhập — mới hoàn toàn ở M12: danh mục bài có tìm kiếm/lọc/phân trang + màn luyện tập; bộ lọc lưu trên URL, mở trực tiếp được bằng ?topicId=)
├── admin.html (yêu cầu đăng nhập + role ADMIN — checkAdmin(); M12: tab Dictation, ô "Working topic" dùng chung, tab Topics có tìm kiếm/lọc/phân trang; M14: cột/ô đổi trạng thái Draft/Published/Archived ở Topics, Quizzes, Dictation, bộ lọc Status, mọi lệnh đọc dùng endpoint /api/admin/**)
├── test-websocket.html (công cụ test riêng, không tính vào bộ chính)
├── js/
│ ├── api.js (fetch engine + toàn bộ hàm gọi API — xem mục 4; M18: lỗi có thêm err.retryAfter)
│ ├── auth.js (JWT storage, checkAuth()/checkAdmin(), getUser()/setUser())
│ ├── config.js (API_BASE_URL — SỬA DUY NHẤT Ở ĐÂY khi đổi domain Backend; imageSrc(path), audioSrc(path) — M12)
│ ├── navbar.js (sidebar trái thu gọn được + topbar — trang đã đăng nhập; khách vẫn thấy sidebar, mục cần tài khoản có ổ khoá và mở modal đăng nhập kèm redirectTo — M12; M16: topbar người đã đăng nhập có <div id="topbarClock">; M17: mục Leaderboard, gọi initAchievementPopups() cho người đã đăng nhập)
│ ├── footer.js (initFooter() — dùng chung mọi trang app, TRỪ index.html)
│ ├── authModal.js (modal Login/Sign Up dùng chung — thay login.html/register.html cũ, đã bỏ hẳn; M12: nhận detail { mode, redirectTo }; M16: gửi múi giờ trình duyệt lúc đăng ký)
│ ├── toast.js (showToast(message, type))
│ ├── ui.js (M12 — el(), buildLevelBadge, buildPagerButton/buildPageInfo, createSelect, createFilterBar, createSearchBox; M14 — buildStatusBadge, buildStatusSelect, STATUS_FILTER_OPTIONS; M18 — formatRetryAfter)
│ ├── topicPicker.js (M12 — combobox chọn Topic có tìm kiếm; M14 — tham số source: 'public' gọi GET /api/topics, 'admin' gọi GET /api/admin/topics)
│ ├── theme.js (dark/light mode)
│ ├── clock.js (M16 — initClock(container): đồng hồ HH:MM theo múi giờ người dùng cho topbar; trả hàm dừng timer)
│ ├── achievements.js (M17 — initAchievementPopups(), refreshAchievements(): popup "Achievement unlocked" ở app shell)
│ ├── chat.js (STOMP/SockJS, chỉ chat.html dùng)
│ └── tailwind-config.js (design token — xem mục 7)
└── css/
└── style.css (CSS custom property màu + component class + skeleton/animation)
```

**Không còn `login.html`/`register.html`, không còn `mockApi.js`** — đã xoá hẳn ở M9.

## 3. Base URL & Auth

- `API_BASE_URL` khai báo **duy nhất** ở `js/config.js` — đổi 1 chỗ này khi backend đổi domain (VD: URL ngrok mới mỗi phiên dev).
- Login nhận **username hoặc email** cùng 1 field (Backend tự thử cả 2).
- `api.js` tự động: gắn `Authorization: Bearer <token>`, tự gọi `/api/auth/refresh` khi gặp `401` rồi thử lại request gốc 1 lần, nếu vẫn thất bại mới `clearAuth()` + redirect `index.html?auth=login`. (401 và 403 đều có body {message, status, timestamp} từ M13.)
- Route bị chặn bởi `checkAuth()`/`checkAdmin()` khi chưa đăng nhập → redirect `index.html?auth=login` (tự mở modal Login qua query param). Logout chủ động → redirect `index.html` **trơn**, không kèm query param.

## 4. Danh sách API Endpoint thật (xác nhận qua Swagger `/swagger-ui.html`, tính đến M18)

### Auth

```
POST /api/auth/register {username, email, password, timezone?} → 200, không có body. Giới hạn 5 lần/giờ/IP, vượt → 429 (M18)
timezone (M16, không bắt buộc): tên IANA như "Asia/Tokyo"; thiếu/rỗng → "Asia/Ho_Chi_Minh". Tên không nằm trong danh sách IANA hoặc không có dấu "/" (ví dụ "+05:00", "EST", "GMT+7") → 400 "Invalid timezone". Riêng "UTC" hợp lệ.
POST /api/auth/login {username, password} → {accessToken, refreshToken, user}. Giới hạn 10 lần/phút/IP, vượt → 429 (M18)
POST /api/auth/refresh {refreshToken} → {accessToken}
```

### Users/me — cần JWT

```
GET /api/users/me → User object (mục 6)
POST /api/users/me/avatar multipart, field "file" → User object
POST /api/users/me/password {currentPassword, newPassword} → 200, không có body
GET /api/users/me/progress → mảng UserProgress (mục 6), mỗi phần tử có topicAvailable (M15)
GET /api/users/me/quiz-attempts?page=&size= → PageResponse<QuizAttempt> — TỔNG HỢP mọi Quiz
GET /api/users/me/stats → UserStats (mục 6) — M16
PATCH /api/users/me/timezone {timezone} → User object (có timezone mới); 400 "Invalid timezone" nếu tên không hợp lệ — M16
PATCH /api/users/me/leaderboard-visibility {visible: boolean} → User object (có leaderboardVisible); thiếu hoặc sai kiểu → 400 — M17
GET /api/users/me/achievements → mảng 5 phần tử theo thứ tự cố định: { code, title, description, unlocked, awardedAt (null nếu chưa mở), dismissed } — M17
POST /api/users/me/achievements/{code}/dismiss → 204, gọi lại vẫn 204 (idempotent). Mã lạ hoặc chưa mở khoá → 404 — M17
```

### Leaderboard — cần JWT (M17)

```
GET /api/leaderboard/weekly → { weekStart, weekEnd, entries: [{ rank, username, xp, me }], me: { rank, xp, visible } }
Tuần thứ Hai → Chủ nhật, theo múi giờ của NGƯỜI XEM (weekStart/weekEnd dạng "2026-10-12"). Top 20, hoà XP thì user_id nhỏ hơn đứng trước.
entries chỉ gồm người có leaderboardVisible = true và có XP trong tuần. me.rank = null khi me.xp = 0.
Người đã ẩn: không có trong entries nhưng me.rank vẫn là số, me.visible = false.
```
Hàm trong api.js: getWeeklyLeaderboard(), getAchievements(), dismissAchievement(code), updateLeaderboardVisibility(visible) (gọi setUser nên cache khớp server).

### Topics — **PUBLIC**, không cần JWT

```
GET /api/topics?page=&size=&keyword=&level=&sort= → PageResponse<Topic> — ĐẦY ĐỦ từ M10
keyword: tối đa 50 ký tự, tìm trong title+description, không phân biệt hoa/thường
level: BEGINNER | INTERMEDIATE | ADVANCED, không phân biệt hoa/thường
sort: newest (mặc định) | popular | title — SAI giá trị trả 400, không có sort theo level
GET /api/topics/{id} → Topic
GET /api/topics/{id}/flashcards → mảng Flashcard (không phân trang)
GET /api/topics/{id}/quizzes → mảng { id, title, questionCount, status } — PUBLIC, chỉ tên+số câu hỏi, KHÔNG phải nội dung (status luôn PUBLISHED ở API public)
LƯU Ý M14: mọi GET trong khối này chỉ trả nội dung có trạng thái hiệu lực PUBLISHED (Topic PUBLISHED, và Quiz PUBLISHED nếu là danh sách Quiz).
Topic DRAFT/ARCHIVED -> 404 (cả /{id}, /{id}/flashcards, /{id}/quizzes). Trang phải xử lý 404 bằng error.status === 404 (xem topic-detail.html), không hiện "Failed to load".
```

### Quizzes — cần JWT (khác GET /api/topics/{id}/quizzes ở trên)

```
GET /api/quizzes/{id} → { id, title, questions: [{id, question, options}] } — KHÔNG có correctAnswer
POST /api/quizzes/{id}/submit {answers: [{questionId, answer}]} → QuizResult (mục 6)
GET /api/quizzes/{id}/attempts?page=&size= → PageResponse<QuizAttempt> — CHỈ riêng Quiz này
M14: GET /api/quizzes/{id} và POST /api/quizzes/{id}/submit trả 404 nếu Quiz hoặc Topic cha không PUBLISHED. Lịch sử (GET .../attempts, GET /api/users/me/quiz-attempts, GET /api/users/me/progress) vẫn xem được sau khi nội dung bị archive.
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
M14: POST /api/vocabulary/{flashcardId}/save trả 404 nếu Topic của flashcard không PUBLISHED. Các endpoint còn lại (today, danh sách, review, delete, saved-ids) KHÔNG lọc: sổ từ vựng vẫn ôn được sau khi Topic bị archive.
```

### Dictation — cần JWT (mới hoàn toàn ở M12)

````
GET /api/topics/{topicId}/dictation → mảng { id, topicId, title, mediaUrl, level } — chỉ bài ĐÃ CÓ audio, KHÔNG có transcript. 404 nếu Topic không tồn tại hoặc không PUBLISHED
KHÁC các GET /api/topics/* ở trên: KHÁCH GỌI SẼ BỊ 401 và api.js tự clearAuth + đá về trang đăng nhập — trang PUBLIC (topic-detail.html) phải kiểm tra getUser() trước khi gọi
GET /api/dictation/lessons?keyword=&topicId=&level=&progress=&sort=&page=&size= → PageResponse<DictationCatalogItem> (danh mục, dùng cho dictation.html)
keyword ≤ 50 ký tự (tìm cả tên bài lẫn tên Topic); level BEGINNER|INTERMEDIATE|ADVANCED (không phân biệt hoa/thường);
progress NEW (chưa làm bao giờ) | PRACTICED (đã làm ≥ 1 lần); sort newest (mặc định) | title | recent (bài vừa luyện lên đầu, bài chưa làm xuống cuối)
Tham số sai kiểu/giá trị → 400. topicId không tồn tại → trang rỗng (KHÔNG 404). Chỉ bài đã có audio, KHÔNG có transcript. size > 100 → 400
POST /api/dictation/{lessonId}/submit {userInput} → DictationSubmitResponse (201)
userInput bắt buộc, ≤ 5000 ký tự, phải có ≥ 1 từ hợp lệ và ≤ 1000 từ, nếu không 400. 404 nếu bài không tồn tại, chưa có audio hoặc không PUBLISHED (kể cả Topic cha).
Gửi NGUYÊN BẢN, KHÔNG trim/lowercase ở FE (server tự chuẩn hoá, lưu bản gốc)
GET /api/dictation/{lessonId}/results?page=&size= → PageResponse<DictationResultResponse>, mới nhất trước, chỉ của chính mình. 404 nếu bài không tồn tại
M14: GET /api/topics/{id}/dictation, GET /api/dictation/lessons, POST /api/dictation/{id}/submit chỉ áp dụng cho bài PUBLISHED có audio và Topic cha PUBLISHED, nếu không 404 (catalog thì bài bị ẩn). GET /api/dictation/{id}/results vẫn xem được sau khi archive.
````

Hàm trong `api.js`: `getDictationLessons(topicId)`, `getDictationCatalog({keyword, topicId, level, progress, sort, page, size})`, `submitDictation(lessonId, userInput)`, `getDictationResults(lessonId, page, size)`.

### Conversations — cần JWT

```
POST /api/conversations (không cần body) → Conversation
GET /api/conversations?page=&size= → PageResponse<Conversation>
GET /api/conversations/{id} → Conversation
GET /api/conversations/{id}/messages → mảng Message (không phân trang)
POST /api/conversations/{id}/messages {content} → Message (đây là USER message vừa lưu — KHÔNG phải câu trả lời AI, câu trả lời AI luôn qua WebSocket, xem mục 5). Giới hạn 20 tin/phút/người dùng (chung bộ đếm với WebSocket), vượt → 429 và tin KHÔNG được lưu — M18
DELETE /api/conversations/{id} → 204. Xoá luôn toàn bộ message. Không phải chủ hoặc không tồn tại → 404 — M18
```

### WS `/ws/chat` (STOMP qua SockJS)
Không đổi từ M6 — chi tiết đầy đủ trong `js/chat.js`. M18: khi vượt hạn mức, kênh /user/queue/errors nhận { message: "Too many requests. Please try again later", status: 429, timestamp } (KHÔNG có retryAfter); không có phản hồi AI và tin không được lưu. chat.html dùng status === 429 để đánh dấu tin "Not sent".

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
GET /api/admin/topics/{id}/dictation → mảng { id, topicId, title, mediaUrl, transcript, level, status } — CÓ transcript, gồm cả bài chưa có audio (mediaUrl null)
POST /api/admin/topics/{id}/dictation {title, transcript, level} → lesson (200), mediaUrl luôn null, status luôn DRAFT — KHÔNG có mediaUrl trong body, audio upload riêng. transcript phải có ≥ 1 từ hợp lệ và ≤ 1000 từ, nếu không 400
PUT /api/admin/dictation/{id} {title, transcript, level} (không đụng mediaUrl); DELETE /api/admin/dictation/{id} → 204, 409 nếu đã có kết quả của User
POST /api/admin/dictation/{id}/audio multipart "file" (MP3/WAV/OGG/M4A, ≤ 10MB) → lesson; 400 nếu sai định dạng/quá lớn/thiếu part "file"/không phải multipart
(api.js: adminGetDictationLessons, adminCreateDictationLesson, adminUpdateDictationLesson, adminDeleteDictationLesson, adminUploadDictationAudio.)
```

### Admin — đọc mọi trạng thái & đổi trạng thái (M14) — cần JWT + ADMIN

```
GET /api/admin/topics?page=&size=&keyword=&level=&sort=&status= → PageResponse<Topic> (mọi trạng thái, không cache)
status: DRAFT | PUBLISHED | ARCHIVED, không phân biệt hoa/thường; rỗng = tất cả; giá trị lạ -> 400. keyword/level/sort giống GET /api/topics
GET /api/admin/topics/{id} → Topic (mọi trạng thái)
GET /api/admin/topics/{id}/flashcards → mảng Flashcard (không cache, Topic ở mọi trạng thái)
GET /api/admin/topics/{id}/quizzes → mảng { id, title, questionCount, status } (mọi trạng thái)
PATCH /api/admin/topics/{id}/status {status} → Topic
PATCH /api/admin/quizzes/{id}/status {status} → { id, title, questionCount, status }   409 nếu publish Quiz chưa có câu hỏi
PATCH /api/admin/dictation/{id}/status {status} → AdminDictationLesson   409 nếu publish bài chưa có audio
Chuyển trạng thái TỰ DO giữa 3 giá trị. status thiếu -> 400 ("status:must not be null"), giá trị lạ -> 400 ("Malformed or missing request body").
Tạo mới (POST topic/quiz/dictation) luôn ra DRAFT: phải PATCH sang PUBLISHED thì người học mới thấy.
DELETE /api/admin/questions/{id} → 409 nếu là câu hỏi cuối của Quiz đang PUBLISHED.
DELETE topic/quiz/dictation/flashcard → 409 nếu đã có dữ liệu cá nhân; message gợi ý archive (Flashcard: archive Topic cha).
```

Hàm trong api.js: adminGetTopics({keyword, level, sort, status, page, size}), adminGetTopic(id), adminGetFlashcards(topicId), adminGetQuizzes(topicId), adminChangeTopicStatus(id, status), adminChangeQuizStatus(id, status), adminChangeDictationStatus(id, status).
Hàm theo dõi hoạt động (M17): submitQuiz, reviewVocabulary, submitDictation phát CustomEvent('learning-activity') trên window sau khi thành công; achievements.js nghe sự kiện này để làm mới popup. Hành động mới sinh XP/achievement cần phát cùng sự kiện này.
Hàm gamification/profile trong api.js (M16): register(email, username, password, timezone) (tham số cuối tuỳ chọn; authModal tự thử lại không kèm timezone nếu server báo "Invalid timezone"), getMyStats(), updateTimezone(timezone) (gọi setUser nên cache khớp server).
KHÔNG dùng getTopicDetail/getFlashcards/getQuizzesByTopic (API public) trong trang Admin: chúng ẩn bản nháp.
adminGetTopics không còn dùng chung GET /api/topics (đã đổi ở M14).

## 5. Quy ước chung

- **Pagination:** `page` (mặc định 0), `size` (mặc định 20, tối đa 100 — vượt quá trả `400`, không tự cắt). Response `{content, totalPages, totalElements, page, size}`.
- **Lỗi:** luôn `{message, status, timestamp}`, kể cả `401` (thiếu/sai JWT), `403` (sai role), `413` (upload vượt dung lượng), `405`, `415`, `429` (vượt hạn mức, M18), `500` (message cố định "Internal server error"). Hiện `message` qua `showToast()` (hành động tức thời) hoặc inline text tại khối nội dung (khi cả 1 khu vực load thất bại — không dùng toast vì tự biến mất sau 3s). `request()` trong `api.js` gắn `error.status` (số HTTP) và `error.retryAfter` (giây, từ header `Retry-After`, `null` nếu không có) vào Error.
- **Ownership sai** → luôn `404`, không `403` (không tiết lộ resource có tồn tại hay không).
- **Chat:** câu trả lời AI KHÔNG đến qua response của `POST /api/conversations/{id}/messages` — luôn qua kênh WebSocket. Đừng nhầm request REST đó với luồng nhận trả lời.
- **Upload file (ảnh, audio):** multipart, part tên `file`. Lỗi: `400` (sai định dạng, thiếu part, không phải multipart), `413` (file vượt dung lượng cho phép), `405`/`415` cho sai method/Content-Type. Backend nhận dạng định dạng bằng nội dung file, không tin Content-Type hay đuôi file. `mediaUrl`/`imageUrl` là đường dẫn TƯƠNG ĐỐI — luôn qua `audioSrc()`/`imageSrc()` (`config.js`) trước khi gán vào `<audio>`/`<img>`.
- **Trang PUBLIC gọi API cần JWT:** khách sẽ nhận `401`, `api.js` tự `clearAuth()` rồi redirect — kiểm tra `getUser()` trước khi gọi, khách bấm hành động cần tài khoản thì mở `open-auth-modal` kèm `redirectTo` thay vì gọi API.
- **Dữ liệu từ server/Admin/người dùng → DOM:** dùng `textContent` (hoặc `el()` trong `ui.js`), KHÔNG nhét vào `innerHTML`/thuộc tính HTML (tên có dấu `"` hay `<` sẽ vỡ giao diện hoặc thành lỗ hổng XSS).
- **Trạng thái nội dung (M14):** khách/người học chỉ thấy nội dung PUBLISHED; nội dung khác là `404`. Trang người học gặp `404` ở chi tiết Topic phải hiện "This topic is no longer available", không phải "Failed to load". Admin luôn dùng endpoint `/api/admin/**` để đọc.
- **Múi giờ và "ngày" (M16):** streak/XP chia ngày theo `user.timezone` do server tính; mọi timestamp server trả vẫn là UTC (`Instant`). FE chỉ hiển thị. Lưu ý `toLocaleDateString()` ở `progress.html` dùng múi giờ trình duyệt nên có thể lệch ngày với ngày tính streak nếu hai múi giờ khác nhau.
- **XP không có API ghi:** FE không bao giờ gửi XP hay streak. Server tự cộng sau khi nộp quiz/ôn từ "đã nhớ"/nộp dictation; sau các thao tác đó chỉ cần gọi lại `getMyStats()` nếu trang cần số mới. XP có thể trễ một chút so với response của thao tác (cộng sau commit) và hiếm khi mất nếu server lỗi.
- **Rate limiting (M18):** login 10/phút/IP, đăng ký 5/giờ/IP, chat 20 tin/phút/người dùng. Xử lý `error.status === 429`: hiện "Try again in ..." bằng `formatRetryAfter(error.retryAfter)` (ui.js). `Retry-After` chỉ đọc được ở trình duyệt vì BE expose header qua CORS; nếu `retryAfter` là `null` thì hiện "a moment". Kênh WebSocket không có `retryAfter`, chỉ có `status: 429`. Form đăng ký tự thử lại không kèm `timezone` khi gặp "Invalid timezone" sẽ tốn hai lượt hạn mức.
- **Achievement (M17):** FE không gửi gì để nhận achievement, server tự cấp ngay trước khi trả response của hành động. Popup chỉ cần gọi `getAchievements()` sau hành động (đã làm tự động qua sự kiện `learning-activity`). "Đã đóng" lưu ở server (`dismissed`), nên đóng ở thiết bị này thì thiết bị khác cũng không hiện lại.
- **Bảng xếp hạng (M17):** tuần theo múi giờ của người xem; `me.rank` có thể là số dù `me.visible = false`. `leaderboardVisible` nằm trong User object và cache `linguistai_user` (cập nhật qua `updateLeaderboardVisibility`).

## 6. Shape dữ liệu thật (xác nhận qua Swagger, không phải bản nháp)

```json
// User object — trả về lồng trong AuthResponse.user (login), hoặc trực tiếp (GET /api/users/me, upload avatar).
// Riêng register KHÔNG trả object này (response rỗng, 200 không có body)
{ "id": 1, "username": "john", "email": "john@mail.com", "role": "USER", "avatarUrl": "/uploads/avatars/<uuid>.jpg", "timezone": "Asia/Ho_Chi_Minh", "leaderboardVisible": true }
// avatarUrl là đường dẫn TƯƠNG ĐỐI — luôn qua imageSrc() (config.js) trước khi gán vào <img src>, không dùng thẳng
// timezone (M16): luôn có giá trị; tài khoản cũ nhận "Asia/Ho_Chi_Minh" từ migration. Cache localStorage của phiên đăng nhập trước M16 chưa có field này cho tới khi đăng nhập lại.
// leaderboardVisible (M17): luôn có giá trị, mặc định true. Cache localStorage cũ chưa có field này cho tới khi đăng nhập lại hoặc bật/tắt công tắc ở profile.html.

// Topic
{ "id": 1, "title": "Travel", "description": "...", "level": "BEGINNER", "imageUrl": "/uploads/topics/<uuid>.jpg", "status": "PUBLISHED" }
// status (M14): ở API public luôn là PUBLISHED. ở API admin có thể là DRAFT | PUBLISHED | ARCHIVED. description có thể là null.
// KHÔNG có createdAt trong response dù entity đã có field này từ M10 — chỉ dùng nội bộ để sort=newest

// Flashcard
{ "id": 1, "word": "airport", "meaning": "sân bay", "example": "...", "imageUrl": "...", "audioUrl": null }

// Quiz — danh sách theo Topic (PUBLIC)
{ "id": 1, "title": "Travel Vocabulary Quiz", "questionCount": 5, "status": "PUBLISHED" }

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
{ "topicId": 1, "topicTitle": "Travel", "status": "IN_PROGRESS", "progressPercent": 60, "updatedAt": "...", "topicAvailable": true }
// topicAvailable (M15): false khi Topic không còn PUBLISHED (nháp/archived). FE làm mờ thẻ, gắn "Unavailable", không cho bấm. Dùng `item.topicAvailable !== false` để an toàn với dữ liệu cũ.
// Dùng `!== false` (không dùng truthy) để thẻ vẫn bấm được nếu server cũ chưa trả field này.

// UserStats — GET /api/users/me/stats (M16)
{ "totalXp": 20, "level": 1, "xpToNextLevel": 80, "currentStreak": 1, "longestStreak": 1, "activeToday": true, "freezeAvailable": true }
// level = floor(sqrt(totalXp/100)) + 1; level L trải từ 100*(L-1)^2 đến 100*L^2 XP. xpToNextLevel = 100*level^2 - totalXp.
// currentStreak đã tính "hiển thị": nếu quá hạn thì trả 0 dù longestStreak vẫn giữ. activeToday = hôm nay (theo múi giờ người dùng) đã có hoạt động.
// freezeAvailable: tuần hiện tại (bắt đầu thứ Hai) chưa dùng đóng băng. Đóng băng tự áp dụng, che đúng một ngày bỏ lỡ.
// Người dùng chưa có hoạt động nào: toàn 0, level 1, xpToNextLevel 100, freezeAvailable true.

// LeaderboardResponse — GET /api/leaderboard/weekly (M17)
{ "weekStart": "2026-10-12", "weekEnd": "2026-10-18",
  "entries": [ { "rank": 1, "username": "alice", "xp": 120, "me": false },
               { "rank": 2, "username": "john", "xp": 90, "me": true } ],
  "me": { "rank": 2, "xp": 90, "visible": true } }
// Rỗng: "entries": [], "me": { "rank": null, "xp": 0, "visible": true }

// AchievementResponse — GET /api/users/me/achievements (M17), luôn đủ 5 phần tử
{ "code": "XP_100", "title": "XP Collector", "description": "Earn 100 XP in total",
  "unlocked": true, "awardedAt": "2026-10-12T07:32:01.382283Z", "dismissed": false }
// Chưa mở khoá: "unlocked": false, "awardedAt": null, "dismissed": false
// Mã hiện có: XP_100, STREAK_7, QUIZ_PASS_10, VOCAB_KNOWN_50, DICTATION_5. title/description do server trả, FE không hardcode.
// Popup hiện cho mọi mục unlocked && !dismissed.

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
// (words trong ví dụ trên chỉ minh hoạ 4 nhãn, không phải kết quả đầy đủ của câu mẫu), words đã chuẩn hoá (chữ thường; mọi dấu câu thành khoảng trắng nên "hello,world" là 2 từ, "1,000" thành "1" và "000", "U.S.A." thành "u" "s" "a"), theo thứ tự đọc. status: CORRECT | WRONG | MISSING | EXTRA
// WRONG: word = từ người dùng gõ, expected = từ đúng. MISSING: word = từ transcript mà người dùng bỏ sót. EXTRA: word = từ người dùng gõ thừa

// DictationResultResponse — lịch sử. userInput là nguyên văn người dùng gõ (có thể chứa < > "): chỉ đi qua textContent
{ "id": 2, "lessonId": 1, "userInput": "I will like cup of tea please", "accuracy": 62.5, "createdAt": "2026-10-01T03:50:44.302226Z" }

// Admin — AdminDictationLesson (có transcript)
{ "id": 1, "topicId": 23, "title": "At the cafe", "mediaUrl": null, "transcript": "I would like a cup of coffee, please.", "level": "BEGINNER", "status": "DRAFT" }
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
        initNavBar('<activePage>');  // 'chat'|'topics'|'vocabulary'|'dictation'|'leaderboard'|'progress'|'admin'|''
        initFooter();
        // ... logic riêng của trang
    </script>
</body>
```
Trang PUBLIC (khách xem được, như `topics.html`/`topic-detail.html`) **bỏ** dòng `checkAuth();` — `navbar.js` tự nhận diện không có `user`. Nếu trang có đủ 2 khung `app-sidebar-container` + `app-topbar-container` thì khách **vẫn thấy sidebar đầy đủ** (mục cần tài khoản có ổ khoá, bấm vào mở modal đăng nhập kèm `redirectTo`; "Topics" vẫn là link thường); trang không có đủ 2 khung (landing) thì chỉ có topbar rút gọn như cũ.

**Trang PUBLIC có app shell (khách thấy sidebar) BẮT BUỘC gọi `initAuthModal()`** — các mục có ổ khoá trong sidebar và nút "Log In" chỉ phát event `open-auth-modal`; trang chưa gọi `initAuthModal()` thì không có gì lắng nghe, khách bấm vào sẽ không có phản ứng và cũng không có lỗi nào trong console. Cách thêm: `import { initAuthModal } from './js/authModal.js';` + gọi `initAuthModal();`.
Nút mở modal: thêm attribute `data-open-login`/`data-open-register` vào bất kỳ `<button>` nào, không cần gắn `onclick` tay. Muốn đăng nhập xong đi tới đúng trang vừa bấm: `window.dispatchEvent(new CustomEvent('open-auth-modal', { detail: { mode: 'login', redirectTo: 'quiz.html?topicId=23' } }))` — `redirectTo` chỉ nhận tên trang `.html` tương đối kèm query, mỗi lần mở modal đều đặt lại, đóng modal thì bị xoá; mở không kèm `redirectTo` luôn về `chat.html`.

**Component dùng chung (M12) — dùng lại thay vì viết mới:**
- `js/ui.js`: `el(tag, class, text)` (tạo phần tử bằng `textContent`, KHÔNG `innerHTML`), `buildLevelBadge(level)`, `buildPagerButton(label, disabled, onClick)` + `buildPageInfo(text)`, `createSelect({container, label, options, value, icon, onChange})` (dropdown tự vẽ cho bộ lọc, thay `<select>` gốc, có ARIA + bàn phím; dùng ở `topics.html`, `admin.html`, `dictation.html`; vẫn dùng `<select>` gốc trong form có `required`/`reset()`), `createFilterBar({...})`, `createSearchBox({...})`, `LEVEL_FILTER_OPTIONS`. **M14:** `STATUS_FILTER_OPTIONS`; `buildStatusBadge(status)` (icon + chữ + viền, không chỉ dựa vào màu: Draft xám nét đứt, Published xanh lá đặc, Archived cam đặc); `buildStatusSelect({current, label, onChange, onError})` (dropdown đổi trạng thái trong dòng danh sách, menu `position: fixed` gắn vào `<body>` nên không bị bảng `overflow` cắt, tự lật lên khi sát mép dưới; `onChange` phải là hàm async, ném lỗi thì giữ giá trị cũ và gọi `onError(error)`).
- `js/topicPicker.js`: `createTopicPicker({container, onChange, placeholder, ariaLabel, inputId, source})` → `{getValue, setValue, setValueById, focus, destroy}`. `source: 'public'` (mặc định, `GET /api/topics`, chỉ Topic PUBLISHED, dùng ở `dictation.html`) hoặc `'admin'` (`GET /api/admin/topics`, thấy cả nháp/archived kèm huy hiệu trạng thái, dùng ở `admin.html`). `onChange` chỉ chạy khi NGƯỜI DÙNG chọn/xoá, `setValue()` không kích hoạt nó.
- Mẫu bộ lọc lưu trên URL (`dictation.html`, `admin.html`): đọc `URLSearchParams` lúc khởi động (bỏ qua giá trị sai), `history.replaceState` sau mỗi lần đổi, để F5 và gửi link vẫn quay về đúng trạng thái.

**Skeleton loading:** mọi khu vực chờ API phải render `div.skeleton` (shimmer có sẵn trong `style.css`) ngay khi bắt đầu gọi API, không dùng chữ "Loading..." hay để trắng.

**Gamification UI (M16) — `progress.html`:**
- Thẻ streak hai trạng thái: **đỏ** (`text-error`, nền `bg-error-container`) khi `activeToday && currentStreak > 0`; **xám** (`text-on-surface-variant`, nền `bg-surface-container-high`) trong mọi trường hợp còn lại. Đổi bằng cách gán lại `className`, không dùng màu hardcode.
- Khiên băng là SVG tự vẽ (hằng số tĩnh `SHIELD_OK`, `SHIELD_BROKEN`; gán bằng `innerHTML` được phép vì không chứa dữ liệu server): còn đóng băng → khiên nguyên `text-secondary`; đã dùng → khiên vỡ đôi `text-outline`. Ghi chú dưới: "Streak freeze ready…" / "…used this week. It renews on Monday".
- Thanh level: phần trăm = (totalXp − 100·(L−1)²) / (100·(2L−1)). Lỗi tải gamification hiện inline ở `#gamificationError`, không toast.
- Đồng hồ topbar (`clock.js`): cập nhật mỗi 15 giây, tooltip "Your day resets in Xh Ym". Chỉ người đã đăng nhập thấy.
- Chọn múi giờ ở `profile.html`: `<select>` từ `Intl.supportedValuesOf('timeZone')` (có danh sách dự phòng), nút "Use my browser's timezone" và "Save timezone".

**Leaderboard & achievement UI (M17):**
- `leaderboard.html`: thẻ `#meCard` (hạng `#N` hoặc `—`, XP tuần; nếu `visible = false` thêm ghi chú kèm liên kết tới `profile.html`), danh sách `#entryList` (hạng 1–3 màu `text-tertiary`; dòng của mình `bg-primary-fixed` + `border-primary` + hậu tố "(you)"), rỗng "Nobody has earned XP this week yet. Be the first!", lỗi inline `#listError`. Dựng bằng `el()`, có skeleton.
- Popup achievement (`achievements.js`): container `fixed bottom-4 right-4 z-[9998]`, `role="status"`, `aria-live="polite"`; thẻ viền `border-tertiary`, biểu tượng `emoji_events`, nút X có `aria-label`. Không tự tắt. Tạo bằng `initAchievementPopups()` (navbar gọi cho người đã đăng nhập); không cần gọi lại ở từng trang.
- `progress.html`: section Achievements (`#achievementGrid`, `#achievementError`): đã mở viền `border-tertiary` + `emoji_events`, chưa mở nền xám + mờ + biểu tượng `lock`.
- `profile.html`: checkbox `#leaderboardVisible`; đổi thất bại thì trả checkbox về trạng thái server.

**Chat & rate limit UI (M18):**
- Danh sách conversation: mỗi mục là `<div class="group">` chứa **hai nút riêng** (mở, xoá), không lồng nút trong nút; nút xoá luôn hiện trên mobile, chỉ hiện khi rê/focus trên desktop; xoá cần `confirm()`. Tiêu đề dựng bằng `textContent` (lấy từ tin người dùng gõ).
- Tin bị chặn 429: `lastSent = { bubble, content }`; khi lỗi `status === 429` bong bóng mờ (`opacity-50`), có nhãn "Not sent", nội dung trả lại ô nhập nếu ô trống, `handleDone` đặt `lastSent = null`.
- `authModal.js`: 429 ở login → toast + khoá nút Login tối đa 60 giây; 429 ở đăng ký → chỉ toast.
- Thanh cuộn `.chat-messages` và `#conversationsList`: mảnh 8px, chỉ to 14px khi rê vào chính thanh (`::-webkit-scrollbar:hover`, `background-clip: content-box`); Firefox cố định `thin`.

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
11. Trang Admin chỉ dùng hàm `admin*` trong `api.js` để ĐỌC dữ liệu (endpoint `/api/admin/**`); dùng API public sẽ không thấy bản nháp. Danh sách có trạng thái: `buildStatusSelect` cho ô đổi, `buildStatusBadge` cho nơi chỉ hiển thị.
12. Trang người học gọi API chi tiết một Topic/Quiz: xử lý `error.status === 404` thành thông báo "không còn tồn tại" thay vì lỗi chung.
13. Trang cần số liệu XP/streak: gọi `api.getMyStats()` (cần JWT), có `try/catch` hiện lỗi inline. Không tự tính streak ở FE. Đổi múi giờ qua `api.updateTimezone` để cache `user.timezone` được cập nhật, đồng hồ topbar sẽ đổi ở lần tick kế.
14. Id mới của M16 (gõ sai một id làm cả script module dừng im lặng): `progress.html` — `levelValue`, `xpValue`, `levelBar`, `levelHint`, `streakValue`, `streakIconBox`, `streakIcon`, `streakHint`, `freezeIcon`, `freezeHint`, `gamificationError`; `profile.html` — `timezoneSelect`, `detectTimezoneBtn`, `saveTimezoneBtn`; topbar — `topbarClock`.
15. Hành động mới có thể sinh XP/achievement (nộp bài, hoàn thành bài học...): sau khi API thành công, trong hàm của `api.js` gọi `window.dispatchEvent(new CustomEvent('learning-activity'))` để popup achievement tự làm mới.
16. Gọi API có thể bị giới hạn (đăng nhập, đăng ký, chat): xử lý `error.status === 429` bằng `formatRetryAfter(error.retryAfter)`; với chat qua WebSocket nhìn `status` trong payload của `/user/queue/errors`.
17. Id mới của M17/M18 (gõ sai một id làm cả script module dừng im lặng): `leaderboard.html` — `weekRange`, `meCard`, `entryList`, `listError`; `progress.html` — `achievementGrid`, `achievementError`; `profile.html` — `leaderboardVisible`; `chat.html` — `conversationsList` (mục con dựng bằng JS).

---
*Nguồn xác nhận cao nhất khi có sai lệch với file này: Swagger UI (`/swagger-ui.html`) cho API, `Requirements.md` mục 10 cho design system đầy đủ.*
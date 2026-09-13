# FRONTEND HANDOFF BRIEF
# Đưa file này cho AI Agent (Claude qua 9Router/Kiro) để code phần Frontend

**Repo:** [điền GitHub repo URL của bạn ở đây]
**Vai trò của bạn (Agent):** Code toàn bộ Frontend trong thư mục `frontend/`. **KHÔNG động vào bất kỳ thư mục nào khác của repo** (đặc biệt là code Backend Java/Spring Boot) — người dùng đang tự code Backend riêng trên máy khác, đụng vào sẽ gây conflict khi merge git.

---

## 1. Ràng buộc bắt buộc

- **Chỉ dùng HTML/CSS/JavaScript thuần + Tailwind CSS** (qua CDN `https://cdn.tailwindcss.com`, cấu hình tuỳ biến qua `js/tailwind-config.js`). **Không** dùng React/Vue/framework nào khác, không cần build tool/npm. **[Sửa 05/09/2026]** Đổi từ Bootstrap (bản brief gốc) sang Tailwind — quyết định thực tế đã áp dụng khi code Frontend, brief gốc ghi nhầm.
- Gọi REST API bằng `fetch` thuần.
- Kết nối WebSocket bằng `SockJS + STOMP` (qua CDN).
- **Được phép dùng `localStorage`** để lưu JWT token (đây là ứng dụng web thật chạy trên trình duyệt người dùng, khác với môi trường sandbox — `localStorage` hoạt động bình thường ở đây).
- Toàn bộ file đặt trong thư mục `frontend/` theo đúng cấu trúc bên dưới.

## 2. Cấu trúc thư mục bắt buộc

```
frontend/
 ├── index.html          (Landing page — trang chủ công khai, chưa đăng nhập)
 ├── login.html
 ├── register.html
 ├── chat.html            (trang chat AI — yêu cầu đã đăng nhập)
 ├── topics.html          (duyệt/tìm kiếm danh sách chủ đề — grid + search/filter/pagination)
 ├── topic-detail.html    (chi tiết 1 Topic — overview + 2 lối vào: Flashcards / Quiz)
 ├── quiz.html            (làm quiz)
 ├── progress.html        (tiến độ học + lịch sử quiz)
 ├── profile.html         (thông tin cá nhân, avatar, đổi mật khẩu)
 ├── admin.html           (trang quản trị nội dung — chỉ ADMIN)
 ├── js/
 │    ├── api.js               (helper fetch, tự động gắn header Authorization: Bearer <JWT>)
 │    ├── auth.js               (login/register, lưu/đọc JWT từ localStorage, redirect nếu chưa đăng nhập)
 │    ├── chat.js               (kết nối WebSocket/STOMP cho trang chat)
 │    ├── mockApi.js            (mock data — xem mục 7, sẽ thay bằng api.js thật khi nối Backend)
 │    ├── navbar.js             (navbar dùng chung giữa các trang)
 │    └── tailwind-config.js    (cấu hình tuỳ biến Tailwind — theme, màu sắc riêng của project)
 └── css/
      └── style.css       (style tùy chỉnh ngoài Tailwind, nếu cần)
```

**Quy tắc điều hướng:** `index.html`, `login.html`, `register.html` là trang công khai (không cần JWT). Tất cả trang còn lại (`chat.html`, `topics.html`, `quiz.html`, `progress.html`, `profile.html`) phải kiểm tra JWT trong `localStorage` khi load — nếu không có, redirect về `login.html`. `admin.html` kiểm tra thêm role `ADMIN` từ JWT payload.

## 3. Base URL & Auth convention

- Backend chạy tại: `http://localhost:8080` (đổi lại nếu người dùng báo port khác)
- Mọi API (trừ `/api/auth/**`) yêu cầu header: `Authorization: Bearer <JWT access token>`
- Sau khi login (`POST /api/auth/login`) thành công, lưu token vào `localStorage`, mọi request sau đó tự động đính kèm header này qua `api.js`
- Nếu API trả `401 Unauthorized` (thiếu/sai JWT) → xóa token, redirect về trang login
- Nếu API trả `404` cho resource không thuộc quyền sở hữu → hiển thị thông báo lỗi phù hợp (Backend luôn trả 404 cho case này, không dùng 403 — kể cả khi Conversation/Topic/Quiz tồn tại nhưng không thuộc user)

## 4. Danh sách API Endpoint (đúng theo Requirements đã chốt)

### Auth
```
POST /api/auth/register   {email, username, password}
POST /api/auth/login      {username, password} → {accessToken, ...}
```

### Chat (trang chat.html) — REST (M4) + WebSocket Streaming (M6), cả 2 đã xong

POST /api/conversations
GET /api/conversations?page=&size=
GET /api/conversations/{id} → có thêm field updatedAt (xem mục 8)
GET /api/conversations/{id}/messages
POST /api/conversations/{id}/messages {content: string} → trả về Message của AI (sender, content, correction, explanation, createdAt) — vẫn dùng được, nhưng khuyến nghị dùng WebSocket bên dưới để có hiệu ứng "đang gõ" thay vì chờ 1 cục


**WS `/ws/chat` (STOMP qua SockJS) — chi tiết bắt buộc để implement `chat.js`:**

- **Kết nối:** `new SockJS('http://localhost:8080/ws/chat')`, JWT gửi qua STOMP CONNECT header (KHÔNG phải HTTP header thường):

```js
  client.connectHeaders = { Authorization: 'Bearer ' + token };
```

  Nếu JWT thiếu/sai, server từ chối kết nối kèm 1 STOMP ERROR frame có `message` rõ ràng trước khi đóng — bắt qua `onStompError` để hiển thị lỗi cho User.

- **Subscribe (2 kênh riêng biệt cho 1 conversation):**
  - `/topic/conversations/{id}` — nhận **message hoàn chỉnh** của AI (đúng lúc AI trả lời xong), payload là `MessageResponse` đầy đủ (xem mục 8).
  - `/topic/conversations/{id}/typing` — nhận **từng chunk text thô** (không phải JSON, là plain string) trong lúc AI đang trả lời, dùng để hiển thị hiệu ứng gõ chữ. Nối các chunk lại theo thứ tự nhận được để hiện dần lên UI, KHÔNG đợi hết mới hiện.
  - Nếu subscribe vào conversation không thuộc quyền sở hữu → nhận STOMP ERROR frame kèm message, sau đó bị ngắt kết nối (hành vi có chủ đích, không phải bug).

- **Gửi tin nhắn:** publish tới `/app/chat.sendMessage/{id}`, body `{content: string}` (giống hệt body REST). Không có response trực tiếp qua publish — kết quả luôn tới qua 2 kênh subscribe ở trên.

- **Kênh lỗi riêng:** subscribe thêm `/user/queue/errors` để nhận lỗi phát sinh **sau khi** đã gửi thành công (VD: AI provider lỗi giữa lúc đang stream) — payload dạng `{message, status, timestamp}` (giống format lỗi REST ở mục 5).

- **Gợi ý luồng UI:** gửi tin nhắn → xoá/reset ô "đang gõ" → nối dần text từ kênh `/typing` → khi kênh chính (không phải `/typing`) nhận được message → thay hẳn ô "đang gõ" bằng bubble chat hoàn chỉnh (đã có `correction`/`explanation` nếu có).

### Topic — Duyệt/Tìm kiếm (trang topics.html)
```
GET  /api/topics?keyword=&level=&page=&size=&sort=
```

### Topic — Chi tiết (trang topic-detail.html)
```
GET  /api/topics/{id}
GET  /api/topics/{id}/flashcards
GET  /api/topics/{id}/quizzes
```

### Quiz (trang quiz.html)
```
GET  /api/topics/{id}/quizzes
GET  /api/quizzes/{id}                    (KHÔNG chứa đáp án đúng — chỉ hiển thị câu hỏi + lựa chọn)
POST /api/quizzes/{id}/submit             {answers: [{questionId, answer}]}
GET  /api/quizzes/{id}/attempts?page=&size=
```

### Progress (trang progress.html)
```
GET /api/users/me/progress
```

### Profile (trang profile.html)
```
POST /api/users/me/avatar (multipart/form-data, field name: "file") → trả về full User object (xem shape "Auth — user object" ở mục 8), gồm avatarUrl mới sau khi upload thành công
```
*(Lưu ý: hiện tại chưa có endpoint `GET /api/users/me` riêng để lấy lại thông tin profile — nếu `profile.html` cần hiển thị avatar/thông tin user mà không qua hành động upload, tạm thời lấy từ `user` object đã lưu trong `localStorage` lúc login, cập nhật lại `localStorage` sau mỗi lần avatar upload thành công bằng chính response trả về từ API này)*

### Admin (trang admin.html — chỉ hiện nếu role = ADMIN)
```
POST/PUT/DELETE /api/admin/topics, /api/admin/topics/{id}
POST /api/admin/topics/{id}/image        (multipart/form-data, field name: "file") → trả về full Topic object (đã có imageUrl mới)
POST /api/admin/topics/{id}/flashcards
PUT/DELETE /api/admin/flashcards/{id}
POST /api/admin/flashcards/{id}/image    (multipart/form-data, field name: "file") → trả về full Flashcard object (đã có imageUrl mới)
POST/PUT/DELETE /api/admin/topics/{id}/quizzes, /api/admin/quizzes/{id}
POST/PUT/DELETE /api/admin/quizzes/{id}/questions, /api/admin/questions/{id}
```

*(Các endpoint v1.1 — Dictation, Vocabulary/SRS, Search nâng cao — sẽ bổ sung sau khi Backend làm tới M10-M12, chưa cần code ở bản đầu)*

## 5. Quy ước chung

- **Pagination:** query param `page` (mặc định 0), `size` (mặc định 20, tối đa 100). Response dạng `{content: [...], totalPages, totalElements, page, size}` — đây là DTO `PageResponse` tự định nghĩa ở Backend (không phải object `Page` thô của Spring Data trả thẳng ra), nhưng có cấu trúc tương thích với format quen thuộc của Spring Data Page nên FE xử lý y hệt như dự kiến ban đầu, không cần thay đổi gì
- **Lỗi:** Backend trả JSON dạng `{message: "...", status: ...}` khi lỗi — hiển thị `message` cho người dùng
- **Dùng mock data có chủ đích** theo đúng chỉ dẫn ở mục 7 bên dưới — đây không phải "tạm bợ" mà là cách làm việc chính thức đã chốt cho giai đoạn này (xây UI trước, nối API thật sau)

## 6. Thứ tự ưu tiên build các trang

Dù build UI trước với mock data, vẫn nên theo thứ tự các trang theo mức độ quan trọng: Login/Register → Landing → Chat → Topics → Topic Detail → Quiz → Progress → Profile → Admin. Điều này giúp có sản phẩm demo được sớm nhất (đăng nhập + chat) trước khi hoàn thiện các trang còn lại.

## 7. CHẾ ĐỘ MOCK DATA — build toàn bộ UI trước, nối API thật sau

**Yêu cầu hiện tại:** Backend chưa code xong, nhưng cần build **toàn bộ UI với dữ liệu giả (mock)** trước, để sau này chỉ cần đổi 1 lớp duy nhất sang gọi API thật — không phải sửa lại UI.

**Cách bắt buộc thực hiện:**
- Tạo 1 file riêng `js/mockApi.js` chứa các hàm mock, đặt tên **giống hệt** hàm thật sẽ có trong `api.js` sau này (VD: `getTopics(page, size)`, `getTopicDetail(id)`, `login(username, password)`...), mỗi hàm trả về `Promise` chứa dữ liệu giả **đúng cấu trúc JSON ở mục 8 bên dưới** (giả lập độ trễ mạng bằng `setTimeout` ~300-500ms cho giống thật)
- Toàn bộ code UI (HTML/JS các trang) **chỉ được gọi qua các hàm này**, không tự bịa cấu trúc dữ liệu riêng trong từng trang
- Sau này khi Backend xong, chỉ cần thay nội dung `api.js` (dùng `fetch` thật) theo đúng chữ ký hàm y hệt `mockApi.js` — UI không cần sửa gì

## 8. DTO SHAPE (bản nháp) — mock data phải khớp đúng cấu trúc này

*(Cấu trúc có thể tinh chỉnh nhẹ khi Backend implement thật, nhưng đây là cơ sở đáng tin cậy nhất hiện tại — dựa đúng theo Entity đã thiết kế trong Requirements)*

**Auth — login response:**
```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "user": { "id": 1, "username": "john", "email": "john@mail.com", "role": "USER", "avatarUrl": null }
}
```
*(`avatarUrl` = `null` nếu user chưa từng upload avatar, hoặc URL thật nếu đã upload. Object `user` này — gọi là "User object" — được dùng lại y hệt ở nhiều nơi: response login, register, và response của `POST /api/users/me/avatar`, xem mục 4)*

**Topic — danh sách (paged, chuẩn Spring Data Page):**
```json
{
  "content": [
    { "id": 1, "title": "Travel", "description": "...", "level": "BEGINNER", "imageUrl": "..." }
  ],
  "totalPages": 5, "totalElements": 42, "page": 0, "size": 20
}
```

**Topic — chi tiết:**
```json
{ "id": 1, "title": "Travel", "description": "...", "level": "BEGINNER", "imageUrl": "..." }
```

**Flashcards theo Topic (không phân trang):**
```json
[
  { "id": 1, "word": "airport", "meaning": "sân bay", "example": "...", "imageUrl": "...", "audioUrl": "..." }
]
```

**Quiz — danh sách theo Topic:**
```json
[ { "id": 1, "title": "Travel Vocabulary Quiz" } ]
```

**Quiz — chi tiết (KHÔNG có correctAnswer):**
```json
{
  "id": 1, "title": "Travel Vocabulary Quiz",
  "questions": [
    { "id": 1, "question": "What is 'airport' in Vietnamese?", "options": ["sân bay", "khách sạn", "nhà ga", "bến xe"] }
  ]
}
```

**Quiz — kết quả sau submit:**
```json
{ "score": 80, "correctAnswers": 4, "totalQuestions": 5, "completedAt": "2026-08-29T10:00:00Z" }
```

**Quiz — lịch sử attempts (paged):**
```json
{
  "content": [
    { "id": 1, "score": 80, "correctAnswers": 4, "totalQuestions": 5, "completedAt": "..." }
  ],
  "totalPages": 1, "totalElements": 3, "page": 0, "size": 20
}
```

**Progress — theo user (mảng, mỗi phần tử 1 Topic):**
```json
[
  { "topicId": 1, "topicTitle": "Travel", "status": "IN_PROGRESS", "progressPercent": 60, "updatedAt": "..." }
]
```

**Conversation — danh sách (paged):**
```json
{
  "content": [ { "id": 1, "title": "Small talk practice", "updatedAt": "..." } ],
  "totalPages": 1, "totalElements": 2, "page": 0, "size": 20
}
```

**Conversation — chi tiết:**
```json
{ "id": 1, "title": "Small talk practice", "createdAt": "...", "updatedAt": "..." }
```

**Conversation — messages (không phân trang):**
```json
[
  { "id": 1, "sender": "USER", "content": "How are you?", "correction": null, "explanation": null, "createdAt": "..." },
  { "id": 2, "sender": "AI", "content": "I'm doing great, thanks for asking!", "correction": null, "explanation": null, "createdAt": "..." }
]
```

**Gửi tin nhắn — request:**
```json
{ "content": "How are you?" }
```

**Gửi tin nhắn — response (Message của AI, dùng chung cho cả REST và kênh WebSocket chính):**
```json
{ "id": 3, "sender": "AI", "content": "I'm doing great, thanks!", "correction": "Not applicable.", "explanation": "No correction needed as the sentence is grammatically correct and natural.", "createdAt": "..." }
```

**WebSocket — kênh `/typing` (chunk đang gõ):** payload là **plain text string thô**, KHÔNG phải JSON — VD server gửi lần lượt `"Hi there"`, `"! How can"`, `" I help..."`. Nối trực tiếp các chuỗi này lại theo đúng thứ tự nhận được.

**WebSocket — kênh `/user/queue/errors` (lỗi giữa lúc stream):**
```json
{ "message": "Không thể stream phản hồi từ AI provider", "status": 500, "timestamp": "..." }
```

**Lỗi AI provider tạm thời không khả dụng (REST):** status 503, body {message, status, timestamp}

**Profile / User object (dùng chung cho login, register, và response của `POST /api/users/me/avatar`):**
```json
{ "id": 1, "username": "john", "email": "john@mail.com", "avatarUrl": "...", "role": "USER" }
```
*(Đây chính xác là response thật của `POST /api/users/me/avatar` sau khi upload thành công — `avatarUrl` là URL ảnh vừa upload, VD `/uploads/avatars/<uuid>.jpg`. Ghép với `http://localhost:8080` làm base URL đầy đủ khi hiển thị `<img src>`)*

---
*File này được tạo từ bản Requirements chính thức của project "Nền tảng học tiếng Anh tích hợp AI". Nếu có thay đổi API/entity ở file Requirements, cần đồng bộ lại brief này trước khi giao tiếp tục cho Agent.*

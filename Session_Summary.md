# TÓM TẮT TOÀN BỘ QUÁ TRÌNH — Project "Nền tảng học tiếng Anh tích hợp AI"
*(Dùng để tiếp tục ở cuộc trò chuyện mới)*

**Ngày tóm tắt:** 13/09/2026 (cập nhật sau M8)

---

## 1. Bối cảnh xuất phát

Đã hoàn thành trọn vẹn **24 bài lý thuyết Backend Java/Spring Boot** (Phase 1–6). Chuyển sang giai đoạn thực hành với 2 project:
- **Project 1 — Nền tảng học tiếng Anh tích hợp AI** (đang làm, chi tiết bên dưới)
- **Project 2 — Backend cho Music Player App** (làm sau)

**Cách học đã thống nhất và giữ xuyên suốt:** có scaffold (khung sườn code) trước → tự code phần logic → gửi review → được chỉ rõ lỗi sai ở đâu, tại sao, sửa đúng kèm giải thích bản chất → nếu lặp lại cùng dạng lỗi, được chỉ ra pattern thay vì chỉ sửa lỗi đơn lẻ. Sau mỗi milestone, chạy full bộ test case qua Postman trước khi coi là hoàn thành.

**Điểm mạnh/yếu đã ghi nhận:**
- Mạnh: tư duy thiết kế tốt, nắm chắc Proxy Pattern/self-invocation, chủ động xin ôn tập, biết đọc log Hibernate/stack trace để tự chẩn đoán thay vì đoán mò (case `NoResourceFoundException` ở M1, case debug `DataIntegrityViolationException` message thật ở M2, case tự truy vết dữ liệu SQL để lý giải con số `progressPercent = 60%` "lạ" ở M3 thay vì nghi ngờ code sai).
- Cần lưu ý: hay nhầm đơn vị số liệu, hay quên quy đổi UTC, thỉnh thoảng bỏ sót 1 bước trong chuỗi logic nhiều bước.
  - **Cập nhật 01/09/2026:** ở M1 đã tự phát hiện + tự sửa đúng 1 lỗi cùng dạng (nhân nhầm đơn vị `refreshTokenExpirationMs`) qua build error, không cần người review chỉ ra trước.
  - **Cập nhật 02/09/2026:** ở M2 tự phát hiện đúng lỗ hổng logic "UNIQUE check tự chặn nhầm chính bản ghi đang update" trước khi được gợi ý đáp án, chỉ cần gợi ý hướng.
  - **Cập nhật 03/09/2026:** ở M3 tự phát hiện + tự sửa 1 lỗ hổng validate (`answer != null &&` khiến null lọt qua âm thầm) trong lúc tự refactor gộp vòng lặp, không cần được chỉ ra.

---

## 2. Tài liệu Requirements — hiện tại: **v1.5**

File chính: `Project1_Requirements_ChatbotHocTiengAnh_v1.5.md` (bump từ v1.4 lên v1.5 ở đợt review 13/09/2026 sau M8). So với v1.2 gốc, các thay đổi đã áp dụng:
- Mục 2.3 và mục 4: toàn bộ `LocalDateTime` → `Instant` cho timestamp (quyết định phát sinh khi code M1).
- Mục 7 (Roadmap): M1, M2 đã đánh dấu hoàn thành.
- Mục 8: 2 điểm mở đã chốt — Database (PostgreSQL), Refresh Token (có).

**Đã đồng bộ đầy đủ vào Requirements (xác nhận 05/09/2026):** mục 4 đã có ghi chú `UNIQUE(title, level)` + `ON DELETE CASCADE` cho `Topic`/`Flashcard`, mục 8 đã chốt công thức `progressPercent`/`COMPLETED`, mục 4 đã ghi rõ `QuizQuestion.options` dùng `@ElementCollection`. Không còn mục nào nợ lại từ M2/M3.

**Đã đồng bộ thêm ở đợt review 05/09/2026 (sau M5):**
- Mục 1.3: sửa bảng công nghệ từ "Java 17+, Spring Boot 3.x" / "Bootstrap" thành đúng thực tế "Java 21, Spring Boot 4.1.1" / "Tailwind CSS" (2 chỗ này bị lệch so với thực tế đang code khá lâu mới phát hiện).
- Mục 7 (Roadmap): M5 đánh dấu hoàn thành.
- FR-3.5: cập nhật cache key thực tế đã dùng (`topics::{size}`, `topicDetails::{id}`, `flashcardsByTopic::{topicId}`), chốt lại dependency Topic↔Flashcard (không cần invalidate chéo vì `TopicResponse` không có field tổng hợp).
- Mục 8: thêm dòng "Chiến lược Cache Eviction & Phòng ngừa Race Condition" — đã chốt.
- `FE_Handoff_Brief.md`: sửa Bootstrap → Tailwind, cập nhật cấu trúc `js/` khớp thực tế (`mockApi.js`, `navbar.js`, `tailwind-config.js`), làm rõ `PageResponse` là DTO tự định nghĩa chứ không phải `Page` thô của Spring Data.

**Đã đồng bộ thêm ở đợt review 08/09/2026 (sau M6):**
- FR-2.5: xác nhận đã implement, ghi rõ kỹ thuật tách `reply`/JSON meta bằng delimiter `---META---` khi streaming.
- Mục 8: thêm dòng "Kiến trúc WebSocket Streaming (FR-2.5/FR-2.7)" — đã chốt (2 điểm ownership CONNECT+SUBSCRIBE, error handler riêng cho STOMP, known limitation blocking save trong Reactor pipeline).
- Dọn 3 chỗ lỗi thời sót lại từ M3/M4 nhưng chưa từng sửa: FR-5.4 (vẫn ghi "chưa chốt" dù đã chốt từ M3), comment `Conversation.title` trong mục 4 (vẫn ghi "sẽ chốt khi implement M4" dù đã chốt), comment `QuizQuestion.options` trong mục 4 + dòng tương ứng ở mục 8 (vẫn ghi "sẽ chốt khi implement M3" dù đã chốt là `@ElementCollection`).
- `FE_Handoff_Brief.md`: ghi nhận lúc đó là **chưa sửa** — **XÁC NHẬN LẠI 10/09/2026: THÔNG TIN NÀY ĐÃ LỖI THỜI.** Người dùng đã tự cập nhật đầy đủ phần Chat (REST + toàn bộ WebSocket: STOMP CONNECT header, 2 kênh subscribe, kênh lỗi `/user/queue/errors`, DTO shape) trước đợt review M7, chỉ chưa kịp ghi nhận vào file tóm tắt này. Không còn nợ mục này.

**Đã đồng bộ thêm ở đợt review 10/09/2026 (sau M7):**
- FR-6: thêm đoạn "ĐÃ CHỐT 10/09/2026 (M7)" — vị trí lưu file, giới hạn 5MB, validate nội dung ảnh thật qua `ImageIO`, quy tắc sinh filename UUID theo format ảnh thật, cơ chế dọn file cũ qua `FileDeletionEvent`, quyết định chỉ hỗ trợ JPEG/PNG.
- Mục 1.4: thêm ghi chú bug `SecurityConfig` thiếu `/error` trong `permitAll()` — phát hiện lần đầu ở M7 (do lần đầu test token USER gọi route ADMIN) nhưng tồn tại từ M4, đã fix.
- Mục 7 (Roadmap): M7 đánh dấu hoàn thành.

**Đã đồng bộ thêm ở đợt review 13/09/2026 (sau M8):**
- NFR-5: thêm dòng "Hoàn thành 13/09/2026 (M8)" — 110 test case / 10 class, phạm vi Unit test thuần (Mockito mock toàn bộ dependency), không dùng `@DataJpaTest`/Testcontainers, coverage theo nhánh logic chứ không theo %.
- Mục 7 (Roadmap): M8 đánh dấu hoàn thành.
- Mục 1.4: thêm ghi chú 3 lỗi kỹ thuật lặp lại nhiều lần khi viết Unit Test (mock method trả object thường không stub → `null`; `ArgumentCaptor.capture()` khớp mọi type bất kể kiểu tham số; validate exact-match luôn chạy trước so sánh case-insensitive/trim) — không phải bug ở Service, mà là kiến thức nền tảng Mockito cần nhớ cho M9 trở đi.

**Quyết định đã chốt xuyên suốt:** Database = PostgreSQL, JWT = Access + Refresh Token, Timestamp = `Instant`/`TIMESTAMPTZ`, `QuizQuestion.options` = `@ElementCollection`, ngưỡng "đạt" 1 Quiz = `score >= 70`, MIME ảnh upload = JPEG/PNG only (5MB max).

---

## 3. Công cụ đang dùng

| Công cụ | Vai trò |
|---|---|
| **Antigravity** (Windows) | IDE code Backend |
| **9Router** (Linux VM) → Kiro AI free tier, Claude Sonnet 4.5 | Agent code Frontend |
| **Google Stitch** | Design UI, xuất HTML/Tailwind |
| **xKiro** (api.xkiro.com) | AI provider — đã tích hợp xong ở M4 |
| **GitHub** — `github.com/Pennywise111124/english-learning-platform`, nhánh `main` | Repo chính |
| **PostgreSQL + pgAdmin** | DB tên `english_learning_db` |
| **Postman** | Test API thủ công |

---

## 4. Tech stack đã chốt

- **Backend:** Java 21, Spring Boot 4.1.1, Maven, package `com.example.englishlearningplatform`
- **Database:** PostgreSQL, Flyway quản lý schema (`ddl-auto: validate`, không `update`), hiện tại đã có **V1 → V6**
- **Security:** Spring Security + JWT (Access + Refresh), BCrypt, `@Enumerated(EnumType.STRING)`, role-based (`hasRole("ADMIN")` cho `/api/admin/**`)
- **Timestamp:** `Instant` (Entity) ↔ `TIMESTAMPTZ` (PostgreSQL) — xuyên suốt mọi entity
- **Frontend:** HTML/JS thuần + Tailwind CSS qua CDN, `js/mockApi.js` sẵn sàng thay `api.js` thật
- **AI Provider:** xKiro, model `qwen/qwen3.6-plus:free`, context N=10 message, response format JSON {reply, correction, explanation}
- **Cache:** Redis qua Spring Cache abstraction (`@Cacheable`/`RedisCacheManager`), evict bằng `@TransactionalEventListener(phase = AFTER_COMMIT)` + `CacheManager` thủ công (không dùng `@CacheEvict` trực tiếp trên method `@Transactional`, tránh race condition evict-trước-commit) — chốt tại M5
- **File upload:** Local filesystem qua `app.upload.dir` (ngoài classpath), validate MIME header + nội dung ảnh thật qua `javax.imageio.ImageIO`, filename `UUID` theo format ảnh thật đọc được (không theo Content-Type/tên file client), serve qua `/uploads/**` (`WebMvcConfig` + `permitAll()`), dọn file cũ khi update/xoá qua `FileDeletionEvent`/`FileDeletionListener` — tái dùng đúng pattern `AFTER_COMMIT` đã chốt ở M5 cho cache — chốt tại M7

---

## 5. Tiến độ Frontend — ĐÃ XONG (mock data, merged `main`)

10 trang hoàn chỉnh, chưa nối Backend thật. Tài liệu: `FE_Handoff_Brief.md` — đã đồng bộ CSS framework (Tailwind) và cấu trúc `js/` khớp thực tế (05/09/2026). **Vẫn còn cần đồng bộ khi bắt đầu nối Backend thật cho `topics.html`/`quiz.html`/`progress.html`/`chat.html`**: response shape thật từ M2-M4 (VD: `ConversationDetailResponse` có thêm `updatedAt`), pagination qua `PageResponse`, endpoint `POST /api/conversations/{id}/messages` chưa có trong brief, và cache ở M5 không ảnh hưởng contract API nên không cần đổi gì thêm ở phần này.

---

## 6. Tiến độ Backend — cấu trúc project hiện tại (sau M8, 13/09/2026)

```
src/main/java/com/example/englishlearningplatform/
├── EnglishLearningPlatformApplication.java
├── ai/
│ ├── {AiChatMessage, AiChatResult, AiClient, AiClientImpl, AiProviderException, AiStreamEvent}
│ └── dto/{XkiroChatRequest, XkiroChatResponse, XkiroChatStreamChunk}
├── config/{AiProperties, AiWebClientConfig, SecurityConfig, RedisConfig, WebSocketConfig, WebMvcConfig} — class cuối mới thêm ở M7 (serve /uploads/**)
├── controller/
│ ├── AuthController, ConversationController, ChatWebSocketController, ProgressController, UserController
│ ├── TopicController, QuizController
│ └── Admin*: AdminTopicController, AdminFlashcardController, AdminQuizController, AdminQuizQuestionController
│   (UserController mới thêm ở M7 — POST /api/users/me/avatar)
├── dto/
│ ├── auth/{RegisterRequest, LoginRequest, AuthResponse, UserResponse}
│ ├── chat/{ConversationDetailResponse, ConversationSummaryResponse, MessageResponse, SendMessageRequest, ChatStreamEvent}
│ ├── common/PageResponse.java
│ ├── progress/UserProgressResponse.java
│ ├── quiz/{QuizRequest, QuizSummaryResponse, QuizQuestionRequest, QuizQuestionPublicResponse, QuizDetailResponse, SubmitAnswerItem, SubmitQuizRequest, QuizResultResponse, QuizAttemptResponse}
│ └── topic/{TopicCreateRequest, TopicUpdateRequest, TopicResponse, FlashcardCreateRequest, FlashcardUpdateRequest, FlashcardResponse}
├── entity/
│ ├── {User, RefreshToken, Role}
│ ├── {Topic, Flashcard, Level}
│ ├── {Quiz, QuizQuestion, QuizAttempt, UserProgress, ProgressStatus}
│ └── {Conversation, Message, Sender}
├── event/
│ ├── {TopicChangedEvent, TopicCacheEvictionListener} — M5
│ ├── {FlashcardChangedEvent, FlashcardCacheEvictionListener} — M5
│ └── {FileDeletionEvent, FileDeletionListener} — mới thêm ở M7 (dọn file ảnh cũ, cùng pattern AFTER_COMMIT với cache eviction)
├── exception/{GlobalExceptionHandler, ErrorResponse, ResourceNotFoundException, ResourceConflictException, InvalidFileException} — class cuối mới thêm ở M7
├── repository/
│ ├── {UserRepository, RefreshTokenRepository}
│ ├── {TopicRepository, FlashcardRepository}
│ ├── {QuizRepository, QuizQuestionRepository, QuizAttemptRepository, UserProgressRepository}
│ └── {ConversationRepository, MessageRepository}
├── security/{JwtUtil, JwtAuthenticationFilter, CustomUserDetailsService, StompAuthChannelInterceptor, StompErrorHandler} — 2 class cuối mới thêm ở M6
├── service/
│ ├── AuthService, ChatService, ChatServiceImpl
│ ├── {TopicService, FlashcardService}
│ ├── {QuizService, QuizAttemptService, UserProgressService}
│ └── {FileStorageService, FileStorageServiceImpl, UserService} — mới thêm ở M7
└── util/PaginationUtils.java

src/main/resources/
├── application.yml — thêm mục `app.upload.*` + `spring.servlet.multipart.*` ở M7
└── db/migration/ (V1 → V6, không đổi ở M5/M6/M7 — M7 không thêm entity/bảng mới, imageUrl/avatarUrl đã có sẵn từ M2/M1)

frontend/ (đã xong ở giai đoạn mock data — 10 trang: index/login/register/chat/topics/topic-detail/quiz/progress/profile/admin.html + js/{api,auth,mockApi,navbar,tailwind-config}.js + css/style.css + README.md. Có thêm `test-websocket.html` — công cụ test riêng, không nằm trong 10 trang chính thức. `FE_Handoff_Brief.md` đã được người dùng tự cập nhật đầy đủ phần Chat REST+WebSocket. Trạng thái `chat.html` có thực sự dùng SockJS/STOMP thật hay vẫn bản mock — CHƯA XÁC NHẬN, xem mục 7)
```

src/test/java/com/example/englishlearningplatform/ — MỚI HOÀN TOÀN Ở M8, trước đó chưa có
├── service/
│ ├── QuizAttemptServiceTest.java (19 test)
│ ├── UserProgressServiceTest.java (3 test)
│ ├── AuthServiceTest.java (10 test)
│ ├── TopicServiceTest.java (18 test)
│ ├── FlashcardServiceTest.java (12 test)
│ ├── FileStorageServiceImplTest.java (14 test)
│ └── ChatServiceImplTest.java (21 test)
├── security/JwtUtilTest.java (7 test)
└── event/
  ├── TopicCacheEvictionListenerTest.java (4 test)
  └── FlashcardCacheEvictionListenerTest.java (2 test)

### M1 — Auth + JWT: **HOÀN THÀNH 01/09/2026**

Register/Login/Refresh token, JWT filter, phân biệt role, exception handling nền tảng. Full test qua Postman.

**Bug đã tự phát hiện + tự sửa:**
- `AuthService.login()`: `expiresAt` nhân nhầm thêm `1_000_000` dù đơn vị đã đúng ms — refresh token gần như không bao giờ hết hạn.
- `NoResourceFoundException` (route không tồn tại) bị `handleGeneric` nuốt thành 500 thay vì 404 — phát hiện qua đọc log, không đoán mò.

### M2 — Topic/Flashcard CRUD: **HOÀN THÀNH 02/09/2026**

CRUD Admin + Read User cho Topic/Flashcard, pagination cơ bản (chưa search/filter/sort thật — để dành M10). 25 test case pass.

**Quyết định chốt:**
- `Topic` có `UNIQUE(title, level)` — tự quyết định trong lúc code, Requirements gốc không ghi.
- `Flashcard` cascade delete theo `Topic`.
- `imageUrl` KHÔNG có trong Create/Update request — chờ M7 (upload ảnh).
- `catch (DataIntegrityViolationException)` phải check message cụ thể chứa tên constraint, không bắt mù.

### M3 — Quiz CRUD + Submit/Attempt + Progress: **HOÀN THÀNH 03/09/2026**

CRUD Admin Quiz/Question (ẩn `correctAnswer` đúng FR-4.5), submit + chấm điểm với 3 rule anti-cheat (FR-4.3), cập nhật `UserProgress` theo công thức đã chốt, lịch sử attempt phân trang, `409 Conflict` cho Content deletion, cascade delete. ~27 test case pass.

**Quyết định chốt tại M3:**
- `QuizQuestion.options`: `@ElementCollection` (không dùng JSONB).
- **Công thức `progressPercent`/`COMPLETED` (FR-5.4) — ĐÃ CHỐT VÀ CODE XONG:**
  ```
  1 Quiz "đạt" ⟺ có ít nhất 1 QuizAttempt của User cho Quiz đó có score >= 70

  progressPercent = round(100 × (số Quiz "đạt") / (tổng số Quiz thuộc Topic))

  status = COMPLETED ⟺ (số Quiz "đạt") == (tổng số Quiz thuộc Topic) VÀ tổng số Quiz > 0
  status = IN_PROGRESS trong mọi trường hợp còn lại
  ```
- 3 rule anti-cheat khi submit (thứ tự bắt buộc): (1) `questionId` phải thuộc đúng Quiz đang submit, (2) không được trùng `questionId` trong 1 lần submit, (3) `answer` phải nằm trong `options` hợp lệ của câu hỏi đó. Chấm điểm dựa trên **tổng số câu hỏi thật của đề** (không phải số câu User chọn trả lời) để tránh "ăn gian" % bằng cách bỏ trống câu khó.
- `TopicService.deleteTopic()` + `QuizService.deleteQuiz()`: retrofit thêm check `409` nếu đã có `UserProgress`/`QuizAttempt` phụ thuộc.
- `DELETE /api/admin/questions/{id}` KHÔNG cần check 409 (QuizAttempt không tham chiếu từng Question).
- `@Valid` phải gắn cả ở field `List<SubmitAnswerItem> answers` bên trong `SubmitQuizRequest`, không chỉ ở tham số Controller — nếu thiếu, validate không xuống được từng phần tử list.

### M4 — Tích hợp AI qua xKiro (Chat REST): **HOÀN THÀNH 04/09/2026**

Entity `Conversation`/`Message` + enum `Sender`, `ChatService`/`ChatServiceImpl`, `ConversationController` (REST: create/list/detail/messages/send), package `ai/` (`AiClient`, `AiClientImpl`, `AiChatMessage`, `AiChatResult`, `AiProviderException`, `ai/dto/`), `AiWebClientConfig` (WebClient + Reactor Netty, connect/read timeout riêng biệt), migration V6. 25 test case pass qua Postman.

**Quyết định chốt tại M4:**
- 4 điểm mở ở Requirements mục 8 đã chốt: title tự động (cắt 50 ký tự), N=10 context message, model `qwen/qwen3.6-plus:free`, format JSON `{reply, correction, explanation}` — chi tiết xem Requirements.
- `ChatService`/`ChatServiceImpl` nhận `username` (không phải `userId`) — khớp cách `Authentication.getName()` trả về từ `CustomUserDetailsService`, tránh thêm cơ chế lấy user mới không nhất quán với `ProgressController` đã có.
- `sendMessage()` KHÔNG dùng `@Transactional` bao trùm toàn method — mỗi `.save()` tự commit riêng qua `SimpleJpaRepository`, tránh giữ transaction/connection DB mở suốt lúc gọi AI (network call). Đảm bảo đúng FR-2.6: Message User vẫn lưu dù AI lỗi.
- `GET /api/conversations` ban đầu trả thẳng `Page<T>` (format lệch chuẩn `PageResponse` của M2/M3) — phát hiện và sửa lại dùng `PageResponse.from()` + `PaginationUtils.validate()` cho nhất quán.
- **Bug phát sinh, đã fix, ảnh hưởng toàn project:** `SecurityConfig` trả 403 thay vì 401 khi thiếu JWT (thiếu `AuthenticationEntryPoint` custom) — đã thêm `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)`. Nên retest nhanh case "thiếu token" ở M1-M3 để confirm không đổi hành vi ngoài ý muốn.
- **Lưu ý kỹ thuật mới:** Spring Boot 4 dùng Jackson 3 (`tools.jackson.databind.json.JsonMapper`, không phải `com.fasterxml.jackson.databind.ObjectMapper`) — cần nhớ cho các lần dùng JSON thủ công sau này (kể cả ở Project 2).

### M5 — Redis Cache cho Topic/Flashcard: **HOÀN THÀNH 07/09/2026**

`RedisConfig` (`RedisCacheManager` với TTL riêng cho từng cache: `topics`, `topicDetails`, `flashcardsByTopic`), `@Cacheable` ở `TopicService.getTopics()`/`getTopicById()` và `FlashcardService.getFlashcardsByTopic()`, package `event/` (`TopicChangedEvent`, `TopicCacheEvictionListener`, `FlashcardChangedEvent`, `FlashcardCacheEvictionListener`). Test cache hit/miss + serialization qua Postman, test riêng 2 kịch bản race condition (update thành công / rollback) cho cả Topic và Flashcard — tất cả PASSED.

**Quyết định chốt tại M5:**
- **Cache key thật** (đơn giản hơn dự kiến ở Requirements FR-3.5 vì chưa có search/filter/sort — để dành M10): `topics::{size}` (chỉ cache `page = 0`), `topicDetails::{id}`, `flashcardsByTopic::{topicId}`.
- **Kiến trúc evict — quyết định quan trọng nhất của M5:** KHÔNG dùng `@CacheEvict` trực tiếp trên method có `@Transactional` (rủi ro race condition: cache bị evict trước khi transaction thật sự commit, request đọc gần như đồng thời có thể tái tạo cache với dữ liệu cũ). Thay bằng: Service publish domain event (`ApplicationEventPublisher`) sau khi `save()`/`delete()`, listener riêng dùng `@TransactionalEventListener(phase = AFTER_COMMIT)` mới evict cache thật qua `CacheManager` — đảm bảo evict chỉ chạy sau commit thành công, và hoàn toàn không chạy nếu rollback.
- Lưu ý self-invocation: listener evict thủ công qua `CacheManager.getCache(...).evict(...)`, không gọi lại method `@Cacheable`/`@CacheEvict` nội bộ qua `this.` — tránh bug quen thuộc "gọi qua `this` không đi qua Proxy nên annotation không chạy".
- `RedisSerializer.json()` (Object-level, có bật default typing bên trong) đã test thực tế OK với `PageResponse<TopicResponse>` (có `List` lồng bên trong) — không cần đổi sang serializer theo type cụ thể như phương án dự phòng đã cân nhắc.
- `TopicResponse` xác nhận **không** có field tổng hợp từ Flashcard (`flashcardCount`...) → 2 nhóm cache Topic và Flashcard hoàn toàn độc lập, không cần invalidate chéo (nếu sau này thêm field tổng hợp, phải bổ sung evict chéo ở `FlashcardService`).
- `Flashcard.topic` là `FetchType.LAZY` — lấy `topicId` trước khi publish event trong `deleteFlashcard()` vẫn an toàn vì nằm trong transaction, không gặp `LazyInitializationException`.

### M6 — WebSocket Streaming cho Chat: **HOÀN THÀNH 08/09/2026**

Chia 2 layer: (A) hạ tầng STOMP/WebSocket — xác thực JWT ở CONNECT qua `StompAuthChannelInterceptor`, ownership check ở SUBSCRIBE, error frame rõ ràng qua `StompErrorHandler` thay vì mất kết nối câm lặng; (B) AI trả lời stream chunk thật — `AiClientImpl chatStream()` dùng `bodyToFlux(ServerSentEvent)`, tách `reply` (stream trực tiếp) khỏi JSON meta bằng delimiter `---META---`. File mới: `config/WebSocketConfig`, `security/StompAuthChannelInterceptor`, `security/StompErrorHandler`, `controller ChatWebSocketController`, `ai/AiStreamEvent`, `ai/dto/XkiroChatStreamChunk`, `dto/chat/ChatStreamEvent`.

**Quyết định chốt tại M6:**
- FR-2.7 áp dụng ở **2 điểm** cho WebSocket (khác REST chỉ 1 điểm): CONNECT (xác thực JWT) và SUBSCRIBE (check ownership qua `chatService.isOwner()` mới thêm) — REST chỉ check ownership 1 lần ngay trong method xử lý.
- Lỗi ở tầng `ChannelInterceptor` (JWT sai, không có quyền) **không** đi qua `@MessageExceptionHandler` — phải viết riêng `StompSubProtocolErrorHandler` để trả STOMP ERROR frame có message rõ ràng trước khi Spring đóng kết nối (đóng kết nối là hành vi đúng chuẩn, không tránh được, chỉ cải thiện được nội dung thông báo).
- Lỗi xảy ra **trong** `.subscribe()` callback (bất đồng bộ, sau khi method `@MessageMapping` đã return) cũng không qua `@MessageExceptionHandler` được — phải tự bắt qua error callback của `subscribe()`, gửi thủ công qua `convertAndSendToUser()`.
- Bug đã tự phát hiện + tự sửa qua đọc log: `.map()` không cho phép trả về `null` (Reactive Streams spec) — chunk cuối SSE trước `[DONE]` thường có `delta.content = null`, làm `.map()` sập toàn bộ stream. Sửa bằng `.handle()` (cho phép bỏ qua phần tử thay vì bắt buộc emit).
- Bug đã tự phát hiện qua test có hệ thống (Nhóm A2/A3): thuật toán giữ "margin an toàn" (11 ký tự = độ dài delimiter - 1) để chống cắt ngang delimiter giữa 2 chunk, nhưng quên flush phần margin còn sót lại ra Client khi stream kết thúc mà không tìm thấy delimiter — DB lưu đủ, nhưng Client luôn thiếu đúng N ký tự cuối. Sửa bằng cách flush `pendingBuffer` thành 1 `ReplyChunk` cuối trước khi bắn event `Complete`.
- Known limitation chấp nhận: `save()` JPA blocking trong `.map()` của Reactor pipeline — không fix trừ khi đo được vấn đề thật ở traffic lớn.

### M7 — File upload ảnh Flashcard/Avatar: **HOÀN THÀNH 10/09/2026**

`FileStorageService`/`FileStorageServiceImpl` (validate MIME header → size → nội dung ảnh thật qua `ImageIO` → sinh UUID filename theo format ảnh thật → lưu ngoài source code), `WebMvcConfig` (serve tĩnh `/uploads/**`), `UserService`/`UserController` (mới, avatar), `InvalidFileException`, package `event/` bổ sung `FileDeletionEvent`/`FileDeletionListener` (dọn file cũ, cùng pattern `AFTER_COMMIT` với cache eviction M5). 4 endpoint mới: `POST /api/admin/topics/{id}/image`, `POST /api/admin/flashcards/{id}/image`, `POST /api/users/me/avatar`. 14 test case Postman pass (happy path, file giả mạo nội dung, sai MIME, quá size, thiếu field, 401/403/404, cache-consistency, dọn file cũ khi update/xoá).

**Quyết định chốt tại M7:**
- Vị trí lưu: `app.upload.dir` (config, ngoài classpath), chia subfolder `topics/`, `flashcards/`, `avatars/`.
- Giới hạn 5MB/file, chặn ở 2 lớp độc lập (Spring `multipart.max-file-size` chặn trước + tự check lại thủ công trong code).
- Validate nội dung ảnh thật bằng `ImageIO.getImageReaders()` + `reader.read(0)`, không dùng Apache Tika (chưa cần ở quy mô hiện tại). Extension sinh theo `formatName` đọc được thật (không theo Content-Type header hay tên file gốc) — cải tiến so với bản đầu chỉ dựa vào Content-Type.
- Chỉ hỗ trợ JPEG/PNG, cố ý không mở rộng GIF/BMP/WBMP dù `ImageIO` hỗ trợ sẵn — các decoder ít phổ biến hơn có bề mặt tấn công lớn hơn khi đọc file dị dạng, trong khi nhu cầu sản phẩm không cần.
- File cũ dọn qua `FileDeletionEvent`/`FileDeletionListener` — áp dụng lại đúng pattern `@TransactionalEventListener(AFTER_COMMIT)` đã chốt ở M5, tránh race condition xoá file trước khi transaction DB thật sự commit (ban đầu code xoá đồng bộ ngay trong transaction, tự phát hiện vấn đề và chủ động đề xuất sửa lại theo đúng pattern M5 trước khi test, không đợi được chỉ ra).
- **Bug phát sinh, đã fix, ảnh hưởng toàn project (không riêng M7):** `SecurityConfig` thiếu `.requestMatchers("/error").permitAll()` khiến MỌI lỗi 403 (`AccessDeniedException`) trong toàn hệ thống bị Spring Boot forward nội bộ sang `/error`, rồi bị chính Security Filter Chain chặn lại lần 2 (vì `JwtAuthenticationFilter` kế thừa `OncePerRequestFilter` mặc định skip dispatch `ERROR`, không set lại `SecurityContext`) → biến 403 thật thành 401 rỗng. Tồn tại từ M4 (lúc thêm `hasRole("ADMIN")`) nhưng chưa từng lộ ra vì chưa có test nào dùng token USER hợp lệ gọi route ADMIN cho tới M7. Phát hiện qua đọc kỹ log debug Security (`AccessDeniedHandlerImpl: Responding with 403` rồi `Securing GET /error` rồi `AnonymousAuthenticationFilter`), không đoán mò. Đã fix bằng cách thêm `/error` vào `permitAll()`.
- Tự phát hiện + tự sửa qua test thực tế: ban đầu chặn nhầm file có `Content-Type: application/octet-stream` (nhiều client, kể cả Postman, gửi header này khi không đoán được MIME) — sửa bằng cách chấp nhận `octet-stream` đi qua bước check header sơ bộ, nhưng lớp quyết định thật vẫn là validate nội dung ảnh qua `ImageIO`, nên không làm giảm độ an toàn.
- **Bug phát sinh sau khi test xong 14 case ban đầu, đã fix:** `dto/auth/UserResponse.java` (dùng chung cho login/register/avatar upload) ban đầu không có field `avatarUrl` — `POST /api/users/me/avatar` trả 200 kèm file lưu đúng trên đĩa, nhưng response không cho biết URL ảnh vừa upload, vi phạm trực tiếp FR-6.2. Phát hiện khi soạn `FE_Handoff_Brief.md` và đối chiếu lại kết quả test case 3 (response thiếu `avatarUrl`). Đã thêm field này vào `UserResponse`, kéo theo response của `login`/`register` từ giờ cũng có thêm `avatarUrl` (giá trị `null` nếu user chưa từng upload).

### M8 — Unit Test cho Service chính: **HOÀN THÀNH 13/09/2026**

JUnit 5 + Mockito, Unit test thuần (mock toàn bộ Repository/dependency), không `@DataJpaTest`/Testcontainers. Thứ tự ưu tiên theo độ phức tạp logic thuần (không theo milestone): `QuizAttemptService` → `UserProgressService` → `AuthService` → `TopicService`/`FlashcardService` [5 service chính bắt buộc theo NFR-5] → `FileStorageServiceImpl` → `ChatServiceImpl` [2 optional] → `JwtUtil` → 2 `CacheEvictionListener` [3 mục phát sinh trong lúc làm, ngoài kế hoạch ban đầu]. Tổng **110 test case / 10 class**, toàn bộ PASSED.

**Quyết định chốt tại M8:**
- Coverage: mỗi method — happy path + mọi nhánh throw exception, không chạy theo %; bỏ qua getter/setter DTO, constructor injection, method chỉ gọi thẳng 1 dòng repository không có nhánh rẽ.
- `UserProgressService` hoá ra chỉ có 1 method thuần map, không chứa công thức `progressPercent`/`COMPLETED` như dự kiến ban đầu trong kế hoạch — công thức đó thực ra nằm ở `QuizAttemptService.updateProgressAfterAttempt()` (private), đã test đầy đủ ở đó rồi.
- `FileStorageServiceImpl`/`JwtUtil` là 2 lớp duy nhất trong M8 KHÔNG dùng Mockito mock — test bằng cách chạy logic thật: `FileStorageServiceImpl` dùng `@TempDir` (JUnit) + ảnh thật sinh bằng `ImageIO.write()` để test I/O đĩa cứng thật; `JwtUtil` dùng `ReflectionTestUtils.setField()` inject `secret`/expiration (field chỉ nhận qua `@Value`, không có constructor phù hợp), rồi gọi thật `Jwts.builder()`/`Jwts.parser()` với key giả tự sinh.
- `ChatServiceImpl.sendMessageStream()` (trả `Flux`) test bằng `.collectList().block()` thay vì `StepVerifier`/`reactor-test` — đủ dùng vì Flux trong test luôn hữu hạn, đã biết trước toàn bộ dữ liệu, không có time-based scheduling thật.
- 3 lỗi kỹ thuật Mockito lặp lại nhiều lần trong lúc viết test (không phải bug ở Service) — chi tiết đầy đủ đã ghi ở Requirements mục 1.4: (1) mock method trả object thường (không `Optional`) quên stub → `null` → NPE; (2) `ArgumentCaptor.capture()` khớp MỌI lời gọi bất kể kiểu tham số, khác `any(Class)`; (3) validate exact-match luôn chạy TRƯỚC so sánh case-insensitive/trim, input "gần đúng" bị chặn sớm hơn tưởng.

---

## 7. Việc cần làm trước khi bắt đầu M9

1. ~~Đồng bộ `FE_Handoff_Brief.md` phần Chat~~ — **ĐÃ XÁC NHẬN HOÀN THÀNH 10/09/2026.** Không còn nợ.
2. **Xác nhận trạng thái `chat.html`** — vẫn CHƯA XÁC NHẬN đã cập nhật dùng SockJS/STOMP thật hay còn bản mock/REST (nợ lại từ trước M8 — M8 chỉ động tới Backend nên chưa xử lý). Cần xác nhận khi bắt đầu M9, đúng lúc Requirements mục 7 dự kiến M9 sẽ "polish UI toàn bộ".
3. ~~M7 (File upload ảnh Flashcard/avatar, FR-6)~~ — **ĐÃ HOÀN THÀNH 10/09/2026.** Chi tiết xem mục 6.
4. ~~M8 (Unit Test cho Service chính, NFR-5)~~ — **ĐÃ HOÀN THÀNH 13/09/2026.** 110 test case / 10 class. Chi tiết xem mục 6.
5. **M9 (Hoàn thiện `admin.html`, polish UI toàn bộ, test tổng thể end-to-end):** chưa có quyết định nào được chốt — cần xác định phạm vi cụ thể khi bắt đầu.

---

## 8. Khi bắt đầu cuộc trò chuyện mới — cần gửi kèm tài liệu gì?

**Bắt buộc:**
1. File tóm tắt này (`Session_Summary.md`)
2. `Project1_Requirements_ChatbotHocTiengAnh_v1.5.md` (Requirements đầy đủ — cần cho chi tiết FR/entity/API khi code)
3. `FE_Handoff_Brief.md` (nếu việc tiếp theo liên quan API contract, đặc biệt là M4 — Chat)

**Nếu đang code dở:** file `.java` đang dở + file nó phụ thuộc trực tiếp.

**Không cần gửi:** toàn bộ source code project, lịch sử chat cũ đầy đủ.

**Câu mở đầu gợi ý cho cuộc trò chuyện mới:**
> "Đây là tóm tắt project mình đang làm (đính kèm), M1-M2-M3-M4-M5-M6-M7-M8 đã xong (Auth, Topic/Flashcard CRUD, Quiz/Submit/Progress, AI Chat REST qua xKiro, Redis Cache cho Topic/Flashcard, WebSocket Streaming cho Chat, File upload ảnh Flashcard/avatar, Unit Test 110 case cho Service chính), giờ bắt đầu M9 (hoàn thiện admin.html, polish UI, test tổng thể end-to-end), tiếp tục giúp mình nhé."
# TÓM TẮT TOÀN BỘ QUÁ TRÌNH — Project "Nền tảng học tiếng Anh tích hợp AI"
*(Dùng để tiếp tục ở cuộc trò chuyện mới)*

**Ngày tóm tắt:** 03/10/2026 (cập nhật sau M12 — Dictation hoàn thành; toàn bộ M1–M12 xong, MVP + v1.1 đầy đủ)

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
  - **Cập nhật 03/10/2026:** ở M12 đọc đúng stack trace trong console để xác nhận nguyên nhân (`truncate()` ném `TypeError` vì `Topic.description = null` làm cả trang Admin báo "Failed to load topics"), và tự sửa lỗi stub Mockito lồng nhau (`UnfinishedStubbingException`) bằng cách tạo đối tượng mock ra biến riêng trước khi stub. Chủ động nêu ra 3 vấn đề trải nghiệm không có trong tài liệu: khó tìm bài Dictation khi số Topic tăng (dẫn tới danh mục có tìm kiếm/lọc), thiếu đường quay lại Topic từ trang Dictation, và khách không thấy được các chức năng của ứng dụng (dẫn tới sidebar cho khách + chuyển tới đúng nơi sau khi đăng nhập). Điểm cần lưu ý (cùng dạng "bỏ sót 1 bước trong chuỗi logic nhiều bước"): guard `requestId` ở `dictation.html` lần đầu thiếu dòng khai báo `const requestId = ++counter` và thiếu `return` trong nhánh `catch`, làm skeleton quay mãi — lỗi chỉ lộ khi chạy thật, nên mở lại trang ngay sau mỗi lần sửa.

---

## 2. Tài liệu Requirements — hiện tại: **v1.8**

File chính: `Requirements.md` (phiên bản 1.8; tên cũ `Project1_Requirements_ChatbotHocTiengAnh_v1.x.md`) (bump từ v1.7 lên v1.8 ở đợt review 03/10/2026 sau M12; trước đó bump từ v1.6 lên v1.7 ở đợt review 29/09/2026 sau M10+M11, xem chi tiết cuối mục này). So với v1.2 gốc, các thay đổi đã áp dụng:
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

**Đã đồng bộ đầy đủ ở đợt review 23/09/2026 (sau M9):** bump lên **v1.6**. Thêm mục 10 hoàn toàn mới — "Frontend Design System" (bảng màu, component signature "nút 3D", dark mode, cấu trúc app shell, module JS dùng chung) — bắt buộc tham chiếu khi code thêm trang mới. Cập nhật đầy đủ 6 endpoint mới phát sinh trong lúc làm M9 (không có trong Requirements gốc, xem mục 6 bên dưới), cập nhật FR-1.2 (login hỗ trợ username/email), thêm ràng buộc unique tên Quiz trong Topic, ghi nhận quyết định mở public 4 route Topic/Quiz-list.

**Đã đồng bộ đầy đủ ở đợt review 29/09/2026 (sau M10+M11):** bump lên **v1.7**. FR-3.5: cập nhật cache key thật cho M10 (`topics::{level|ALL}:{sort}:{size}`, chỉ cache khi `page=0` và không có `keyword`). FR-9: chốt 4 điểm còn mở của FR-9 (bộ giá trị `sort`, định nghĩa "lượt học" = đếm `UserProgress` riêng biệt, quyết định không thêm index ở M10, giới hạn `keyword` 50 ký tự có escape). FR-8: chốt thuật toán SRS (thang 5 bậc 1/3/7/14/30 ngày), cách hiện thực FR-8.5 (sort theo `difficulty`), rule lưu trùng trả 409, retrofit rule "không cascade" cho `UserVocabulary`. Thêm cột `Topic.createdAt` (migration V8) và entity `UserVocabulary` đầy đủ (migration V9) vào mục 4. Cập nhật mục 5 (API Endpoints): `GET /api/topics` thêm đủ tham số, mục Vocabulary/SRS thêm 3 endpoint phát sinh ngoài phạm vi gốc (`GET /api/vocabulary`, `GET /api/vocabulary/topics/{topicId}/saved-ids`, `DELETE /api/vocabulary/{id}`). Mục 7 (Roadmap): M10, M11 đánh dấu hoàn thành. Mục 10.9 (Backlog): gỡ dòng "Search/Level/Sort chưa có tác dụng" (đã làm xong), thêm 1 giới hạn đã biết mới của `vocabulary.html`. Mục 1.4: thêm 7 bullet lưu ý kỹ thuật mới từ M10/M11 (cache key theo chữ ký method, sort Enum-as-String sai thứ tự, tie-break bắt buộc trong ORDER BY phân trang, Specification dùng chung cho count query, thiếu `@Mock` khi đổi constructor không lỗi biên dịch mà lỗi lúc chạy, `UnnecessaryStubbingException` khi code throw sớm hơn dòng stub, `GlobalExceptionHandler` thiếu 2 handler cho lỗi kiểu dữ liệu, retrofit rule cascade khi thêm entity cá nhân mới tham chiếu FK).

**Đã đồng bộ đầy đủ ở đợt review 03/10/2026 (sau M12):** bump lên **v1.8**. FR-7 viết lại hoàn toàn theo thực tế (upload audio thay URL, thuật toán so sánh + công thức accuracy, không lộ transcript, ownership + quy tắc xoá, mã HTTP) và thêm **FR-7.8** (danh mục bài Dictation kèm số liệu cá nhân). FR-9: danh mục Dictation thuộc nhóm "Search + Filter + Sort + Pagination đầy đủ". Mục 2.2: ownership của `DictationResult` mở rộng cho số liệu cá nhân trong danh mục. Mục 4: `DictationLesson`/`DictationResult` đầy đủ (migration V10, cascade/không cascade, index). Mục 5: bảng Dictation 9 endpoint (6 gốc + 3 bổ sung). Mục 7: M12 hoàn thành. Mục 8: thêm 3 dòng đã chốt (nguồn audio, thuật toán, quyền + cache). Mục 10: 10.4 (activePage mới), 10.5 (sidebar cho khách), 10.6 (`redirectTo`), 10.7 (`audioSrc`, `ui.js`, `topicPicker.js`), 10.9 (backlog mới). Mục 1.4: thêm 6 bullet lưu ý kỹ thuật từ M12 (Sort ghi đè `orderBy` của Specification, `NULL` lên đầu khi `DESC`, cascade DB không xoá file, 3 handler còn thiếu trong `GlobalExceptionHandler`, 2 constructor + stub Mockito lồng nhau, `catch` che mất nguyên nhân thật).

**Quyết định đã chốt xuyên suốt:** Database = PostgreSQL, JWT = Access + Refresh Token, Timestamp = `Instant`/`TIMESTAMPTZ`, `QuizQuestion.options` = `@ElementCollection`, ngưỡng "đạt" 1 Quiz = `score >= 70`, MIME ảnh upload = JPEG/PNG only (5MB max), `Topic` sort mặc định = mới nhất, "lượt học" (popular) = số người học riêng biệt qua `UserProgress`, SRS = thang cố định 5 bậc 1/3/7/14/30 ngày (không SM-2), Dictation = audio **upload** (MP3/WAV/OGG/M4A, 10MB, magic bytes) thay vì URL external, so sánh bằng LCS mức từ với `accuracy = round1(100 × đúng / max(n_transcript, n_user))` và không bao giờ lộ transcript trước khi nộp.

---

## 3. Công cụ đang dùng

| Công cụ | Vai trò |
|---|---|
| **Antigravity** (Windows) | IDE code Backend |
| **9Router** (Linux VM) → Kiro AI free tier, Claude Sonnet 4.5 | Môi trường dev chạy Frontend (M1-M8: agent tự code UI; M9: chủ yếu dùng để chạy static server test FE + chạy Impeccable, phần lớn code redesign làm trực tiếp qua chat) |
| **Google Stitch** | Design UI, xuất HTML/Tailwind |
| **ngrok** (free tier) | Expose Backend (Windows) ra ngoài cho Linux VM gọi vào qua HTTPS — dùng từ M9 |
| **Impeccable** (`pbakaus/impeccable`) | Skill audit/polish UI trong Claude Code — dùng từ M9, xem chi tiết mục 6 phần M9 |
| **xKiro** (api.xkiro.com) | AI provider — đã tích hợp xong ở M4 |
| **GitHub** — `github.com/Pennywise111124/english-learning-platform`, nhánh `main` | Repo chính |
| **PostgreSQL + pgAdmin** | DB tên `english_learning_db` |
| **Postman** | Test API thủ công |

---

## 4. Tech stack đã chốt

- **Backend:** Java 21, Spring Boot 4.1.1, Maven, package `com.example.englishlearningplatform`
- **Database:** PostgreSQL, Flyway quản lý schema (`ddl-auto: validate`, không `update`), hiện tại đã có **V1 → V10** (V8: thêm `Topic.createdAt` cho sort=newest — M10; V9: bảng `user_vocabulary` — M11; V10: bảng `dictation_lessons` (FK `topic_id` ON DELETE CASCADE) + `dictation_results` (không cascade) — M12)
- **Security:** Spring Security + JWT (Access + Refresh), BCrypt, `@Enumerated(EnumType.STRING)`, role-based (`hasRole("ADMIN")` cho `/api/admin/**`)
- **Timestamp:** `Instant` (Entity) ↔ `TIMESTAMPTZ` (PostgreSQL) — xuyên suốt mọi entity
- **Frontend:** HTML/JS thuần + Tailwind CSS qua CDN, `js/api.js` gọi API thật (không còn mock từ M9) — chi tiết đầy đủ xem mục 5
- **AI Provider:** xKiro, model `qwen/qwen3.6-plus:free`, context N=10 message, response format JSON {reply, correction, explanation}
- **Cache:** Redis qua Spring Cache abstraction (`@Cacheable`/`RedisCacheManager`), evict bằng `@TransactionalEventListener(phase = AFTER_COMMIT)` + `CacheManager` thủ công (không dùng `@CacheEvict` trực tiếp trên method `@Transactional`, tránh race condition evict-trước-commit) — chốt tại M5
- **File upload:** Local filesystem qua `app.upload.dir` (ngoài classpath), validate MIME header + nội dung ảnh thật qua `javax.imageio.ImageIO`, filename `UUID` theo format ảnh thật đọc được (không theo Content-Type/tên file client), serve qua `/uploads/**` (`WebMvcConfig` + `permitAll()`), dọn file cũ khi update/xoá qua `FileDeletionEvent`/`FileDeletionListener` — tái dùng đúng pattern `AFTER_COMMIT` đã chốt ở M5 cho cache — chốt tại M7
- **Audio upload (Dictation, M12):** dùng chung `FileStorageService`, thêm `storeAudio(file, "dictation")`. Định dạng nhận dạng bằng **magic bytes** của 12 byte đầu (MP3: `ID3` hoặc frame sync `0xFF`+`(b1 & 0xE0)==0xE0`; WAV: `RIFF`…`WAVE`; OGG: `OggS`; M4A: `ftyp` ở byte 4-7), không tin `Content-Type`/đuôi file; đuôi file lưu lấy từ nội dung nhận dạng được. Giới hạn riêng `app.upload.audio-max-file-size-mb` = 10, `spring.servlet.multipart` nâng lên 10MB (ảnh vẫn 5MB ở tầng Service). Constructor `FileStorageServiceImpl` có 2 phiên bản (3 tham số cho test cũ, 4 tham số có `@Autowired`). Dọn file qua `FileDeletionEvent` như ảnh — chốt tại M12
- **CORS:** cấu hình tường minh qua `CorsConfigurationSource` bean (không có mặc định) — `setAllowedOriginPatterns` (không phải `setAllowedOrigins`, bắt buộc khi kết hợp `allowCredentials(true)`), preflight `OPTIONS` permitAll riêng — chốt tại M9
- **API Documentation:** springdoc-openapi 3.1.1, Swagger UI tại `/swagger-ui.html` — thêm tại M9, dùng làm nguồn đối chiếu contract thật khi có sai lệch với Requirements
- **Testing:** JUnit 5 + Mockito, tổng **198 test case** tính tới hết M12 (số lấy từ log `mvnw test` ngày 02/10/2026; 14 class: `AuthServiceTest` 10, `ChatServiceImplTest` 21, `DictationComparatorTest` 19, `DictationServiceTest` 32, `FileStorageServiceImplTest` 31, `FlashcardServiceTest` 13, `QuizAttemptServiceTest` 19, `TopicServiceTest` 21, `UserProgressServiceTest` 3, `VocabularyServiceTest` 12, `TopicSortTest` 4, `JwtUtilTest` 7, `TopicCacheEvictionListenerTest` 4, `FlashcardCacheEvictionListenerTest` 2). Không có test riêng cho các `Specification` (`TopicSpecifications`, `DictationLessonSpecifications` — logic query, được phủ qua bộ Postman thay vì Mockito) và không có test cho `getTopics()`/`getAllSaved()` (chỉ dựng Specification/Pageable rồi uỷ quyền, không có nhánh rẽ). Riêng `getCatalog()` của Dictation có test vì có nhánh gộp thống kê (trang rỗng không gọi truy vấn thống kê, `Pageable` không kèm Sort).

---

## 5. Tiến độ Frontend — HOÀN THÀNH, đã nối Backend thật + redesign UI (M9, 23/09/2026)

10 trang, đã bỏ hẳn `js/mockApi.js` — toàn bộ gọi API thật qua `js/api.js`. **Đổi kiến trúc trang so với giai đoạn mock:**
- **Bỏ hẳn** `login.html`/`register.html` — gộp vào modal chung (`authModal.js`), mở qua `index.html?auth=login` hoặc `data-open-login`/`data-open-register` attribute.
- **Đổi hẳn** navbar ngang → **sidebar trái thu gọn được + topbar riêng** (`navbar.js` viết lại hoàn toàn, xem chi tiết Requirements mục 10.5).
- Thêm mới: `topic-detail.html` vẫn giữ tên cũ, không đổi.

**Các trang hiện tại:** 10 trang chính — `index.html`, `chat.html`, `topics.html`, `topic-detail.html`, `quiz.html`, `progress.html`, `profile.html`, `admin.html`, `vocabulary.html` (M11), `dictation.html` (M12) — cộng `test-websocket.html` (công cụ test riêng, không tính vào bộ chính thức).

**Module JS dùng chung (mới, không có ở giai đoạn mock):** `config.js`, `toast.js`, `chat.js`, `footer.js`, `theme.js`, `authModal.js`, `ui.js` (M12), `topicPicker.js` (M12) — vai trò từng file xem Requirements mục 10.7.

**Redesign UI hoàn chỉnh (Duolingo-inspired):** đổi bảng màu, thêm component "nút 3D" (`btn-3d` + `border-*-shadow`), dark mode thật (CSS custom property, không phải chỉ khai báo `darkMode: "class"` suông như dự kiến ban đầu), skeleton loading toàn site, empty/error state chuẩn hoá. Chi tiết đầy đủ đã chuyển hẳn vào Requirements mục 10 (không lặp lại ở đây, tránh 2 nguồn dễ lệch nhau) — **khi cần chi tiết design system, luôn tham chiếu Requirements, không phải file này**.

**Cập nhật M10:** `topics.html` nối thật 3 ô Search/Level/Sort (trước đó chỉ có UI, `keyword`/`level`/`sort` bị Backend bỏ qua) — thêm debounce 300ms cho ô search, guard chống response trả về trễ ghi đè kết quả mới hơn (biến đếm `requestSeq`), `maxlength=50` khớp giới hạn Backend, đổi 4 option sort cũ (A-Z/Z-A/Level asc/desc) thành 3 option mới (`newest`/`popular`/`title`) khớp đúng bộ giá trị `sort` server chấp nhận.

**Cập nhật M11:** trang mới `vocabulary.html` (yêu cầu đăng nhập, thêm mục "My Words" vào sidebar `navbar.js`) — gồm 2 tab: "Due Today" (ôn từ đến hạn, lật thẻ, Next/Previous duyệt xem không cần đánh giá, Remembered/Not yet gọi API rồi loại khỏi hàng đợi tại chỗ) và "All Words" (xem + xoá toàn bộ sổ từ đã lưu, phân trang, badge trạng thái NEW/LEARNING/KNOWN theo màu). Nút Save mới thêm vào `topic-detail.html` (trên mặt trước flashcard) — hiện đúng trạng thái đã lưu/chưa lưu ngay khi tải trang (gọi song song 1 API riêng lấy set `flashcardId` đã lưu trong Topic đang xem), khách chưa đăng nhập bấm Save sẽ mở modal Login thay vì gọi API.

**Cập nhật M12:** trang mới `dictation.html` (yêu cầu đăng nhập, mục "Dictation" trong sidebar) gồm 2 màn: **danh mục** (ô tìm kiếm, lọc Topic bằng `topicPicker`, lọc Level/tiến độ, sắp xếp, phân trang; mỗi thẻ hiện Topic, Level, "Best x%", số lần làm; bộ lọc lưu trên URL nên `?topicId=` mở trực tiếp được, có liên kết "Back to <Topic>" khi đang lọc theo Topic) và **luyện tập** (trình phát audio, ô gõ, kết quả từng từ `CORRECT/WRONG/MISSING/EXTRA` có chữ + gạch + viền riêng chứ không chỉ dựa vào màu, "New personal best!", lịch sử các lần làm có phân trang). `admin.html` thêm tab Dictation (tạo/sửa/xoá bài, upload + nghe thử audio, bài chưa có audio có nhãn cảnh báo) và được làm lại ở 3 điểm: tab Topics có tìm kiếm/lọc Level/sắp xếp/phân trang 10 Topic mỗi trang (mặc định Newest first, khớp `topics.html`); **một ô "Working topic" dùng chung** (`topicPicker`) thay 3 dropdown cũ, nhớ lựa chọn qua `sessionStorage` + URL (`?tab=&topicId=`); mỗi tab Flashcards/Quizzes/Dictation có thanh lọc trong trình duyệt. `navbar.js`: **khách vẫn thấy sidebar** (mục cần tài khoản có ổ khoá và mở modal đăng nhập), `authModal.js` nhận `redirectTo` nên đăng nhập xong khách được đưa tới đúng nơi vừa bấm. `topic-detail.html`: thêm thẻ Dictation, thẻ Quiz/Dictation/nút Save của khách mở modal đăng nhập thay vì bị đẩy về `index.html`, và sửa lỗi dựng tiêu đề Topic bằng `innerHTML`. Các dropdown bộ lọc là component tự vẽ (`createSelect` trong `ui.js`, có ARIA + bàn phím).

`FE_Handoff_Brief.md` — **CẬP NHẬT 03/10/2026 (sau M12; bản gốc 29/09/2026):** đã được "hồi sinh" lại làm tài liệu tra cứu nhanh (khác hẳn vai trò brief-build-mock ban đầu ở M1-M8), đồng bộ đầy đủ endpoint + shape dữ liệu mới của M12 (khối Dictation, gồm các endpoint bổ sung và các component dùng chung `ui.js`/`topicPicker.js`), M10 (`GET /api/topics` đủ tham số) và M11 (khối Vocabulary/SRS, gồm cả 3 endpoint bổ sung ngoài phạm vi gốc). Từ giờ nên gửi kèm file này khi cần tra nhanh contract API mà không muốn đọc lại toàn bộ Requirements — Swagger UI (`/swagger-ui.html`) vẫn là nguồn xác nhận cao nhất khi có sai lệch.

---

## 6. Tiến độ Backend — cấu trúc project hiện tại (sau M12, 03/10/2026)

```
src/main/java/com/example/englishlearningplatform/
├── EnglishLearningPlatformApplication.java
├── ai/
│ ├── {AiChatMessage, AiChatResult, AiClient, AiClientImpl, AiProviderException, AiStreamEvent}
│ └── dto/{XkiroChatRequest, XkiroChatResponse, XkiroChatStreamChunk}
├── config/{AiProperties, AiWebClientConfig, SecurityConfig, RedisConfig, WebSocketConfig, WebMvcConfig, TimeConfig} — TimeConfig mới thêm ở M11 (bean Clock, inject vào VocabularyService để unit test SRS với thời gian cố định); M12: DictationService cũng inject Clock
├── controller/
│ ├── AuthController, ConversationController, ChatWebSocketController, ProgressController, UserController
│ ├── TopicController, QuizController, VocabularyController (M11), DictationController (M12 — GET /api/topics/{id}/dictation, GET /api/dictation/lessons, submit, results)
│ └── Admin*: AdminTopicController, AdminFlashcardController, AdminQuizController, AdminQuizQuestionController, AdminDictationController (M12)
│   (UserController mới thêm ở M7 — POST /api/users/me/avatar)
├── dto/
│ ├── auth/{RegisterRequest, LoginRequest, AuthResponse, UserResponse}
│ ├── chat/{ConversationDetailResponse, ConversationSummaryResponse, MessageResponse, SendMessageRequest, ChatStreamEvent}
│ ├── common/PageResponse.java
│ ├── dictation/{DictationLessonRequest, DictationSubmitRequest, DictationLessonResponse, AdminDictationLessonResponse, DictationWordResult, WordStatus, DictationSubmitResponse, DictationResultResponse, DictationCatalogItem, DictationSort, DictationProgress} — package mới hoàn toàn ở M12 (DTO response dùng `record`)
│ ├── progress/UserProgressResponse.java
│ ├── quiz/{QuizRequest, QuizSummaryResponse, QuizQuestionRequest, QuizQuestionPublicResponse, QuizDetailResponse, SubmitAnswerItem, SubmitQuizRequest, QuizResultResponse, QuizAttemptResponse}
│ ├── topic/{TopicCreateRequest, TopicUpdateRequest, TopicResponse, TopicSort, FlashcardCreateRequest, FlashcardUpdateRequest, FlashcardResponse} — TopicSort mới thêm ở M10
│ └── vocabulary/{ReviewRequest, VocabularyResponse} — package mới hoàn toàn ở M11
├── entity/
│ ├── {User, RefreshToken, Role}
│ ├── {Topic, Flashcard, Level} — Topic thêm field createdAt ở M10
│ ├── {Quiz, QuizQuestion, QuizAttempt, UserProgress, ProgressStatus}
│ ├── {UserVocabulary, VocabularyStatus} — mới hoàn toàn ở M11
│ ├── {DictationLesson, DictationResult} — mới hoàn toàn ở M12
│ └── {Conversation, Message, Sender}
├── event/
│ ├── {TopicChangedEvent, TopicCacheEvictionListener} — M5
│ ├── {FlashcardChangedEvent, FlashcardCacheEvictionListener} — M5
│ └── {FileDeletionEvent, FileDeletionListener} — mới thêm ở M7 (dọn file ảnh cũ, cùng pattern AFTER_COMMIT với cache eviction); M12: còn dọn file audio Dictation (kể cả khi xoá cả Topic — TopicService.deleteTopic() publish 1 event cho mỗi audio)
├── exception/{GlobalExceptionHandler, ErrorResponse, ResourceNotFoundException, ResourceConflictException, InvalidFileException} — M11: thêm 2 handler (HttpMessageNotReadableException, MethodArgumentTypeMismatchException), vá lỗ hổng cũ từ M1-M7 (2 loại lỗi này trước đó rơi vào 500 thay vì 400); M12: thêm 3 handler cùng loại lỗ hổng (HttpRequestMethodNotSupportedException → 405, HttpMediaTypeNotSupportedException → 415, MultipartException → 400)
├── repository/
│ ├── {UserRepository, RefreshTokenRepository}
│ ├── {TopicRepository, TopicSpecifications, FlashcardRepository} — TopicSpecifications mới ở M10 (JpaSpecificationExecutor, gộp filter+sort search Topic)
│ ├── {QuizRepository, QuizQuestionRepository, QuizAttemptRepository, UserProgressRepository}
│ ├── UserVocabularyRepository — mới ở M11
│ ├── {DictationLessonRepository, DictationResultRepository, DictationLessonSpecifications, DictationLessonStats} — mới ở M12 (Specification cho danh mục; DictationLessonStats là projection của truy vấn thống kê GROUP BY)
│ └── {ConversationRepository, MessageRepository}
├── security/{JwtUtil, JwtAuthenticationFilter, CustomUserDetailsService, StompAuthChannelInterceptor, StompErrorHandler} — 2 class cuối mới thêm ở M6
├── service/
│ ├── AuthService, ChatService, ChatServiceImpl
│ ├── {TopicService, FlashcardService} — cả 2 retrofit thêm check UserVocabularyRepository ở M11 (chặn xoá khi đã có User lưu từ); M12: TopicService retrofit thêm check DictationResult + dọn file audio
│ ├── {QuizService, QuizAttemptService, UserProgressService}
│ ├── VocabularyService — mới hoàn toàn ở M11
│ ├── {DictationService, DictationComparator} — mới hoàn toàn ở M12 (DictationComparator là class static pure function: tokenize + LCS mức từ, không phụ thuộc Spring)
│ └── {FileStorageService, FileStorageServiceImpl, UserService} — mới thêm ở M7; M12: `FileStorageService` thêm `storeAudio`; `FileStorageServiceImpl` thêm `detectAudioExtension` (static, nhận dạng bằng magic bytes)
└── util/PaginationUtils.java

src/main/resources/
├── application.yml — thêm mục `app.upload.*` + `spring.servlet.multipart.*` ở M7; M12: thêm `app.upload.audio-max-file-size-mb` (10), nâng multipart lên 10MB
└── db/migration/ (V1 → V10 — V8 thêm cột Topic.createdAt ở M10, V9 tạo bảng user_vocabulary ở M11, V10 tạo dictation_lessons + dictation_results ở M12)

frontend/ — trạng thái thật đã đổi hoàn toàn ở M9, xem mục 5 (không mô tả lại ở đây để tránh 2 nguồn lệch nhau)
```

src/test/java/com/example/englishlearningplatform/ — dựng nền ở M8, mở rộng ở M10/M11/M12
├── service/
│ ├── QuizAttemptServiceTest.java (19 test)
│ ├── UserProgressServiceTest.java (3 test)
│ ├── AuthServiceTest.java (10 test)
│ ├── TopicServiceTest.java (21 test — M11 thêm test conflict "đã có flashcard được lưu"; M12 thêm 2 test: conflict "đã có DictationResult" và "đọc URL audio trước khi xoá + publish FileDeletionEvent cho từng file")
│ ├── FlashcardServiceTest.java (13 test — M11 thêm test conflict "đã được User lưu")
│ ├── VocabularyServiceTest.java (12 test — mới hoàn toàn ở M11: applyReview từng bậc SRS, saveWord, review ownership, getAllSaved, deleteSavedWord)
│ ├── FileStorageServiceImplTest.java (31 test — M12 thêm 17: detectAudioExtension từng định dạng, JPEG/văn bản/RIFF-không-phải-WAVE/null không bị nhận nhầm, storeAudio happy path/quá dung lượng/giả audio/ảnh giả audio/đuôi lấy từ nội dung/thư mục bị chặn)
│ ├── ChatServiceImplTest.java (21 test)
│ ├── DictationComparatorTest.java — mới ở M12 (19 test: tokenize, mỗi nhãn CORRECT/WRONG/MISSING/EXTRA, khoảng không cân, quy ước khi hoà, làm tròn, giới hạn 1000 từ)
│ └── DictationServiceTest.java — mới ở M12 (32 test: phía User + Admin + danh mục; ownership, 409, thứ tự storeAudio/FileDeletionEvent, `Pageable` không kèm Sort)
├── dto/topic/TopicSortTest.java (4 test) — mới ở M10 (parse null/blank/hoa-thường/giá trị lạ)
├── security/JwtUtilTest.java (7 test)
└── event/
  ├── TopicCacheEvictionListenerTest.java (4 test)
  └── FlashcardCacheEvictionListenerTest.java (2 test)

Tổng cộng tới hết M12: 198 test case, toàn bộ PASSED.

### M9 — Hoàn thiện Admin, Polish UI, Nối Backend thật, Test tổng thể: **HOÀN THÀNH 23/09/2026**

Khối lượng lớn nhất từ trước tới giờ, gồm cả Backend lẫn Frontend, chia làm 4 phần:

**A. Backend — 6 endpoint mới + 2 thay đổi hành vi (không có trong Requirements gốc, phát sinh từ nhu cầu thực tế lúc nối FE):**
- `GET /api/users/me` — lấy lại profile hiện tại (thiếu ngay từ đầu, chỉ có avatar upload)
- `POST /api/users/me/password` — đổi mật khẩu (form Change Password ở `profile.html` từng chỉ giả lập thành công, không có API thật)
- `GET /api/users/me/quiz-attempts?page=&size=` — lịch sử làm bài **tổng hợp mọi Quiz** (khác `GET /api/quizzes/{id}/attempts` chỉ giới hạn 1 Quiz — `progress.html` cần cái tổng hợp, phát hiện lúc test thấy hardcode nhầm `quizId=1`)
- `GET /api/admin/quizzes/{quizId}/questions` — Admin xem lại câu hỏi **kèm** `correctAnswer` (trước đó Admin sửa câu hỏi cũ không thấy được đáp án đúng hiện tại)
- Login hỗ trợ **username hoặc email** (`findByUsername().or(() -> findByEmail())`) — UI ghi "Username or Email" nhưng Backend ban đầu chỉ nhận username, phát hiện qua review kỹ giao diện
- `GET /api/topics`, `GET /api/topics/{id}`, `GET /api/topics/{id}/flashcards`, `GET /api/topics/{id}/quizzes` đổi thành **public** — cho khách chưa đăng nhập xem trước (đúng tinh thần landing page mời chào), quyết định có chủ đích chứ không phải sơ hở: `GET /api/topics/{id}/quizzes` chỉ trả tên Quiz, còn `GET /api/quizzes/{id}` (câu hỏi thật)/`submit`/`attempts` vẫn đòi JWT như cũ

**B. Backend — sửa lỗi/bổ sung ràng buộc:**
- CORS: thêm hoàn toàn mới (`CorsConfigurationSource`, `allowCredentials(true)`, `setAllowedOriginPatterns`) — không có sẵn ở M1-M8 vì trước đó FE/BE luôn chạy cùng origin lúc test Postman, chỉ lộ ra khi FE chạy trên máy ảo Linux khác origin
- Unique constraint tên Quiz trong cùng Topic (không phân biệt hoa/thường) — migration V7, `existsByTopicIdAndTitleIgnoreCase`

**C. Frontend — nối API thật + redesign UI hoàn chỉnh.** Chi tiết kiến trúc/design system đầy đủ đã ghi ở Requirements mục 10 — không lặp lại ở đây.

**D. QA tổng thể end-to-end** — đi hết luồng khách vãng lai → đăng ký → học tập đầy đủ (Chat/Topic/Flashcard/Quiz/Progress) → Admin CRUD, cả 2 chế độ màu và cả di động. Phát hiện thêm vài bug nhỏ trong lúc QA (tiêu đề dài tràn khung ở 2 chỗ khác nhau trong `chat.html`, "Take Quiz" im lặng khi Topic chưa có Quiz, Quiz rỗng bị treo loading, nút Next/Submit lệch trái ở câu đầu) — đã sửa hết.

**Công cụ mới dùng ở M9, không có trong danh sách công cụ ở mục 3:**
- **ngrok** (free tier) — expose Backend chạy trên máy Windows thật ra ngoài cho Linux VM gọi vào qua HTTPS, vì 2 máy khác mạng/khác origin. Giới hạn đã biết: trang cảnh báo interstitial của ngrok free chặn request đầu tiên từ phiên trình duyệt mới — đã vá cho REST (header `ngrok-skip-browser-warning` gắn trong `api.js`), **chưa vá được cho SockJS** (thư viện tự gọi request nội bộ riêng, không có header đó) — chỉ ảnh hưởng lúc test qua tunnel dev, biến mất khi deploy domain thật.
- **Impeccable** (`pbakaus/impeccable`, cài qua `npx impeccable install`) — skill audit/polish UI chạy trong Claude Code, dùng cho 1 đợt audit accessibility/performance trên `index.html` (phát hiện: thiếu skip link, logo không phải link, ảnh hero thiếu `loading="lazy"`, thiếu `focus-visible`...) và 1 lần polish khu vực `#resultView` của `quiz.html` (SVG progress ring animated thay khung viền tĩnh). Chỉ dùng 2 lần trong toàn bộ M9 — phần lớn redesign còn lại làm trực tiếp trong chat để giữ đúng `id`/logic.

### M10 — Search/Filter/Pagination cho Topic: **HOÀN THÀNH 29/09/2026**

`GET /api/topics` mở rộng nhận `keyword`/`level`/`sort` cùng `page`/`size` cũ, giữ nguyên public. Migration V8 (thêm `Topic.createdAt`), `TopicSpecifications` (gộp filter + `ORDER BY` vào 1 `Specification` dùng chung cho cả query dữ liệu lẫn count, phải tự kiểm tra `query.getResultType()` để không áp `orderBy` vào nhánh count), `TopicSort` enum (`NEWEST`/`POPULAR`/`TITLE`, parse không phân biệt hoa/thường, ném lỗi rõ ràng cho giá trị lạ thay vì âm thầm rơi về mặc định). Cache key đổi từ `topics::{size}` (M5) sang `topics::{level|ALL}:{sort}:{size}`, chỉ cache khi `page=0` và không có `keyword`. `topics.html` nối thật (xem mục 5). Bộ Postman ~40 case (search/filter/sort/pagination/cache/hồi quy) đều PASSED.

**Quyết định chốt tại M10:**
- "Lượt học" cho `sort=popular` = đếm số bản ghi `UserProgress` riêng biệt theo Topic (không thêm counter/entity mới, không đếm lượt xem `GET /api/topics/{id}` vì endpoint đó public và đang cache, đếm ở đó sẽ ghi DB trên mỗi lượt đọc của khách).
- Không thêm index mới cho `Topic.title`/`level` ở M10 (khác dự kiến ban đầu ở Requirements) — B-tree không hỗ trợ `LIKE '%kw%'`, `level` chỉ 3 giá trị nên gần như vô ích, `UNIQUE(title, level)` đã có sẵn 1 index dùng được 1 phần. Để dành `pg_trgm` (GIN) nếu sau này thật sự cần.
- Sort theo `level` (Enum-as-String) bị loại khỏi bộ `sort` — `ORDER BY level` cho ra thứ tự alphabet (`ADVANCED, BEGINNER, INTERMEDIATE`), không phải thứ tự cấp độ thật. Chỉ giữ `newest`/`popular`/`title`.
- Mọi nhánh `ORDER BY` đều có tie-break `id` để đảm bảo phân trang ổn định qua nhiều lần gọi.
- `keyword` giới hạn 50 ký tự, escape ký tự đặc biệt của `LIKE` (`%`, `_`, `\`) theo đúng thứ tự (escape `\` trước để không escape đôi).
- Bug build phát sinh do thêm `JpaSpecificationExecutor` vào `TopicRepository`: method `delete(any())` trong test cũ trở nên "ambiguous" (khớp cả `delete(T)` lẫn `delete(DeleteSpecification<T>)`) — sửa bằng `any(Topic.class)`.

### M11 — Vocabulary & SRS: **HOÀN THÀNH 29/09/2026**, có 4 việc bổ sung ngoài phạm vi Requirements gốc

3 endpoint gốc theo FR-8 (`save`/`review`/`today`) cộng thêm 4 việc phát sinh khi FE test thực tế phát hiện thiếu (xem chi tiết ở phần Frontend, mục 5, và ở Requirements FR-8): `saved-ids` (hiện trạng thái nút Save khi quay lại trang), `GET /api/vocabulary` (xem toàn bộ sổ từ, không giới hạn theo hạn ôn), `DELETE /api/vocabulary/{id}` (xoá từ khỏi sổ). Migration V9 (bảng `user_vocabulary`, `UNIQUE(user_id, flashcard_id)`, index `(user_id, next_review_at)`). `VocabularyService` — `saveWord()`/`review()`/`getToday()`/`getAllSaved()`/`getSavedFlashcardIds()`/`deleteSavedWord()`, đều lấy user qua `SecurityContextHolder` (không tin `userId` từ client), ownership qua `findByIdAndUser_Id` trả 404 đồng nhất cho "không tồn tại" và "của người khác". Bean `Clock` (`TimeConfig`) inject vào Service để `applyReview()` (static, pure function) unit test được với thời gian cố định qua từng bậc SRS. Bộ Postman đầy đủ (nhóm A-G theo `save`/`review`/`today`/retrofit xoá/hồi quy, cộng nhóm test riêng cho 3 endpoint bổ sung) đều PASSED, xác nhận cả bằng dữ liệu thật (chuỗi 5 lần "đã nhớ" liên tiếp đạt đúng bậc 4/30 ngày/status KNOWN).

**Quyết định chốt tại M11:**
- Thuật toán SRS: thang cố định 5 bậc `intervalLevel` (0-4) = 1/3/7/14/30 ngày, không dùng SM-2. Nhớ: tăng 1 bậc, `nextReviewAt` theo bậc hiện tại trước khi tăng, `status=KNOWN` khi đạt bậc 4. Quên: reset về bậc 0, `difficulty++`, hẹn lại sau 1 ngày.
- FR-8.5 (ưu tiên từ hay quên) hiện thực bằng `ORDER BY difficulty DESC, nextReviewAt ASC, id ASC` ở `getToday()`, không thêm cơ chế riêng.
- Lưu trùng trả `409` (check `existsBy` trước + bắt `DataIntegrityViolationException` theo tên constraint để chặn race condition double-click).
- Retrofit rule "không cascade xoá lịch sử cá nhân" (đã có ở Requirements mục 2.3 từ M1): cả `FlashcardService.deleteFlashcard()` và `TopicService.deleteTopic()` phải thêm check `UserVocabularyRepository`, trả `409` nếu đã có User lưu từ liên quan — dễ bỏ sót nhất milestone, vì đây là 2 Service cũ từ M2/M3, không phải code mới viết trong M11.
- `GlobalExceptionHandler` vá 2 lỗ hổng cũ (từ M1-M7, chỉ lộ ra ở M11): `HttpMessageNotReadableException` (body JSON sai kiểu) và `MethodArgumentTypeMismatchException` (path variable sai kiểu) trước đó rơi vào handler `Exception` chung, trả nhầm 500 thay vì 400.

**Bug/sự cố đã xử lý trong lúc làm M11 (đáng nhớ cho các milestone sau):**
- Test cũ (`TopicServiceTest`, `FlashcardServiceTest`) vỡ `NullPointerException` khi thêm tham số constructor mới vào Service (`UserVocabularyRepository`) mà quên thêm `@Mock` tương ứng — Mockito lặng lẽ truyền `null`, không báo lỗi biên dịch, chỉ vỡ lúc chạy đúng nhánh code mới. Phải rà lại toàn bộ test `@InjectMocks` của Service đó mỗi khi đổi constructor.
- `UnnecessaryStubbingException` khi stub 1 lời gọi nhưng nhánh code throw exception SỚM HƠN (trước khi chạm tới lời gọi đó) — chỉ stub đúng những gì nhánh test đó thực sự chạy tới.
- `VocabularyServiceTest` ban đầu mock sai cách lấy user (`authentication.getPrincipal()` + `userRepository.findById()`) trong khi code thật dùng `authentication.getName()` + `userRepository.findByUsername()` — mock không khớp implementation thật khiến test luôn đi vào nhánh lỗi sai (throw "User not found" thay vì nhánh đang muốn test), phải đối chiếu đúng luồng code thật trước khi viết mock, không đoán theo tên biến.
- FE `vocabulary.html`: quên hẳn đoạn gắn `addEventListener('click', ...)` để lật thẻ khi tạo trang mới dựa theo `topic-detail.html` — lỗi "thiếu code" chứ không phải bug logic, class CSS `flipped` không bao giờ được toggle nên không lật được; kiểm tra qua DevTools Elements panel (class có đổi khi bấm hay không) là cách nhanh nhất phân biệt lỗi JS (thiếu listener) với lỗi CSS (style bị đè).
- FE `vocabulary.html`: mất nút phân trang dù `totalPages > 1` — nguyên nhân là 1 vòng lặp dọn dẹp UI khi chuyển tab (`switchTab`) thêm nhầm class `hidden` vào chính `#allWordsPagination` (div chứa nút), trong khi hàm render nút chỉ đổ `innerHTML` chứ không tự gỡ `hidden` khỏi div cha — nút được tạo ra thật nhưng vô hình vì cha đang `display: none`.
- FE `vocabulary.html`: sau khi thêm tính năng xoá từ ở tab "All Words", tab "Due Today" có thể vẫn hiện 1 card đã bị xoá dưới DB (2 tab giữ state riêng trong bộ nhớ trình duyệt, không tự đồng bộ) — vá bằng cách chủ động loại phần tử khỏi mảng `queue` trong bộ nhớ ngay khi xoá thành công, nếu id trùng khớp; đây là giới hạn đã biết của cách quản lý state hiện tại (không phải single source of truth thật sự), ghi vào Requirements mục 10.9.

### M12 — Dictation: **HOÀN THÀNH 03/10/2026**, có 3 endpoint + nhiều việc bổ sung ngoài phạm vi Requirements gốc

9 endpoint (6 gốc theo FR-7 + 3 bổ sung: `GET /api/admin/topics/{id}/dictation`, `POST /api/admin/dictation/{id}/audio`, `GET /api/dictation/lessons`). Migration V10 (`dictation_lessons` cascade theo Topic, `dictation_results` không cascade, index `(user_id, lesson_id, created_at DESC)` và `lesson_id`). `DictationComparator` (static pure function, không Spring): chuẩn hoá + LCS mức từ → nhãn `CORRECT/WRONG/MISSING/EXTRA` + accuracy. `FileStorageService.storeAudio` (magic bytes, 10MB riêng). Retrofit `TopicService.deleteTopic()` (409 khi có kết quả, dọn file audio). `GlobalExceptionHandler` thêm 405/415/400-multipart. Danh mục bài dùng `Specification` + một truy vấn thống kê `GROUP BY` (log SQL xác nhận không N+1: 1 `select` bài đã `join` Topic + 1 truy vấn thống kê; câu `count` chỉ chạy khi trang đầy). Bộ Postman đều PASSED: 7 nhóm A–G (tạo/đọc, upload audio, làm bài, cô lập 2 user, sửa + validate, quy tắc xoá + dọn file trên đĩa, phân quyền) và 20 case danh mục P1–P20 (riêng B1 với file WAV tự sinh bị từ chối — do script tạo file sai header, backend từ chối đúng). Xác nhận cả trên đĩa: `uploads/dictation` chỉ còn đúng file của Lesson hiện tại sau khi thay audio, xoá Lesson, xoá Topic.

**Quyết định chốt tại M12:** (chi tiết đầy đủ ở Requirements FR-7 và mục 8)
- Audio **upload file** thay vì URL external; `mediaUrl` nullable, User chỉ thấy bài đã có audio; response phía User **không bao giờ có transcript** trước khi nộp.
- `accuracy = round1(100 × đúng / max(n_transcript, n_user))`; giới hạn 1000 từ mỗi bên (bảng LCS O(n×m)); `userInput` lưu nguyên văn.
- `GET /api/topics/{id}/dictation` cần JWT (không `permitAll` như các `GET /api/topics/*`), không cache.
- Danh mục: `progress` (`NEW`/`PRACTICED`) và số liệu `attempts/bestAccuracy/lastAttemptAt` tính theo **người dùng hiện tại**; `sort=recent` = bài vừa luyện lên đầu, bài chưa làm xuống cuối.
- Create/update/upload của Admin trả 200 (giống nhóm Admin hiện có), `submit` 201.

**Bẫy/bug đáng nhớ cho các milestone sau:** (3 bẫy đầu cũng đã ghi ở Requirements mục 1.4)
- **`Specification` + `Pageable` có `Sort` → Sort ghi đè `orderBy` của Specification.** Phải dùng `PageRequest.of(page, size)` không kèm Sort (có test `ArgumentCaptor<Pageable>` khoá lại).
- **PostgreSQL xếp `NULL` lên đầu khi `ORDER BY ... DESC`** → `sort=recent` thêm khoá `CASE WHEN EXISTS(...) THEN 0 ELSE 1 END` đứng trước, nếu không bài chưa làm nằm trên cùng.
- **DB cascade chỉ xoá record, không xoá file** → `deleteTopic()` phải đọc danh sách `mediaUrl` TRƯỚC khi xoá (sau khi xoá thì Lesson đã biến mất theo cascade, danh sách rỗng) rồi publish `FileDeletionEvent` cho từng file; `uploadAudio` phải tìm Lesson trước khi gọi `storeAudio` (nếu không, Lesson không tồn tại vẫn sinh file mồ côi).
- Mockito: `when(...).thenReturn(stats(...))` mà `stats()` bên trong lại gọi `when()` → `UnfinishedStubbingException` (stub lồng nhau). Tạo đối tượng mock ra biến riêng trước, rồi mới stub.
- `@InjectMocks` + thêm 2 dependency vào `TopicService` → phải thêm `@Mock` tương ứng ở `TopicServiceTest`, nếu không Mockito lặng lẽ truyền `null` (cùng bẫy đã gặp ở M11).
- Constructor `FileStorageServiceImpl` có 2 phiên bản (3 và 4 tham số): phải gắn `@Autowired` vào bản 4 tham số (thiếu thì Spring báo "No default constructor found"); giữ bản 3 tham số để `FileStorageServiceImplTest` cũ không phải sửa.
- Script PowerShell tự sinh file WAV thử nghiệm sai (mảng byte do scriptblock trả về bị "trải" nên các chuỗi ASCII `RIFF`/`WAVE`/`fmt `/`data` không vào file) → backend từ chối file là ĐÚNG; kiểm tra file bằng `Format-Hex -Path ... -Count 12` trước khi nghi ngờ backend.
- Số test: so từng class (`Tests run:` mỗi class trong log) thay vì cộng nhẩm — một lần cộng nhẩm sai làm mất công đi tìm 2 test "bị thiếu" không có thật.

**Frontend (xem mục 5):** `dictation.html`, tab Dictation + Working topic + tìm kiếm/lọc/phân trang ở `admin.html`, `ui.js`, `topicPicker.js`, sidebar cho khách, `redirectTo`. Lỗi đáng nhớ: `truncate()` ném `TypeError` khi `Topic.description` là `null` (cột cho phép NULL) làm cả trang Admin báo "Failed to load topics", và `catch` chỉ hiện câu cố định nên không biết nguyên nhân — nay `catch` luôn `console.error` + hiện `error.message`; guard `requestId` lần đầu quên khai báo `const requestId = ++counter` và thiếu `return` trong nhánh `catch`.

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

## 7. Trạng thái hiện tại — M1–M12 hoàn thành

Toàn bộ lộ trình trong Requirements (MVP M1–M9 + v1.1 M10–M12) đã xong. Backend + Frontend nối thật, redesign UI hoàn chỉnh, 198 test PASSED.

**Việc còn treo, cố ý để dành sau (không phải nợ/sai sót):**
- **Rủi ro hỏng dữ liệu, nên xử lý sớm nhất:** modal "Manage Questions" của `admin.html` (từ M3) dựng bằng `innerHTML` và nhét giá trị đáp án vào `value="..."` — đáp án chứa dấu `"` bị cắt khi mở lại để sửa, bấm Save sẽ ghi đè bản đã cắt. `vocabulary.html` cũng còn dựng bằng `innerHTML`. Chi tiết ở Requirements mục 10.9.
- `403` của `/api/admin/**` chưa có body `message` (thiếu `accessDeniedHandler` trong `SecurityConfig`) — FE chỉ hiện `HTTP 403`.
- Dictation: `<audio>` không gửi được header `ngrok-skip-browser-warning` (chưa kiểm chứng riêng với trình duyệt ẩn danh/phiên mới qua ngrok); `.m4a` chưa kiểm chứng trên Safari; file mồ côi hiếm gặp nếu transaction rollback sau khi `storeAudio` ghi file.
- Accessibility nâng cao mới áp dụng đầy đủ cho `index.html` qua Impeccable (các component mới `createSelect`/`topicPicker` đã có ARIA + bàn phím) — các trang còn lại (kể cả `vocabulary.html`, `dictation.html`) chưa rà theo cùng chuẩn.
- Giới hạn đã biết ở `vocabulary.html`: state 2 tab ("Due Today"/"All Words") giữ riêng trong bộ nhớ trình duyệt, chỉ đồng bộ thủ công cho đúng 1 trường hợp đã phát hiện (xoá từ đang nằm trong hàng đợi Due), không phải cơ chế đồng bộ tổng quát.
- Dictation: dấu câu bị xoá chứ không thành khoảng trắng (`hello,world` → `helloworld`); số viết bằng chữ số khác số viết bằng chữ (`5` ≠ `five`).

**Việc tiếp theo:** chọn từ backlog trên hoặc mục 9 của Requirements (định hướng mở rộng dài hạn v2+: Gamification, Notification, Exam, Shadowing, Voice Chat...). Chưa có milestone nào được lên kế hoạch.

---

## 8. Khi bắt đầu cuộc trò chuyện mới — cần gửi kèm tài liệu gì?

**Bắt buộc:**
1. File tóm tắt này (`Session_Summary.md`)
2. `Requirements.md` (v1.8) (Requirements đầy đủ — mục 10 là Frontend Design System, bắt buộc đọc trước khi code thêm trang mới)

**Tuỳ chọn, nên gửi nếu cần tra nhanh API:** `FE_Handoff_Brief.md` (đã cập nhật lại tới hết M12, xem mục 5) — vẫn không bắt buộc như 2 file trên, dùng khi muốn tra contract nhanh mà không đọc lại toàn bộ Requirements; Swagger UI (`/swagger-ui.html`) là nguồn xác nhận cao nhất khi có sai lệch.

**Nếu đang code dở:** file `.java`/`.html`/`.js` đang dở + file nó phụ thuộc trực tiếp.

**Câu mở đầu gợi ý cho cuộc trò chuyện mới:**
> "Đây là tóm tắt project mình đang làm (đính kèm), M1–M12 đã xong (MVP đầy đủ + toàn bộ v1.1: Search/Filter/Pagination cho Topic, Vocabulary/SRS, Dictation). Giờ mình muốn làm [việc tiếp theo — một mục trong backlog ở mục 7, hoặc một hạng mục ở mục 9 của Requirements]. Mình sẽ gửi từng tài liệu một, bạn đọc kỹ rồi xác nhận đã hiểu giúp mình nhé."
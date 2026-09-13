# TÀI LIỆU YÊU CẦU (REQUIREMENTS)
# Project 1 — Nền tảng học tiếng Anh tích hợp AI
*(AI English Learning Platform — trước đây gọi là "Chatbot Học Tiếng Anh"; đổi tên vì phạm vi đã lớn hơn 1 chatbot đơn thuần: gồm AI Chat, Topic/Flashcard, Quiz, Progress, Vocabulary/SRS, Dictation, Search)*

**Phiên bản:** 1.5
**Ngày tạo:** 28/08/2026 — cập nhật lần 5 (13/09/2026, hoàn thành M8 Unit Test)
**Trạng thái:** Đã chốt thiết kế, sẵn sàng bắt đầu code

---

## 1. TỔNG QUAN DỰ ÁN

### 1.1. Mục tiêu
Xây dựng backend Spring Boot cho một ứng dụng học tiếng Anh, cho phép người dùng:
- Trò chuyện tự do với AI để luyện phản xạ và được sửa lỗi ngữ pháp
- Học theo các chủ đề có cấu trúc (từ vựng, quiz)
- Theo dõi tiến độ học tập cá nhân

Đây là project thực hành đầu tiên sau khi hoàn thành 24 bài lý thuyết (Phase 1–6), nhằm áp dụng toàn bộ kiến thức đã học vào một sản phẩm thật, đặc biệt là các kỹ thuật ở Phase 6: **File Upload, WebSocket & Async, Tích hợp API bên thứ 3, Caching (Redis)**.

### 1.2. Đối tượng sử dụng (Role)
| Role | Mô tả |
|---|---|
| `USER` | Người học: chat với AI, học bài, làm quiz, xem tiến độ cá nhân |
| `ADMIN` | Quản trị nội dung: CRUD chủ đề học, flashcard, quiz |

### 1.3. Công nghệ sử dụng

| Thành phần | Lựa chọn | Ghi chú |
|---|---|---|
| Backend | Java 21, Spring Boot 4.1.1 | Cập nhật 01/09/2026 — khác bản dự kiến ban đầu (Java 17+/Spring Boot 3.x); kéo theo hệ quả Jackson 3 thay vì Jackson 2, xem ghi chú mục 1.4 |
| Bảo mật | Spring Security + JWT | Phân quyền USER/ADMIN |
| Database | PostgreSQL | Qua Spring Data JPA, quản lý schema bằng **Flyway migration** (không dùng `ddl-auto: update`) để kiểm soát lịch sử thay đổi schema rõ ràng |
| Cache | Redis | Cache Topic/Flashcard, cân nhắc cache câu trả lời AI lặp lại |
| Realtime | WebSocket (STOMP) | Chat AI trả lời dạng stream |
| AI Provider | xKiro (`api.xkiro.com/v1`, chuẩn OpenAI-compatible) | Dùng `WebClient`/`RestClient` |
| Upload file | Lưu local filesystem (giai đoạn đầu), có thể nâng cấp cloud storage sau | Ảnh flashcard, avatar |
| Frontend | HTML/CSS/JavaScript thuần + Tailwind CSS (qua CDN) | Gọi REST API bằng `fetch`, WebSocket bằng `SockJS + STOMP` client. **Sửa 05/09/2026:** đổi từ Bootstrap (dự kiến ban đầu) sang Tailwind — cần đồng bộ lại `FE_Handoff_Brief.md` mục 1 cho khớp |
| Testing | JUnit 5 + Mockito | Theo đúng Phase 5 đã học |

### 1.4. Ràng buộc & nguyên tắc làm việc
- Ưu tiên có **scaffold (khung sườn code)** trước khi người học tự code phần logic chi tiết
- Khi phát sinh lỗi cú pháp/chính tả, dừng lại rà soát kỹ trước khi chốt code (điểm yếu đã ghi nhận)
- Khi giải thích kỹ thuật, luôn đi tới cơ chế cụ thể tầng dưới, không dừng ở kết luận chung chung
- Cẩn trọng khi áp dụng lại 1 rule cũ (VD: cache side-effect vs quyết định dùng Async — 2 tiêu chí độc lập, đã từng nhầm)
- Mọi Enum trong Entity dùng `@Enumerated(EnumType.STRING)`, không dùng mặc định (ORDINAL) — tránh lỗi âm thầm khi thêm giá trị enum mới về sau làm lệch dữ liệu cũ
- Lưu ý: Spring Boot 4 mặc định dùng **Jackson 3** (package `tools.jackson.databind`, bean `JsonMapper`), KHÔNG phải Jackson 2 (`com.fasterxml.jackson.databind.ObjectMapper`) như quen thuộc — phát hiện khi code `AiClientImpl` ở M4. Cần nhớ khi làm việc với JSON serialize/deserialize thủ công ở các milestone sau.
- Lưu ý: `SecurityConfig` mặc định trả 403 thay vì 401 khi thiếu/sai JWT (do chưa custom `AuthenticationEntryPoint`) — đã fix ở M4 bằng `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)`, áp dụng cho toàn bộ API từ M4 trở đi.
- Lưu ý: phát hiện ở M7 — thiếu `.requestMatchers("/error").permitAll()` trong `SecurityConfig` khiến MỌI lỗi 403 (`AccessDeniedException`, VD: USER hợp lệ gọi route `hasRole("ADMIN")`) bị biến thành 401 rỗng. Nguyên nhân: Spring Boot tự forward nội bộ sang `/error` để render error body, request forward này đi lại qua Security Filter Chain; `JwtAuthenticationFilter` (kế thừa `OncePerRequestFilter`) mặc định bỏ qua dispatch `ERROR` (không set lại `SecurityContext`) nên `/error` rơi vào trạng thái anonymous, bị chặn tiếp bởi `anyRequest().authenticated()`, rồi `AuthenticationEntryPoint` ghi đè 403 thành 401. Bug tồn tại từ M4 (khi thêm `hasRole("ADMIN")`) nhưng chưa từng lộ ra vì chưa có test nào dùng token USER hợp lệ gọi route ADMIN cho tới M7. Đã fix bằng cách thêm `/error` vào danh sách `permitAll()`.
- Lưu ý: phát hiện ở M8 khi viết Unit Test — 3 lỗi kỹ thuật lặp lại nhiều lần, đáng nhớ cho các milestone sau: (1) mock 1 method trả về object thường (không phải `Optional`) mà quên stub sẽ mặc định trả `null` (khác `Optional` được Mockito tự trả `Optional.empty()`), dễ gây `NullPointerException` ở bất kỳ chỗ nào service đọc lại giá trị từ `save()`; (2) `ArgumentCaptor.capture()` khớp với **mọi** lời gọi bất kể kiểu tham số thật (khác `any(SomeClass.class)` có kiểm tra kiểu runtime) — nếu 1 method bị gọi nhiều lần với các loại Event/tham số khác nhau trong cùng 1 lần thực thi, dùng `capture()` với `verify(times(1))` mặc định sẽ gây lỗi `TooManyActualInvocations`; cách đúng là `verify(times(n))` rồi tự lọc lại theo `instanceof`; (3) khi input được validate theo kiểu exact-match (VD: `options.contains(rawAnswer)`) TRƯỚC, rồi mới tới bước so sánh case-insensitive/trim, thì input có khoảng trắng thừa hoặc sai case sẽ bị chặn ngay ở bước validate — không bao giờ chạm tới được bước so khớp linh hoạt hơn phía sau.

---

## 2. YÊU CẦU CHỨC NĂNG (FUNCTIONAL REQUIREMENTS)

### FR-1: Authentication & Authorization `[MVP]`
- FR-1.1: Đăng ký tài khoản (email, username, password) — mặc định role `USER`
- FR-1.2: Đăng nhập, trả về JWT access token
- FR-1.3: Middleware/filter xác thực JWT cho mọi API cần bảo vệ
- FR-1.4: Phân quyền endpoint theo role (`/api/admin/**` chỉ ADMIN)
- FR-1.5: Mã hóa mật khẩu bằng BCrypt

### FR-2: Chat tự do với AI `[MVP]`
- FR-2.1: Tạo mới một `Conversation`
- FR-2.2: Gửi tin nhắn tới AI, nhận phản hồi có: câu trả lời tự nhiên + phần sửa lỗi ngữ pháp (nếu có lỗi) + giải thích ngắn gọn
- FR-2.3: Lưu toàn bộ `Message` (cả của user và AI) vào từng `Conversation`
- FR-2.4: Lấy lịch sử hội thoại theo `conversationId`
- FR-2.5: Phản hồi AI được đẩy real-time qua WebSocket (stream từng phần, hiệu ứng "đang gõ"). `Message` với `sender = AI` chỉ được **persist vào DB sau khi stream hoàn tất thành công** — nội dung lưu là toàn bộ response đã ghép đầy đủ từ các chunk. Nếu kết nối WebSocket bị ngắt giữa chừng, không coi phần đã stream dở là 1 AI response hoàn chỉnh và không lưu. **ĐÃ IMPLEMENT VÀ TEST XONG 08/09/2026:** vì AI vẫn phải trả `correction`/`explanation` dạng JSON (không chỉ `reply` thuần), không thể stream JSON dở dang an toàn ra Client — giải pháp: dùng 1 delimiter cố định (`\n---META---\n`) để tách phần `reply` (plain text, stream trực tiếp từng chunk ra Client ngay) khỏi phần JSON meta (`correction`/`explanation`, chỉ gom và parse sau khi stream kết thúc). Chi tiết đầy đủ xem mục 8.
- FR-2.6: Xử lý lỗi khi gọi AI provider:
  - Có connect timeout và response/read timeout riêng biệt, không để request treo vô thời hạn
  - Timeout/lỗi từ provider phải được chuyển thành lỗi ứng dụng rõ ràng (không để lộ raw exception/stack trace ra response)
  - Không retry vô điều kiện — chỉ retry với các lỗi tạm thời phù hợp (VD: timeout, 5xx), không retry lỗi do input sai (4xx)
  - *(Backlog — chưa cần ở MVP)* Rate limiting/quota cho AI sẽ được cân nhắc ở giai đoạn hardening
  - `Message` của User được lưu vào `Conversation` ngay khi nhận thành công, **không phụ thuộc** vào việc AI trả lời được hay không. Nếu AI Provider thất bại (timeout/lỗi), **không tạo một `Message` giả với `sender = AI`** như thể AI đã trả lời thành công — thay vào đó trả lỗi rõ ràng cho client để hiển thị trạng thái "gửi thất bại", User có thể thử lại
- FR-2.7: User chỉ được phép xem/gửi message trong `Conversation` thuộc về chính mình. **Quy tắc ownership áp dụng cho cả REST API và WebSocket** — không chỉ REST:
  - WebSocket connection phải được authenticate (JWT) trước khi cho phép send/subscribe
  - Backend phải kiểm tra ownership của `Conversation` trước khi xử lý bất kỳ message nào gửi qua WebSocket, tương tự cách REST đang làm — không cho subscribe vào conversation không thuộc quyền sở hữu
- FR-2.8: Khi gửi tin nhắn mới tới AI, backend phải đính kèm một phần lịch sử hội thoại gần nhất (`Message` trước đó trong cùng `Conversation`) làm context, để AI trả lời có mạch lạc/nhớ ngữ cảnh thay vì coi mỗi tin nhắn độc lập. Số lượng message đính kèm (VD: N tin gần nhất) sẽ quyết định cụ thể khi implement M4, cân bằng giữa chất lượng trả lời và chi phí token

### FR-3: Module học theo chủ đề (Topic & Flashcard) `[MVP]`
- FR-3.1 (Admin): Tạo/sửa/xóa `Topic` (tiêu đề, mô tả, cấp độ)
- FR-3.2 (Admin): Tạo/sửa/xóa `Flashcard` thuộc 1 Topic (từ, nghĩa, ví dụ, ảnh minh họa, audio)
- FR-3.3 (Admin): Upload ảnh minh họa cho Flashcard/Topic
- FR-3.4 (User): Xem danh sách Topic, xem chi tiết Flashcard theo Topic
- FR-3.5: Danh sách Topic/Flashcard được cache bằng Redis, invalidate cache khi Admin **C**reate/**U**pdate/**D**elete Topic hoặc Flashcard. Quy ước cache:
  - Cache key phải bao gồm toàn bộ tham số ảnh hưởng tới kết quả (VD dự kiến cho M10 khi có đủ search/filter/sort: `topic:list:{keyword}:{level}:{page}:{size}:{sort}`)
  - **Cache key thực tế đã code ở M5** (vì API `GET /api/topics` hiện chỉ có `page`/`size`, chưa có search/filter/sort — để dành M10): `topics::{size}` (chỉ cache khi `page = 0`), `topicDetails::{id}`, `flashcardsByTopic::{topicId}`. Sẽ mở rộng đúng theo key dự kiến ở trên khi M10 thêm search/filter/sort.
  - Khi dữ liệu nguồn thay đổi (Admin CUD Topic/Flashcard), invalidate các cache liên quan
  - Không bắt buộc cache mọi biến thể search/filter/pagination — chỉ cache các query có lợi ích rõ ràng (VD: trang đầu, không filter), tránh biến Redis thành hàng nghìn cache key khó kiểm soát khi có FR-9 (search/filter)
  - **Dependency Topic ↔ Flashcard — ĐÃ CHỐT tại M5:** `TopicResponse` hiện **không** có field tổng hợp từ Flashcard (không có `flashcardCount` hay tương tự), nên cập nhật Flashcard **không** cần invalidate chéo cache Topic — 2 cache (`topics`/`topicDetails` và `flashcardsByTopic`) hoàn toàn độc lập. Nếu sau này `TopicResponse` được bổ sung field tổng hợp từ Flashcard, phải quay lại bổ sung evict chéo ở `FlashcardService` (evict thêm cache `topics`/`topicDetails` khi Flashcard CUD).

### FR-4: Quiz & Đánh giá `[MVP]`
- FR-4.1 (Admin): Tạo `Quiz` + `QuizQuestion` (trắc nghiệm) thuộc 1 Topic
- FR-4.2 (User): Lấy đề quiz theo Topic
- FR-4.3 (User): Nộp bài, hệ thống tự chấm điểm. Validation bắt buộc khi submit:
  - Chỉ chấp nhận `questionId` thuộc đúng Quiz đang submit — từ chối nếu `questionId` thuộc Quiz khác
  - Không chấp nhận `questionId` bị trùng lặp trong 1 lần submit
  - `answer` gửi lên phải nằm trong tập `options` hợp lệ của câu hỏi đó
  - Validate cấu trúc bài submit (đủ định dạng JSON, đúng kiểu dữ liệu) trước khi tiến hành chấm điểm
  - **Không bao giờ dùng dữ liệu do client gửi để xác định đáp án đúng** — `correctAnswer` luôn lấy từ database tại thời điểm chấm, không tin bất kỳ trường nào client tự khai là "đúng/sai"
- FR-4.4: Mỗi lần nộp bài lưu 1 bản ghi `QuizAttempt` mới (không ghi đè lên lần trước — giữ đầy đủ *các lần làm*, nhưng mỗi bản ghi chỉ chứa kết quả tổng quan, xem chi tiết phạm vi ở mục Entity) — sau đó cập nhật `UserProgress` tương ứng (trạng thái tổng quan hiện tại của Topic đó)
- FR-4.5: `QuizQuestion.correctAnswer` **không bao giờ được trả về cho User** trong bất kỳ API GET nào (lấy đề quiz, xem lại câu hỏi...) — chỉ được Service dùng nội bộ khi chấm điểm ở backend. Bắt buộc dùng DTO riêng (VD: `QuizQuestionPublicDTO`) ẩn hẳn trường này, không chỉ ẩn ở tầng serialize

### FR-5: Theo dõi tiến độ `[MVP]`
- FR-5.1: User xem được tiến độ học của mình: `UserProgress` theo từng Topic (trạng thái hiện tại), và có thể xem lại lịch sử các lần làm quiz qua `QuizAttempt`
- FR-5.2: User chỉ được xem tiến độ/lịch sử của chính mình — không cho truyền `userId` để xem của người khác (áp dụng cùng nguyên tắc FR-2.7)
- FR-5.3: (Mở rộng, không bắt buộc) Thống kê tổng quan: số từ đã học, số quiz đã làm
- FR-5.4: Công thức tính `UserProgress.progressPercent`. **ĐÃ CHỐT 03/09/2026 (M3):** xem công thức đầy đủ ở mục 8.

### FR-6: Quản lý file upload `[MVP]`
- FR-6.1: Upload ảnh (Flashcard, avatar), quy tắc bắt buộc:
  - Chỉ chấp nhận MIME type được phép (`image/jpeg`, `image/png`) — kiểm tra nội dung file, không chỉ tin vào extension
  - Giới hạn dung lượng file
  - Không tin tưởng filename/extension do client gửi lên
  - Filename lưu trên server phải được sinh lại (VD: UUID), không dùng nguyên filename gốc từ client
  - Không cho phép path traversal (chặn ký tự `../` hoặc đường dẫn lạ trong tên file)
  - File được lưu ở vị trí tách khỏi source code/application configuration
- FR-6.2: Trả về URL truy cập file sau khi upload thành công
- FR-6.3: Validate và xử lý lỗi file không hợp lệ
- **ĐÃ CHỐT 10/09/2026 (M7)** — chi tiết implement:
  - Vị trí lưu: `app.upload.dir` (config, ngoài classpath), chia subfolder theo loại (`topics/`, `flashcards/`, `avatars/`)
  - Giới hạn dung lượng: 5MB/file, chặn ở 2 lớp độc lập — `spring.servlet.multipart.max-file-size` (Spring tự chặn trước) và tự check lại thủ công trong `FileStorageService` (không tin 1 lớp validate duy nhất)
  - Kiểm tra nội dung file thật: dùng `javax.imageio.ImageIO` đọc thử ảnh (`ImageReader.read()`) — nếu không đọc được hoặc `formatName` không thuộc `jpeg`/`png`, từ chối dù `Content-Type` header client khai đúng. Không dùng Apache Tika (không cần thiết ở quy mô hiện tại)
  - Filename: `UUID.randomUUID()` + extension xác định từ **format ảnh thật đã đọc được** (`ImageReader.getFormatName()`), không dựa vào Content-Type header hay tên file gốc — do đó path traversal bị loại trừ hoàn toàn bằng thiết kế (UUID không thể chứa `../`)
  - Serve file: `WebMvcConfig` map `/uploads/**` ra thư mục thật qua `addResourceHandlers`, route này `permitAll()` trong `SecurityConfig` (ảnh cần xem được qua thẻ `<img>` không cần đính JWT)
  - File cũ bị ghi đè khi upload ảnh mới, hoặc khi xoá hẳn Topic/Flashcard/User, đều được dọn khỏi đĩa qua `FileDeletionEvent`/`FileDeletionListener` (`@TransactionalEventListener(AFTER_COMMIT)`, cùng pattern cache eviction đã chốt ở M5) — tránh race condition xoá file trước khi transaction DB thực sự commit
  - **Chỉ hỗ trợ đúng JPEG/PNG, không mở rộng thêm GIF/BMP/WBMP** dù `ImageIO` hỗ trợ sẵn — quyết định có chủ đích: các decoder ít phổ biến hơn (đặc biệt BMP/WBMP) có bề mặt tấn công lớn hơn JPEG/PNG khi xử lý file dị dạng, trong khi nhu cầu sản phẩm (ảnh minh hoạ tĩnh cho Flashcard/Topic/Avatar) không cần các format này
  - **Bug phát sinh sau khi chốt, đã fix:** `dto/auth/UserResponse.java` (dùng chung cho login/register/avatar upload) ban đầu không có field `avatarUrl` — khiến `POST /api/users/me/avatar` trả về 200 kèm file lưu đúng trên đĩa, nhưng response không cho biết URL ảnh vừa upload, vi phạm trực tiếp FR-6.2. Đã thêm `avatarUrl` vào `UserResponse`, nên giờ response của login/register cũng có thêm field này (giá trị `null` nếu user chưa từng upload avatar)

---

## 2.1. MỞ RỘNG SAU MVP `[v1.1]` (Milestone M10–M12)

Sau khi hoàn thành MVP (M1–M9), tham khảo thêm sản phẩm Parroto, đã chọn 3 nhóm tính năng **ưu tiên làm trước** vì giá trị học Backend cao và độ phức tạp vừa sức để tiếp cận ngay sau MVP. Toàn bộ các nhóm còn lại (Shadowing, Voice Chat, Exam đầy đủ, YouTube Generator, Notification, Gamification...) **vẫn nằm trong kế hoạch**, được ghi ở mục 9 — Định hướng mở rộng dài hạn, làm sau khi nền tảng đã vững.

### FR-7: Dictation (rút gọn) `[v1.1]`
Người học nghe audio rồi gõ lại câu nghe được, hệ thống so sánh với transcript và chỉ ra lỗi.
- FR-7.1: Admin tạo `DictationLesson` thuộc 1 Topic (có audio, transcript, level)
- FR-7.2: User nghe lesson (audio của `DictationLesson`)
- FR-7.3: User nhập nội dung nghe được
- FR-7.4: Hệ thống so sánh input với transcript, tính accuracy (%)
- FR-7.5: Highlight từ đúng/sai/thiếu/thừa trong kết quả trả về
- FR-7.6: Cho phép nghe lại, không giới hạn
- FR-7.7: Lưu kết quả từng lần làm vào `DictationResult`

*Không làm trong v1.1: điều chỉnh tốc độ audio, AI giải thích lỗi chi tiết — để lại backlog.*

### FR-8: Vocabulary & SRS (Spaced Repetition) `[v1.1]`
Nâng cấp Flashcard hiện có thành hệ thống ôn từ vựng cá nhân hóa.
- FR-8.1: User lưu 1 Flashcard vào từ vựng cá nhân (`UserVocabulary`)
- FR-8.2: User đánh dấu "đã nhớ" / "chưa nhớ" sau mỗi lần ôn
- FR-8.3: Hệ thống tính `nextReviewAt` theo thuật toán SRS đơn giản. **Thuật toán cụ thể sẽ được quyết định khi implement M11** (không commit SM-2 hay bất kỳ thuật toán chuẩn nào ngay trong Requirements — có thể tự thiết kế kiểu khoảng cách tăng dần 1/3/7/14/30 ngày, đơn giản hơn và vẫn đủ để hiểu bản chất SRS)
- FR-8.4: API trả về danh sách từ cần ôn hôm nay (`nextReviewAt <= now`)
- FR-8.5: Từ bị đánh "chưa nhớ" nhiều lần được ưu tiên xuất hiện lại sớm hơn

### FR-9: Search & Filter & Pagination `[v1.1]`
Áp dụng cho danh sách Topic/Flashcard khi dữ liệu lớn dần. Giữ đúng 4 tiêu chí này, chưa cần thêm filter khác (skill/difficulty/duration...) vì dữ liệu hiện tại chưa đủ lớn để các filter đó có ý nghĩa thực tế.
- FR-9.1: Search Topic theo từ khóa (title/description)
- FR-9.2: Filter theo `level`
- FR-9.3: Pagination áp dụng cho các API danh sách có khả năng tăng lớn; danh sách nhỏ có thể không cần pagination
- FR-9.4: Sort theo mới nhất / phổ biến (dựa trên lượt học)

**Phạm vi áp dụng cụ thể — tránh nhầm lẫn đâu cần search đầy đủ, đâu chỉ cần phân trang:**
- **Search + Filter + Sort + Pagination đầy đủ:** chỉ `GET /api/topics` (đây là danh sách chính người dùng duyệt/tìm, xứng đáng đầu tư đầy đủ)
- **Chỉ Pagination đơn giản (page, size), không cần search/filter:** `GET /api/conversations`, `GET /api/quizzes/{id}/attempts`, `GET /api/vocabulary/today` (v1.1), lịch sử `DictationResult` (v1.1) — đây là các danh sách cá nhân, thường không lớn và không cần tìm kiếm phức tạp
- **Không cần pagination (danh sách nhỏ, cố định theo 1 Topic):** `GET /api/topics/{id}/flashcards`, `GET /api/topics/{id}/quizzes`

---

## 2.2. QUY TẮC OWNERSHIP & SECURITY (áp dụng cho các entity gắn với dữ liệu cá nhân — liên quan FR-2, FR-5, FR-8)

Nguyên tắc chung: **mọi entity gắn với 1 User cụ thể chỉ được chính User đó (qua JWT) truy cập** — không bao giờ tin tưởng `userId`/`ownerId` do client truyền lên qua query param hay body. Tổng hợp lại để tránh rải rác, thiếu sót khi code:

| Entity | Quy tắc ownership | Áp dụng ở |
|---|---|---|
| `Conversation` / `Message` | Chỉ chủ sở hữu (`conversation.user == JWT.userId`) mới xem/gửi được | FR-2.7 |
| `QuizAttempt` | Chỉ chủ sở hữu mới xem lịch sử làm quiz của mình | FR-5.2 |
| `UserProgress` | Chỉ chủ sở hữu mới xem tiến độ của mình | FR-5.2 |
| `UserVocabulary` (v1.1) | Chỉ chủ sở hữu mới xem/sửa sổ từ vựng cá nhân | mở rộng theo FR-5.2 |
| `DictationResult` (v1.1) | Chỉ chủ sở hữu mới xem kết quả dictation của mình | mở rộng theo FR-5.2 |

**Cách thực thi (kỹ thuật):** ở Service layer, mọi query lấy dữ liệu theo user phải luôn lọc thêm điều kiện `userId = principal.getId()` lấy từ `SecurityContext`/JWT — tuyệt đối không dùng `userId` client gửi lên để query trực tiếp. Đây là nơi rất dễ mắc lỗi IDOR (Insecure Direct Object Reference) nếu chủ quan.

---

## 2.3. GLOBAL DESIGN CONVENTIONS (áp dụng toàn hệ thống, tránh rải rác vào từng FR)

**Ownership failure — response code:**
- Resource không tồn tại → `404`
- Resource tồn tại nhưng thuộc về user khác → **cũng trả `404`**, không trả `403`
- Lý do: `403` gián tiếp xác nhận resource đó *có tồn tại*, chỉ là không có quyền — đây là rò rỉ thông tin không cần thiết cho client. Trả `404` đồng nhất cho cả 2 trường hợp để không tiết lộ sự tồn tại của resource thuộc người khác

**Timezone:**
- Backend/database sử dụng UTC cho mọi timestamp
- Các trường timestamp hệ thống (`createdAt`, `updatedAt`, `completedAt`...) đều do server sinh ra, không do client truyền vào
- Client chịu trách nhiệm chuyển đổi UTC sang timezone hiển thị phù hợp
- **[CẬP NHẬT 01/09/2026 — thay đổi so với bản gốc]** Dùng `Instant` (không phải `LocalDateTime`) cho mọi timestamp ở tầng Entity, map sang cột `TIMESTAMPTZ` ở PostgreSQL. Lý do đổi: `LocalDateTime.now()` phụ thuộc timezone của JVM chạy app, dễ tái phát lỗi quên UTC đã ghi nhận ở mục 1.4; `Instant` loại bỏ khả năng quên bằng thiết kế thay vì bằng kỷ luật nhớ gọi `ZoneOffset.UTC` thủ công. Quyết định này chốt trong lúc code M1 (`User`/`RefreshToken`), áp dụng đồng loạt cho mọi entity có timestamp về sau (xem mục 4).

**Conversation/Entity timestamp — server-owned:**
- `createdAt`/`updatedAt` luôn do backend gán, client không được ghi đè qua request body
- `Conversation.updatedAt` chỉ thay đổi khi có `Message` mới được tạo **thành công** (không update nếu tạo Message thất bại)

**Pagination — default/max:**
- `page` mặc định = `0`
- `size` mặc định = `20`
- `size` tối đa = `100`
- Nếu client truyền `size > 100` → server trả `400 Bad Request` (KHÔNG tự động cắt về giới hạn — chốt 1 hành vi rõ ràng duy nhất để dễ test, tránh "hoặc A hoặc B" gây khó quyết định lúc code)
- *(Giá trị 20/100 là đề xuất thiết kế ban đầu, có thể điều chỉnh khi code thực tế nếu cần)*

**Content deletion — không cascade xóa lịch sử cá nhân:**
- Không hard-delete cascade xuống dữ liệu lịch sử cá nhân của User (`QuizAttempt`, `UserProgress`, và tương tự ở v1.1 như `DictationResult`, `UserVocabulary`)
- MVP chọn cách đơn giản: Admin **chỉ được `DELETE` Topic/Quiz/Flashcard/Question khi resource đó chưa có dữ liệu phụ thuộc** (chưa có `QuizAttempt`/`UserProgress` nào tham chiếu tới) — nếu đã có, API trả `409 Conflict`
- Cơ chế soft-delete/trạng thái nội dung (`DRAFT`/`PUBLISHED`/`ARCHIVED` — đã nêu ở mục 9, Admin Content Management `[v2+]`) sẽ thay thế cách làm đơn giản này khi làm tới v2+

**Database indexing:**
- Cân nhắc thêm index cho các cột khóa ngoại (FK) và các cột thường dùng để filter/sort/query
- Index cụ thể được thêm cùng migration Flyway của milestone tương ứng khi cột đó thực sự cần (VD: index cho `Topic.title`/`level` thêm ở M10 khi làm Search/Filter)
- Không over-index ngay từ đầu khi chưa có nhu cầu truy vấn thực tế

---

## 3. YÊU CẦU PHI CHỨC NĂNG (NON-FUNCTIONAL REQUIREMENTS)

| Mã | Yêu cầu |
|---|---|
| NFR-1 | API tuân thủ REST convention (đúng HTTP method, status code) đã học ở Phase 2 |
| NFR-2 | Áp dụng Layered Architecture + DTO (không expose Entity trực tiếp) — Phase 5 |
| NFR-3 | Có Global Exception Handler (`@ControllerAdvice`) xử lý lỗi thống nhất |
| NFR-4 | Có Validation (`@Valid`, Bean Validation) cho input |
| NFR-5 | Viết Unit Test (JUnit/Mockito) cho Service layer, tối thiểu các luồng chính. **Hoàn thành 13/09/2026 (M8):** 110 test case trên 10 class (5 Service chính bắt buộc + `FileStorageServiceImpl`/`ChatServiceImpl` optional + `JwtUtil`/2 `CacheEvictionListener` phát sinh trong lúc làm). Phạm vi: Unit test thuần, Mockito mock toàn bộ Repository/dependency, không dùng `@DataJpaTest`/Testcontainers (để dành M9 nếu cần integration test). Coverage tính theo nhánh logic (happy path + mọi nhánh throw exception), không chạy theo %. |
| NFR-6 | Cache Redis phải có chiến lược invalidate rõ ràng, tránh dữ liệu cũ |
| NFR-7 | JWT token có thời gian hết hạn hợp lý, không lưu password dạng plaintext |
| NFR-8 | Code cần được rà soát cú pháp/chính tả annotation kỹ trước khi chạy (điểm yếu đã ghi nhận) |
| NFR-9 | Hệ thống có logging phù hợp cho các request quan trọng, lỗi hệ thống và lỗi khi gọi AI Provider (INFO/WARN/ERROR theo mức độ); **không log password, JWT hoặc dữ liệu nhạy cảm** |
| NFR-10 | Các thao tác ghi dữ liệu liên quan nhiều entity phải dùng `@Transactional` phù hợp để đảm bảo tính nhất quán (VD: nộp Quiz → tính điểm → lưu `QuizAttempt` → cập nhật `UserProgress` phải cùng thành công hoặc cùng rollback) |

---

## 4. THIẾT KẾ DỮ LIỆU (ENTITY)

```
User
 ├─ id: Long
 ├─ username: String (unique)
 ├─ email: String (unique)
 ├─ passwordHash: String
 ├─ role: Enum(USER, ADMIN)
 ├─ avatarUrl: String
 └─ createdAt: Instant

Conversation
 ├─ id: Long
 ├─ user: User (ManyToOne)
 ├─ title: String    — ĐÃ CHỐT 04/09/2026 (M4): tự động lấy từ nội dung Message đầu tiên của User, cắt tối đa 50 ký tự (thêm "..." nếu bị cắt), chỉ set 1 lần — xem chi tiết mục 8
 ├─ createdAt: Instant
 └─ updatedAt: Instant   — cập nhật mỗi khi có Message mới, dùng để sort danh sách conversation theo hoạt động gần nhất

Message
 ├─ id: Long
 ├─ conversation: Conversation (ManyToOne)
 ├─ sender: Enum(USER, AI)
 ├─ content: String (Text)             — câu trả lời tự nhiên (reply)
 ├─ correction: String (nullable)      — phần sửa lỗi ngữ pháp nếu có
 ├─ explanation: String (nullable)     — giải thích ngắn gọn vì sao sửa như vậy
 └─ createdAt: Instant

[Business invariant — Message]
Nếu sender = USER:
 - content bắt buộc (not null)
 - correction = null
 - explanation = null
Nếu sender = AI:
 - content là câu trả lời tự nhiên (bắt buộc)
 - correction có thể null (null nếu không có lỗi cần sửa)
 - explanation có thể null (null nếu không có correction)

Topic
 ├─ id: Long
 ├─ title: String
 ├─ description: String
 ├─ level: Enum(BEGINNER, INTERMEDIATE, ADVANCED)
 └─ imageUrl: String
  [DB constraint: UNIQUE(title, level) — chốt 02/09/2026, cho phép trùng title nếu khác level]

Flashcard
 ├─ id: Long
 ├─ topic: Topic (ManyToOne)     — ON DELETE CASCADE, chốt 02/09/2026: Flashcard là nội dung
 │                                  Admin quản lý, không phải lịch sử cá nhân User nên không
 │                                  áp dụng rule "không cascade" ở mục 2.3
 ├─ word: String
 ├─ meaning: String
 ├─ example: String
 ├─ imageUrl: String
 └─ audioUrl: String (nullable)   — CHỐT: URL external do Admin tự nhập (VD: link từ điển online có sẵn audio phát âm), KHÔNG phải file do hệ thống tự lưu trữ. FR-6 (file upload) chỉ áp dụng cho ảnh — không mở rộng sang audio ở MVP để tránh kéo thêm phạm vi không cần thiết

Quiz
 ├─ id: Long
 ├─ topic: Topic (ManyToOne)     — CHỐT: 1 Topic có thể có nhiều Quiz (VD: Travel → Vocabulary Quiz, Grammar Quiz, Conversation Quiz)
 └─ title: String

QuizQuestion
 ├─ id: Long
 ├─ quiz: Quiz (ManyToOne)
 ├─ question: String
 ├─ options: List<String>   — ĐÃ CHỐT 03/09/2026 (M3): dùng @ElementCollection, không dùng JSONB
 └─ correctAnswer: String

QuizAttempt                 — lịch sử từng lần làm quiz, KHÔNG ghi đè
 ├─ id: Long
 ├─ user: User (ManyToOne)
 ├─ quiz: Quiz (ManyToOne)
 ├─ score: Integer
 ├─ correctAnswers: Integer
 ├─ totalQuestions: Integer
 └─ completedAt: Instant

[Business rule — QuizAttempt]
- `score`, `correctAnswers`, `totalQuestions` đều do **backend tự tính**, không bao giờ tin/nhận các giá trị này từ client
- `totalQuestions` = số `QuizQuestion` hợp lệ hiện có của Quiz đó **tại thời điểm submit** (không phải số lượng client tự đếm và gửi lên)

UserProgress                — trạng thái tổng quan HIỆN TẠI của 1 Topic, được cập nhật (không phải tạo mới) sau mỗi hoạt động học liên quan
 ├─ id: Long
 ├─ user: User (ManyToOne)
 ├─ topic: Topic (ManyToOne)
 ├─ status: Enum(NOT_STARTED, IN_PROGRESS, COMPLETED)
 ├─ progressPercent: Integer
 └─ updatedAt: Instant
 [DB constraint: UNIQUE(user, topic) — đảm bảo tối đa 1 record cho mỗi cặp user-topic, thực thi ràng buộc ngay ở tầng DB chứ không chỉ dựa vào logic Service]

[Business rule — UserProgress]
- progressPercent luôn nằm trong khoảng 0–100
- **Record được tạo lazy** — chỉ tạo khi User có hoạt động học đầu tiên liên quan tới Topic đó (VD: lần đầu nộp Quiz thuộc Topic này); **không** tạo sẵn `UserProgress` cho mọi (user, topic) ngay từ khi User đăng ký (tránh phình bảng vô ích, VD 1000 user × 500 topic = 500,000 row rỗng)
- Nếu **chưa tồn tại** record `UserProgress` cho 1 cặp (user, topic) → hiểu ngầm là `NOT_STARTED`, API trả về state mặc định tương ứng thay vì lỗi
- `status = COMPLETED` **khi và chỉ khi** `progressPercent = 100` **và** đạt điều kiện hoàn thành cụ thể được định nghĩa tại M3 (không đơn thuần chỉ dựa vào `progressPercent = 100`, vì công thức tính progressPercent có thể thay đổi sau này)
- `status = IN_PROGRESS` trong mọi trường hợp còn lại, sau khi record đã được tạo (đã có hoạt động học) nhưng chưa đạt điều kiện COMPLETED — **kể cả khi `progressPercent = 0`** (VD: user đã làm quiz nhưng làm sai hết, vẫn tính là đã bắt đầu học, không phải NOT_STARTED)
- Công thức tính cụ thể `progressPercent` và điều kiện `COMPLETED` được chốt tại M3 (FR-5.4)
```

> **Phân biệt rõ 2 entity dễ nhầm:**
> - `QuizAttempt` = **HISTORY** — mỗi lần User nộp bài tạo **1 record mới**, không bao giờ update/ghi đè record cũ. Dùng để xem lại "tôi đã làm Quiz Travel 5 lần, điểm lần lượt là..."
> - `UserProgress` = **CURRENT STATE** — chỉ có **tối đa 1 record** cho mỗi cặp (`user`, `topic`), được **UPDATE tại chỗ** mỗi khi có hoạt động học mới liên quan tới Topic đó. Dùng để trả lời nhanh "hiện tại tôi đang ở mức nào với Topic Travel". Query kiểu `findByUserAndTopic()` nên trả về 0 hoặc 1 kết quả, không bao giờ nhiều hơn; nếu không tìm thấy, Service tự map thành `NOT_STARTED` chứ không throw lỗi
>
> **Phạm vi `QuizAttempt` trong MVP:** chỉ lưu **kết quả tổng quan** của 1 lần làm bài (`score`, `correctAnswers`, `totalQuestions`, `completedAt`) — **chưa lưu chi tiết từng đáp án** người dùng đã chọn cho từng câu. Chức năng "xem lại tôi đã chọn đáp án nào cho từng câu" (cần thêm entity `QuizAttemptAnswer`) là mở rộng sau MVP, không nằm trong M3.

```

--- Entity mở rộng `[v1.1]` — Milestone M10–M12 ---

DictationLesson
 ├─ id: Long
 ├─ topic: Topic (ManyToOne)
 ├─ title: String
 ├─ mediaUrl: String
 ├─ transcript: String (Text)
 └─ level: Enum(BEGINNER, INTERMEDIATE, ADVANCED)

DictationResult
 ├─ id: Long
 ├─ user: User (ManyToOne)
 ├─ lesson: DictationLesson (ManyToOne)
 ├─ userInput: String
 ├─ accuracy: Double
 └─ createdAt: Instant

UserVocabulary
 ├─ id: Long
 ├─ user: User (ManyToOne)
 ├─ flashcard: Flashcard (ManyToOne)
 ├─ status: Enum(NEW, LEARNING, KNOWN)
 ├─ reviewCount: Integer
 ├─ lastReviewedAt: Instant
 ├─ nextReviewAt: Instant
 └─ difficulty: Integer
 [DB constraint: UNIQUE(user, flashcard) — 1 User chỉ có tối đa 1 bản ghi ôn tập cho mỗi Flashcard, tránh trùng lặp khi lưu từ nhiều lần]
```

**Lưu ý khi implement:** cân nhắc kỹ Lazy vs Eager Loading giữa các quan hệ trên (Phase 3) để tránh N+1, đặc biệt khi load `Conversation` kèm `Message`, hoặc `Topic` kèm `Flashcard`.

---

## 5. API ENDPOINTS

### Auth
| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Đăng ký |
| POST | `/api/auth/login` | Public | Đăng nhập, trả JWT |

### Chat `[MVP]`
| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| POST | `/api/conversations` | USER | Tạo conversation mới |
| GET | `/api/conversations?page=&size=` | USER | Danh sách conversation của user, sort theo `updatedAt` giảm dần, phân trang đơn giản (không search — theo phạm vi FR-9) |
| GET | `/api/conversations/{id}` | USER | Chi tiết 1 conversation (title, thời gian) — chỉ nếu thuộc về user hiện tại (FR-2.7), ngược lại trả 404 (theo convention mục 2.3) |
| GET | `/api/conversations/{id}/messages` | USER | Lịch sử tin nhắn — chỉ nếu conversation thuộc về user hiện tại (FR-2.7), ngược lại trả 404 (theo convention mục 2.3) |
| WS | `/ws/chat` (STOMP) | USER | Gửi/nhận tin nhắn realtime — connection phải authenticate bằng JWT, backend kiểm tra ownership conversation trước khi xử lý send/subscribe (FR-2.7); backend tự đính kèm context lịch sử hội thoại (FR-2.8) |

### Topic & Flashcard (Admin quản lý nội dung) `[MVP]`
| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| POST | `/api/admin/topics` | ADMIN | Tạo topic |
| PUT | `/api/admin/topics/{id}` | ADMIN | Sửa topic |
| DELETE | `/api/admin/topics/{id}` | ADMIN | Xóa topic |
| POST | `/api/admin/topics/{id}/image` | ADMIN | Upload ảnh topic |
| POST | `/api/admin/topics/{id}/flashcards` | ADMIN | Thêm flashcard vào topic |
| PUT | `/api/admin/flashcards/{id}` | ADMIN | Sửa flashcard |
| DELETE | `/api/admin/flashcards/{id}` | ADMIN | Xóa flashcard |
| POST | `/api/admin/flashcards/{id}/image` | ADMIN | Upload ảnh minh họa cho flashcard |

### Topic & Flashcard (User học) `[MVP]`
| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| GET | `/api/topics?keyword=&level=&page=&size=&sort=` | USER | Danh sách topic — search/filter/sort/pagination đầy đủ (có cache, FR-9) |
| GET | `/api/topics/{id}` | USER | Chi tiết 1 topic |
| GET | `/api/topics/{id}/flashcards` | USER | Danh sách flashcard theo topic (không cần phân trang — danh sách nhỏ theo 1 topic) |

### Quiz (Admin quản lý nội dung) `[MVP]`
| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| POST | `/api/admin/topics/{id}/quizzes` | ADMIN | Tạo quiz mới thuộc topic |
| PUT | `/api/admin/quizzes/{id}` | ADMIN | Sửa thông tin quiz |
| DELETE | `/api/admin/quizzes/{id}` | ADMIN | Xóa quiz |
| POST | `/api/admin/quizzes/{id}/questions` | ADMIN | Thêm câu hỏi (bao gồm `correctAnswer`) vào quiz |
| PUT | `/api/admin/questions/{id}` | ADMIN | Sửa câu hỏi |
| DELETE | `/api/admin/questions/{id}` | ADMIN | Xóa câu hỏi |

### Quiz (User làm bài) `[MVP]`
| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| GET | `/api/topics/{id}/quizzes` | USER | Danh sách quiz thuộc 1 topic |
| GET | `/api/quizzes/{id}` | USER | Chi tiết đề quiz — **không** trả `correctAnswer` (FR-4.5) |
| POST | `/api/quizzes/{id}/submit` | USER | Nộp bài, chấm điểm, tạo `QuizAttempt` mới + cập nhật `UserProgress` (trong 1 transaction — NFR-10) |
| GET | `/api/quizzes/{id}/attempts?page=&size=` | USER | Lịch sử các lần làm quiz này của chính mình, phân trang đơn giản |

### Progress `[MVP]`
| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| GET | `/api/users/me/progress` | USER | Xem tiến độ học của bản thân — luôn lấy `userId` từ JWT, không nhận `?userId=` từ query (FR-5.2) |

### Profile `[MVP]`
| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| POST | `/api/users/me/avatar` | USER | Upload/cập nhật avatar cá nhân, dùng chung rule validate với FR-6 |

### Dictation `[v1.1]`
| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| POST | `/api/admin/topics/{id}/dictation` | ADMIN | Tạo Dictation Lesson thuộc topic |
| PUT | `/api/admin/dictation/{id}` | ADMIN | Sửa Dictation Lesson |
| DELETE | `/api/admin/dictation/{id}` | ADMIN | Xóa Dictation Lesson |
| GET | `/api/topics/{id}/dictation` | USER | Danh sách bài dictation theo topic (không phân trang — nhỏ) |
| POST | `/api/dictation/{id}/submit` | USER | Nộp kết quả, nhận accuracy + highlight, lưu `DictationResult` |
| GET | `/api/dictation/{id}/results?page=&size=` | USER | Lịch sử kết quả của chính mình cho bài dictation này |

### Vocabulary/SRS `[v1.1]` (FR-8)
| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| POST | `/api/vocabulary/{flashcardId}/save` | USER | Lưu từ vào sổ từ vựng cá nhân |
| PATCH | `/api/vocabulary/{id}/review` | USER | Đánh dấu đã/chưa nhớ sau khi ôn |
| GET | `/api/vocabulary/today?page=&size=` | USER | Danh sách từ cần ôn hôm nay, phân trang đơn giản |

---

## 6. CẤU TRÚC THƯ MỤC DỰ KIẾN

```
com.example.englishlearningplatform
 ├── config/         (SecurityConfig, WebSocketConfig, RedisConfig, WebClientConfig)
 ├── controller/
 ├── service/
 ├── repository/
 ├── entity/
 ├── dto/            (request/response, tách biệt Entity)
 ├── security/        (JwtFilter, JwtUtil, UserDetailsServiceImpl)
 ├── ai/              (AiClient — gọi xKiro API)
 ├── exception/       (GlobalExceptionHandler, custom exceptions)
 └── util/

frontend/
 ├── index.html        (trang chat)
 ├── topics.html        (danh sách chủ đề học)
 ├── quiz.html
 ├── admin.html          (trang quản trị nội dung)
 ├── js/
 │    ├── api.js          (wrapper fetch có gắn JWT)
 │    ├── chat.js          (kết nối WebSocket)
 │    └── auth.js
 └── css/
```

**Nguyên tắc:** đây là Layered Architecture chuẩn (Phase 5), nhưng **không tạo sẵn toàn bộ class/package ngay từ đầu** chỉ vì Requirements liệt kê nhiều chức năng. Cấu trúc nên "tiến hóa" theo từng milestone — ví dụ M1 chỉ cần `entity/User`, `repository/UserRepository`, `service/AuthService`, `controller/AuthController`, `dto/auth/*`, `security/*`; đến M2 mới xuất hiện `Topic`/`Flashcard`, v.v. Tránh việc tạo package rỗng gây rối mà chưa dùng tới.

*(Đây là cấu trúc **dự kiến** lúc thiết kế ban đầu, không phải snapshot thực tế — cấu trúc project thật tại từng thời điểm được track ở `Session_Summary.md` mục 6, cập nhật sau mỗi milestone.)*

---

## 7. LỘ TRÌNH XÂY DỰNG (MILESTONES)

| Giai đoạn | Nội dung | Trạng thái |
|---|---|---|
| M1 | Scaffold project, cấu hình DB (Flyway migration), Security cơ bản, JWT Auth (FR-1) | **Hoàn thành 01/09/2026** |
| M2 | CRUD Topic/Flashcard (Admin) + xem (User) — chưa cache vội, CRUD thuần trước (FR-3) → test bằng Postman → dựng `topics.html` hiển thị thử | **Hoàn thành 02/09/2026** |
| M3 | Module Quiz + chấm điểm + `QuizAttempt`/`UserProgress` (FR-4, FR-5) → test Postman → `quiz.html` | **Hoàn thành 02/09/2026** |
| M4 | Tích hợp AI qua xKiro — chat REST cơ bản trước, chưa cần WebSocket (FR-2 phần cơ bản) → test Postman → chat.html bản REST đơn giản | **Hoàn thành 04/09/2026** |
| M5 | Thêm Redis cache cho danh sách Topic/Flashcard, có invalidate khi Admin cập nhật (FR-3.5) |  **Hoàn thành 07/09/2026** |
| M6 | Nâng cấp chat sang WebSocket streaming (FR-2.5) → cập nhật `chat.html` dùng SockJS/STOMP | **Hoàn thành 08/09/2026** |
| M7 | File upload ảnh flashcard/avatar (FR-6) | **Hoàn thành 10/09/2026** |
| M8 | Viết Unit Test cho các Service chính (NFR-5) | **Hoàn thành 13/09/2026** — 110 test: `QuizAttemptService`(19), `UserProgressService`(3), `AuthService`(10), `TopicService`(18), `FlashcardService`(12) [chính]; `FileStorageServiceImpl`(14), `ChatServiceImpl`(21) [optional]; `JwtUtil`(7), `TopicCacheEvictionListener`(4), `FlashcardCacheEvictionListener`(2) [phát sinh] |
| M9 | Hoàn thiện `admin.html`, polish UI toàn bộ, test tổng thể end-to-end | Chưa bắt đầu |
| **M10** | **(v1.1)** Search/Filter/Pagination cho Topic (FR-9) | Chưa bắt đầu |
| **M11** | **(v1.1)** Vocabulary & SRS — lưu từ, thuật toán ôn tập (FR-8) | Chưa bắt đầu |
| **M12** | **(v1.1)** Dictation — nghe, so sánh transcript, tính accuracy (FR-7) | Chưa bắt đầu |

*Thứ tự này ưu tiên CRUD/domain logic (Auth → Topic → Quiz) trước, rồi mới tới 2 phần "khó" là tích hợp AI và Redis/WebSocket — tránh việc học nhiều kỹ thuật khó cùng lúc ở M2 như bản trước. Mỗi milestone backend đều có bước test Postman + dựng mini-frontend ngay sau đó, thay vì dồn toàn bộ tích hợp frontend vào 1 milestone cuối (dễ gây debug integration dồn cục, khó xác định lỗi nằm ở đâu). M1–M9 là MVP bắt buộc; M10–M12 làm ngay sau khi MVP chạy ổn định. Các tính năng mở rộng khác (mục 9) vẫn nằm trong kế hoạch dài hạn, làm sau M12 hoặc song song với việc quay lại Project 2 tùy thời gian thực tế.*

---

## 8. GHI CHÚ MỞ — QUYẾT ĐỊNH TẠI MILESTONE TƯƠNG ỨNG (KHÔNG CHẶN TIẾN ĐỘ HIỆN TẠI)

Các điểm dưới đây **cố ý chưa khóa cứng** ngay trong Requirements — không phải vì thiếu sót, mà vì quyết định quá sớm khi chưa code tới sẽ dễ phải sửa lại. Mỗi điểm đã gắn milestone cụ thể sẽ chốt khi tới lượt:

| Điểm còn mở | Chốt tại | Ghi chú |
|---|---|---|
| Database: PostgreSQL hay MySQL | ~~M1~~ **Đã chốt 01/09/2026: PostgreSQL** | Không ảnh hưởng thiết kế tổng thể, chỉ khác driver/config; PostgreSQL mở khóa dùng JSONB cho `QuizQuestion.options` |
| Có dùng Refresh Token hay chỉ Access Token đơn giản | ~~M1~~ **Đã chốt 01/09/2026: cả Access + Refresh Token** | Refresh Token lưu DB để có thể revoke, không chỉ dựa vào thời gian hết hạn |
| Kiểu lưu `QuizQuestion.options` (JSONB / `@ElementCollection`) | ~~M3~~ **Đã chốt 03/09/2026: `@ElementCollection`** | Không dùng JSONB dù PostgreSQL hỗ trợ — ưu tiên database-independent, đơn giản hơn cho quy mô hiện tại |
| Công thức tính `UserProgress.progressPercent` và điều kiện `COMPLETED` (FR-5.4) | ~~M3~~ **Đã chốt 03/09/2026** | 1 Quiz được coi là "đạt" ⟺ tồn tại ít nhất 1 QuizAttempt của User cho Quiz đó có score ≥ 70 progressPercent = round(100 × (số Quiz "đạt") / (tổng số Quiz thuộc Topic)) status = COMPLETED  ⟺  (số Quiz "đạt") == (tổng số Quiz thuộc Topic)  VÀ  tổng số Quiz > 0 status = IN_PROGRESS trong mọi trường hợp còn lại (đã có ít nhất 1 lần hoạt động nhưng chưa COMPLETED) |
| Định nghĩa "lượt học" dùng cho `sort=popular` (FR-9.4) — tính khi User mở Topic? Làm Quiz? Hoàn thành Quiz? | **M10** | Chưa thêm counter/entity nào (VD: `studyCount`) ngay bây giờ — chỉ quyết định và implement khi tới M10, tránh thiết kế sai rồi phải sửa lại |
| Cách tạo `Conversation.title` | ~~M4~~ **Đã chốt 04/09/2026** | Tự động lấy từ nội dung `Message` đầu tiên của User trong Conversation, cắt tối đa 50 ký tự (thêm "..." nếu bị cắt). Chỉ set 1 lần, không ghi đè ở các message sau |
| Số lượng message lịch sử đính kèm làm AI context (FR-2.8) | ~~M4~~ **Đã chốt 04/09/2026** | N = 10 message gần nhất (bao gồm cả message User vừa gửi). Config qua `app.ai.context-message-limit` trong `application.yml`, không hardcode |
| Chọn cụ thể model AI trên xKiro | ~~M4~~ **Đã chốt 04/09/2026** | `qwen/qwen3.6-plus:free` — tier free để test không tốn phí, multilingual tốt (hợp cho response tiếng Việt + tiếng Anh). Model ID dạng `vendor/model`, đổi được qua config nếu cần chất lượng cao hơn |
| Format JSON chuẩn cho phản hồi AI (`reply`/`correction`/`explanation`) | ~~M4~~ **Đã chốt 04/09/2026** | Ép qua system prompt yêu cầu model trả đúng 1 JSON object 3 field. Parse bằng Jackson, có fallback: nếu parse lỗi (model trả kèm text/markdown fence dù đã cấm), coi toàn bộ raw text là `reply`, `correction`/`explanation` = null — không throw 500 vì lỗi format của model |
| Thuật toán SRS cụ thể (FR-8.3) | **M11** | Không commit SM-2, có thể tự thiết kế đơn giản hơn (xem FR-8.3) |
| Chiến lược Cache Eviction & Phòng ngừa Race Condition | ~~M5~~ **Đã chốt 05/09/2026** | Không dùng @CacheEvict trực tiếp trên method có @Transactional để tránh race condition evict-trước-commit (Redis bị xóa trước khi DB commit, dẫn đến request đọc lại đúng lúc đó sẽ cache lại dữ liệu cũ). Giải pháp: Dùng @TransactionalEventListener(phase = AFTER_COMMIT) lắng nghe Domain Event do Service phát ra sau khi DB update thành công, rồi xóa cache thủ công qua CacheManager. Nếu Transaction bị Rollback, event xóa cache hoàn toàn bị hủy, giữ an toàn tuyệt đối cho Redis Cache. |
| Kiến trúc WebSocket Streaming (FR-2.5/FR-2.7) | ~~M6~~ **Đã chốt 08/09/2026** | **Ownership (FR-2.7) áp dụng 2 điểm khác REST:** xác thực JWT ở STOMP CONNECT (qua `ChannelInterceptor`, không qua `JwtAuthenticationFilter` cũ vì STOMP frame sau CONNECT không đi qua HTTP filter chain), check ownership `Conversation` ở STOMP SUBSCRIBE (method `chatService.isOwner()` mới thêm). **Lỗi ở tầng ChannelInterceptor/subscribe callback không đi qua `@MessageExceptionHandler`** (chỉ bắt exception đồng bộ trong `@MessageMapping`) — cần `StompSubProtocolErrorHandler` riêng để trả STOMP ERROR frame rõ ràng trước khi Spring đóng kết nối (đóng kết nối là hành vi đúng chuẩn STOMP, không tránh được). **Tách reply/meta khi stream:** xem chi tiết ở FR-2.5. **Known limitation chấp nhận:** `save()` JPA là blocking call chạy trên Reactor event loop thread trong `sendMessageStream()` — chấp nhận ở quy mô hiện tại, cân nhắc `Schedulers.boundedElastic()` nếu traffic tăng lớn. |


---

## 9. ĐỊNH HƯỚNG MỞ RỘNG DÀI HẠN (v2+ — SAU M12)

Tham khảo từ [parroto.app](https://parroto.app/vi) (28/08/2026). Đây là **roadmap thật sự**, không phải danh sách loại bỏ — vẫn muốn làm, nhưng **sau khi nền tảng (M1–M12) đã vững**, vì mỗi nhóm dưới đây đòi hỏi kỹ thuật/hạ tầng riêng khá nặng, nên cần Backend đã chắc mới bắt đầu để tránh vỡ kiến trúc giữa chừng. Thứ tự trong bảng là gợi ý độ ưu tiên/độ khó tăng dần khi quay lại:

| Nhóm | Ghi chú kỹ thuật khi triển khai |
|---|---|
| Admin Content Management đầy đủ (status Draft/Published/Archived) | Dễ nhất — chỉ cần thêm field `status` vào Topic/Quiz/Lesson đã có |
| User Profile mở rộng (statistics, achievements) | Mở rộng tự nhiên từ entity `User`, ghép cùng Gamification |
| Gamification (XP, streak, leaderboard, achievement) | Cần thêm entity `Achievement`, `UserAchievement`; leaderboard nên cache Redis (sorted set) |
| Learning Profile & Personalized Path | Cần đủ dữ liệu tích lũy từ Progress/Vocab để gợi ý có ý nghĩa — nên làm sau khi có user thật dùng thử |
| Notification hệ thống | Cần Spring Scheduler + tích hợp email/push, thêm hạ tầng gửi thư |
| Exam Preparation (IELTS/TOEIC/TOEFL) | Entity `Exam → ExamPart → Question`, chủ yếu là CRUD nội dung lớn + Timer/Auto-save |
| AI Lesson Generator từ YouTube | Cần kiểm tra kỹ giới hạn API lấy transcript YouTube trước khi làm, rủi ro kỹ thuật/pháp lý |
| Shadowing (chấm phát âm, intonation, rhythm) | Cần tích hợp Speech-to-Text API + xử lý audio — nên khảo sát provider (Whisper API, Google STT...) trước |
| Community Speaking / Voice Chat (WebRTC) | Khó nhất — cần signaling server, WebRTC, matching queue; nên làm cuối cùng |

*Khi bắt đầu bất kỳ nhóm nào ở trên, quay lại bổ sung FR/entity/API chi tiết vào tài liệu này trước khi code — giữ đúng nguyên tắc "thiết kế trước, code sau" đã áp dụng xuyên suốt.*

---

*Tài liệu này sẽ được cập nhật trạng thái Milestone khi tiến độ thay đổi. Dùng làm tài liệu tham chiếu chính trong suốt quá trình code Project 1.*
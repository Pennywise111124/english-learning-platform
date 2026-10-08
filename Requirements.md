# TÀI LIỆU YÊU CẦU (REQUIREMENTS)
# Project 1 — Nền tảng học tiếng Anh tích hợp AI
*(AI English Learning Platform — trước đây gọi là "Chatbot Học Tiếng Anh"; đổi tên vì phạm vi đã lớn hơn 1 chatbot đơn thuần: gồm AI Chat, Topic/Flashcard, Quiz, Progress, Vocabulary/SRS, Dictation, Search)*

**Phiên bản:** 1.9
**Ngày tạo:** 28/08/2026 — cập nhật lần 9 (07/10/2026, hoàn thành M13 Hardening + M14 Content Status)
**Trạng thái:** M1–M14 hoàn thành (MVP + v1.1 + hardening + trạng thái nội dung Draft/Published/Archived)

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
| `ADMIN` | Quản trị nội dung: CRUD chủ đề học, flashcard, quiz, bài Dictation; quản lý trạng thái nội dung (Draft/Published/Archived) |

### 1.3. Công nghệ sử dụng

| Thành phần | Lựa chọn | Ghi chú |
|---|---|---|
| Backend | Java 21, Spring Boot 4.1.1 | Cập nhật 01/09/2026 — khác bản dự kiến ban đầu (Java 17+/Spring Boot 3.x); kéo theo hệ quả Jackson 3 thay vì Jackson 2, xem ghi chú mục 1.4 |
| Bảo mật | Spring Security + JWT | Phân quyền USER/ADMIN |
| Database | PostgreSQL | Qua Spring Data JPA, quản lý schema bằng **Flyway migration** (không dùng `ddl-auto: update`) để kiểm soát lịch sử thay đổi schema rõ ràng |
| Cache | Redis | Cache Topic/Flashcard, cân nhắc cache câu trả lời AI lặp lại |
| Realtime | WebSocket (STOMP) | Chat AI trả lời dạng stream |
| AI Provider | xKiro (`api.xkiro.com/v1`, chuẩn OpenAI-compatible) | Dùng `WebClient`/`RestClient` |
| Upload file | Lưu local filesystem (giai đoạn đầu), có thể nâng cấp cloud storage sau | Ảnh flashcard, avatar; audio Dictation (M12, xem FR-7) |
| Frontend | HTML/CSS/JavaScript thuần + Tailwind CSS (qua CDN) | Gọi REST API bằng `fetch`, WebSocket bằng `SockJS + STOMP` client. **Sửa 05/09/2026:** đổi từ Bootstrap (dự kiến ban đầu) sang Tailwind. **Cập nhật M9:** redesign toàn bộ theo design system riêng (Duolingo-inspired) — xem chi tiết đầy đủ ở mục 10 (mới). |
| Testing | JUnit 5 + Mockito + MockMvc + Testcontainers (PostgreSQL 16) | Unit test Service (Mockito), test `GlobalExceptionHandler` bằng MockMvc standalone, integration test Repository/Specification/migration trên PostgreSQL thật (M13). **Mọi `mvnw test` đều cần Docker đang chạy.** |
| API Documentation | springdoc-openapi 3.1.1 | Swagger UI tự sinh từ code tại `/swagger-ui.html` — nguồn tham chiếu chính xác nhất cho request/response shape thật, ưu tiên hơn tài liệu này khi có sai lệch. Thêm ở M9. |

### 1.4. Ràng buộc & nguyên tắc làm việc
- Ưu tiên có **scaffold (khung sườn code)** trước khi người học tự code phần logic chi tiết
- Khi phát sinh lỗi cú pháp/chính tả, dừng lại rà soát kỹ trước khi chốt code (điểm yếu đã ghi nhận)
- Khi giải thích kỹ thuật, luôn đi tới cơ chế cụ thể tầng dưới, không dừng ở kết luận chung chung
- Cẩn trọng khi áp dụng lại 1 rule cũ (VD: cache side-effect vs quyết định dùng Async — 2 tiêu chí độc lập, đã từng nhầm)
- Mọi Enum trong Entity dùng `@Enumerated(EnumType.STRING)`, không dùng mặc định (ORDINAL) — tránh lỗi âm thầm khi thêm giá trị enum mới về sau làm lệch dữ liệu cũ
- Lưu ý: Spring Boot 4 mặc định dùng **Jackson 3** (package `tools.jackson.databind`, bean `JsonMapper`), KHÔNG phải Jackson 2 (`com.fasterxml.jackson.databind.ObjectMapper`) như quen thuộc — phát hiện khi code `AiClientImpl` ở M4. Cần nhớ khi làm việc với JSON serialize/deserialize thủ công ở các milestone sau.
- Lưu ý: `SecurityConfig` mặc định trả 403 thay vì 401 khi thiếu/sai JWT (do chưa custom `AuthenticationEntryPoint`) — đã fix ở M4 bằng `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)`, áp dụng cho toàn bộ API từ M4 trở đi. (M13: `HttpStatusEntryPoint` đã được thay bằng `JsonAuthenticationEntryPoint` để 401 có body.)
- Lưu ý: phát hiện ở M7 — thiếu `.requestMatchers("/error").permitAll()` trong `SecurityConfig` khiến MỌI lỗi 403 (`AccessDeniedException`, VD: USER hợp lệ gọi route `hasRole("ADMIN")`) bị biến thành 401 rỗng. Nguyên nhân: Spring Boot tự forward nội bộ sang `/error` để render error body, request forward này đi lại qua Security Filter Chain; `JwtAuthenticationFilter` (kế thừa `OncePerRequestFilter`) mặc định bỏ qua dispatch `ERROR` (không set lại `SecurityContext`) nên `/error` rơi vào trạng thái anonymous, bị chặn tiếp bởi `anyRequest().authenticated()`, rồi `AuthenticationEntryPoint` ghi đè 403 thành 401. Bug tồn tại từ M4 (khi thêm `hasRole("ADMIN")`) nhưng chưa từng lộ ra vì chưa có test nào dùng token USER hợp lệ gọi route ADMIN cho tới M7. Đã fix bằng cách thêm `/error` vào danh sách `permitAll()`.
- Lưu ý: phát hiện ở M8 khi viết Unit Test — 3 lỗi kỹ thuật lặp lại nhiều lần, đáng nhớ cho các milestone sau: (1) mock 1 method trả về object thường (không phải `Optional`) mà quên stub sẽ mặc định trả `null` (khác `Optional` được Mockito tự trả `Optional.empty()`), dễ gây `NullPointerException` ở bất kỳ chỗ nào service đọc lại giá trị từ `save()`; (2) `ArgumentCaptor.capture()` khớp với **mọi** lời gọi bất kể kiểu tham số thật (khác `any(SomeClass.class)` có kiểm tra kiểu runtime) — nếu 1 method bị gọi nhiều lần với các loại Event/tham số khác nhau trong cùng 1 lần thực thi, dùng `capture()` với `verify(times(1))` mặc định sẽ gây lỗi `TooManyActualInvocations`; cách đúng là `verify(times(n))` rồi tự lọc lại theo `instanceof`; (3) khi input được validate theo kiểu exact-match (VD: `options.contains(rawAnswer)`) TRƯỚC, rồi mới tới bước so sánh case-insensitive/trim, thì input có khoảng trắng thừa hoặc sai case sẽ bị chặn ngay ở bước validate — không bao giờ chạm tới được bước so khớp linh hoạt hơn phía sau.
- Lưu ý: phát hiện ở M9 — CORS **không có cấu hình mặc định** trong Spring Security, phải khai báo tường minh (`CorsConfigurationSource` bean + `.cors(...)` trong `SecurityFilterChain`) ngay khi FE/BE chạy khác origin (kể cả localhost khác port, hoặc qua tunnel như ngrok). Riêng SockJS (WebSocket) tự gửi request kèm `withCredentials: true` cho bước `/info` — nếu bật `allowCredentials(true)` phía Backend, **bắt buộc** dùng `setAllowedOriginPatterns(...)` thay vì `setAllowedOrigins("*")` (Spring Security cấm kết hợp wildcard `*` với credentials, ném exception lúc khởi động). Preflight `OPTIONS` cũng phải nằm trong danh sách `permitAll()` riêng, đặt trước `anyRequest().authenticated()`, nếu không JWT filter sẽ chặn preflight trước khi CORS kịp xử lý.
- Lưu ý: phát hiện ở M9 (lặp lại nhiều lần trong lúc code FE) — gắn `addEventListener` cho 1 `id` không tồn tại trên DOM tại thời điểm script chạy sẽ ném `TypeError` đồng bộ ngay tại dòng đó; vì đây là top-level statement trong `<script type="module">`, **toàn bộ code phía sau trong cùng module dừng chạy hoàn toàn** (kể cả lệnh gọi hàm khởi tạo dữ liệu ở cuối file) — không có exception nào hiện ra ngoài Console, dễ nhầm là "trang không load được gì cả" thay vì đúng nguyên nhân là 1 dòng bị lỗi giữa file. Luôn kiểm tra Console trước khi đoán nguyên nhân khi 1 trang JS đột nhiên "im lặng không làm gì".
- Lưu ý: phát hiện ở M9 — khi mở public 1 API vốn trước đó yêu cầu JWT (VD: `GET /api/topics`), cần sửa **cả 2 lớp** cùng lúc: (1) `SecurityConfig` thêm `permitAll()` cho đúng method + path, và (2) tầng FE dùng chung (`navbar.js`) phải có nhánh render riêng cho "khách chưa đăng nhập" — nếu chỉ sửa Backend, trang vẫn tải được dữ liệu nhưng phần điều hướng/thanh nav sẽ trống trơn với khách vãng lai (vì logic cũ giả định luôn có `user` mới render).
- Lưu ý: phát hiện ở M9 — dịch vụ tunnel dev (VD: ngrok free tier) chèn trang cảnh báo (interstitial) chặn request đầu tiên từ 1 trình duyệt/phiên chưa từng xác nhận; cần thêm header `ngrok-skip-browser-warning` vào **mọi request `fetch()` tự viết**. Riêng các thư viện bên thứ 3 tự thực hiện request nội bộ không qua code của mình (VD: bước dò `/info` của SockJS trước khi nâng WebSocket) sẽ **không** có header này và có thể vẫn bị chặn ở phiên trình duyệt hoàn toàn mới (ẩn danh) — đây là giới hạn của môi trường dev qua tunnel, không phải bug code, và biến mất khi deploy domain thật.
- Lưu ý: phát hiện ở M10 — khi 1 method `@Cacheable` đổi chữ ký (thêm tham số mới), `condition`/`key` SpEL PHẢI đổi cùng lúc, nếu không cache cũ (key theo chữ ký cũ) vẫn nằm trong Redis và có thể bị đọc nhầm nếu key mới trùng tình cờ; an toàn nhất là đổi định dạng key đủ khác biệt (VD thêm tiền tố) để không bao giờ đụng entry cũ, thay vì tin tưởng TTL tự dọn kịp.
- Lưu ý: phát hiện ở M10 — sort theo cột kiểu Enum lưu dạng String (`ORDER BY level`) sẽ ra thứ tự theo alphabet của tên hằng số (`ADVANCED, BEGINNER, INTERMEDIATE`), không phải thứ tự cấp độ thật; nếu cần sort theo "độ khó" phải map thủ công sang số hoặc dùng `CASE WHEN`, không dùng thẳng cột enum.
- Lưu ý: phát hiện ở M10 — mọi nhánh `ORDER BY` áp dụng cho 1 danh sách phân trang phải có tie-break theo cột duy nhất (`id`), nếu không thứ tự giữa các bản ghi bằng điểm nhau (VD cùng lượt học) có thể xáo trộn giữa 2 lần gọi, gây trùng/sót bản ghi khi client duyệt qua nhiều trang.
- Lưu ý: phát hiện ở M10 — 1 `Specification` dùng chung cho cả query dữ liệu lẫn query đếm (`count`) của Spring Data JPA; `ORDER BY` (đặc biệt kèm subquery) chỉ được áp dụng nhánh dữ liệu, phải tự kiểm tra `query.getResultType()` khác `Long`/`long` trước khi gọi `query.orderBy(...)`, nếu không count query sẽ sai hoặc lỗi.
- Lưu ý: phát hiện ở M10/M11 (lặp lại nhiều lần) — thêm 1 tham số constructor mới vào Service (VD thêm Repository để retrofit check) mà quên thêm `@Mock` tương ứng trong test class cũ sẽ KHÔNG gây lỗi biên dịch — Mockito lặng lẽ truyền `null` vào tham số không có mock khớp, và test chỉ vỡ lúc chạy (`NullPointerException`) tại đúng nhánh code mới chạm tới tham số đó; phải rà lại mọi test dùng `@InjectMocks` của Service đó ngay khi đổi constructor.
- Lưu ý: phát hiện ở M10/M11 — `MockitoExtension` dùng strict stubbing mặc định: nếu 1 test stub sẵn 1 lời gọi nhưng nhánh code thực tế throw exception SỚM HƠN (trước khi chạm tới lời gọi đó), test sẽ vỡ với `UnnecessaryStubbingException` dù logic đúng — chỉ stub đúng những gì nhánh code thật sự sẽ gọi tới trong test đó.
- Lưu ý: phát hiện ở M11 — `GlobalExceptionHandler` ban đầu (từ M1-M7) chưa bắt `HttpMessageNotReadableException` (body JSON hỏng hoặc sai kiểu, VD gửi chuỗi cho field `Boolean`) và `MethodArgumentTypeMismatchException` (path variable sai kiểu, VD `/api/vocabulary/abc/save`) — cả 2 rơi xuống handler `Exception.class` chung, trả nhầm `500` thay vì `400`. Vá ở M11 bằng 2 `@ExceptionHandler` riêng. Lỗi có từ trước M11 nhưng chỉ lộ rõ khi M11 là milestone đầu tiên có field kiểu `Boolean` trong request body. (M13: các handler vá từng cái này được thay bằng cách kế thừa `ResponseEntityExceptionHandler`, xem bullet M13 bên dưới; chỉ còn handler riêng cho `MultipartException` và `MethodArgumentTypeMismatchException`.)
- Lưu ý: phát hiện ở M11 — 1 tính năng cho phép sửa/xoá dữ liệu Admin quản lý (Topic/Flashcard) phải được rà lại MỖI KHI thêm 1 entity cá nhân mới tham chiếu tới nó qua FK (ở đây là `UserVocabulary` tham chiếu `Flashcard`), nếu không sẽ vi phạm rule "không cascade xoá lịch sử cá nhân" (mục 2.3) một cách âm thầm — Admin xoá Flashcard/Topic thành công nhưng để lại bản ghi `UserVocabulary` mồ côi, hoặc tệ hơn là câu lệnh xoá tự nổ lỗi 500 do vi phạm ràng buộc FK nếu FK đó có cascade ở tầng DB.
- Lưu ý: phát hiện ở M12 — `Specification` + `Pageable` có `Sort`: nếu `Pageable` truyền vào `findAll(spec, pageable)` có `Sort`, Spring Data **ghi đè** `ORDER BY` do chính `Specification` đặt, nên `sort=recent`/`title` âm thầm mất tác dụng mà không báo lỗi. Khi `ORDER BY` nằm trong `Specification` (như `TopicSpecifications`, `DictationLessonSpecifications`), phải dùng `PageRequest.of(page, size)` **không kèm Sort**. Có test `ArgumentCaptor<Pageable>` khoá lại (`pageable.getSort().isUnsorted()`).
- Lưu ý: phát hiện ở M12 — PostgreSQL xếp `NULL` lên **đầu** khi `ORDER BY ... DESC`. Khi sắp xếp theo cột có thể rỗng (VD thời điểm làm bài gần nhất, bài chưa làm = null), bản ghi null sẽ nằm trên cùng. Giải quyết bằng một khoá sắp xếp đứng trước: `CASE WHEN EXISTS(...) THEN 0 ELSE 1 END`.
- Lưu ý: phát hiện ở M12 — `ON DELETE CASCADE` ở DB chỉ xoá **record**, không xoá **file** trên đĩa. Với entity có file đính kèm bị cascade theo cha (Lesson theo Topic), Service phải đọc danh sách đường dẫn file **trước khi xoá** (sau khi xoá, record con đã biến mất theo cascade nên danh sách rỗng), rồi publish `FileDeletionEvent` cho từng file. Tương tự, khi upload phải tìm entity **trước** khi gọi `storeAudio`/`storeImage`, nếu không entity không tồn tại vẫn sinh ra file mồ côi.
- Lưu ý: phát hiện ở M12 — `GlobalExceptionHandler` chưa bắt `HttpRequestMethodNotSupportedException`, `HttpMediaTypeNotSupportedException`, `MultipartException` nên cả ba rơi vào handler `Exception` chung và trả nhầm `500` (VD gọi endpoint upload bằng body JSON). Đã vá bằng 3 handler riêng (`405`, `415`, `400`). Cùng loại lỗ hổng với bullet M11 phía trên. (M13: các handler vá từng cái này được thay bằng cách kế thừa `ResponseEntityExceptionHandler`, xem bullet M13 bên dưới; chỉ còn handler riêng cho `MultipartException` và `MethodArgumentTypeMismatchException`.)
- Lưu ý: phát hiện ở M12 — Spring chỉ tự chọn constructor khi class có đúng một constructor; nếu thêm constructor thứ hai (để giữ test cũ không phải sửa, như `FileStorageServiceImpl` 3 và 4 tham số), phải gắn `@Autowired` vào constructor mà Spring cần dùng, thiếu thì báo "No default constructor found". Ngoài ra, stub Mockito lồng nhau (`when(a).thenReturn(helper())` mà `helper()` bên trong lại gọi `when(...)`) ném `UnfinishedStubbingException` — tạo đối tượng mock ra biến riêng trước rồi mới stub.
- Lưu ý: phát hiện ở M12 (Frontend) — khối `catch` chỉ hiện một câu cố định (VD "Failed to load topics") che mất nguyên nhân thật: `truncate()` ném `TypeError` vì `Topic.description` là `null` (cột cho phép NULL) làm cả trang Admin báo lỗi mà không có dấu vết. Luôn `console.error('[tênHàm]', error)` và hiện `error.message` trong `catch`.
- Lưu ý: phát hiện ở M13 — `@ExceptionHandler(Exception.class)` trong `@RestControllerAdvice` **nuốt mọi exception chuẩn của Spring MVC chưa được khai báo riêng** (thiếu `@RequestParam` → `MissingServletRequestParameterException`, `@Size` trên tham số → `HandlerMethodValidationException`...) và trả `500`. Cơ chế: exception đi vào `ExceptionHandlerExceptionResolver` (các `@ExceptionHandler` của project) TRƯỚC `DefaultHandlerExceptionResolver` (nơi biết map sang 400/405/415...); handler `Exception.class` khớp mọi thứ nên resolver sau không bao giờ chạy tới. Đây là gốc của lỗi lặp lại ở M11 và M12. Giải pháp ở M13: `GlobalExceptionHandler extends ResponseEntityExceptionHandler` (lớp cha khai báo sẵn ~15 exception chuẩn với status đúng), ghi đè `handleExceptionInternal` để đổi body sang `ErrorResponse {message, status, timestamp}`, cộng 3 method giữ message theo hợp đồng cũ (`handleMethodArgumentNotValid` dạng `field:message`, `handleHttpMessageNotReadable`, `handleHandlerMethodValidationException`). Khi nhiều handler cùng khớp, Spring chọn handler có kiểu exception **gần nhất** trong cây kế thừa (vì vậy `MaxUploadSizeExceededException` ra `413` của lớp cha, không rơi vào handler `MultipartException` của project).
- Lưu ý: quyết định ở M13 — `DataIntegrityViolationException` không khớp constraint nào Service đã biết **giữ `500`** (không đổi thành `409`): Service đã tự bắt các constraint biết trước và đổi thành `ResourceConflictException`, nên vi phạm lạ lọt tới đây là bug phía server, trả `409` sẽ che mất nó. Mọi `5xx` (kể cả từ lớp cha như `MissingPathVariableException`) trả message cố định `Internal server error`, chi tiết chỉ nằm trong log (`log.error`). `HttpMessageNotReadable` giữ message `Malformed or missing request body` (là một phần hợp đồng API, không để câu chữ của Spring quyết định).
- Lưu ý: M13 — lỗi `401`/`403` sinh ra ở tầng Security Filter Chain, TRƯỚC khi tới Controller nên `@RestControllerAdvice` không bắt được. Phải cấu hình `AuthenticationEntryPoint` (`JsonAuthenticationEntryPoint`) và `AccessDeniedHandler` (`JsonAccessDeniedHandler`) trong `SecurityConfig.exceptionHandling(...)` để body có cùng shape `{message, status, timestamp}`. Message cố định, không dùng `ex.getMessage()`, không log username/token.
- Lưu ý: M13/M14 — integration test dùng `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + Testcontainers PostgreSQL (một container dùng chung qua lớp cha `PostgresIntegrationTestBase`, `static { start(); }`). Flyway chạy V1→V11 thật trên container nên migration cũng được kiểm chứng. Bẫy: sau một lần flush thất bại (vi phạm unique/FK) PostgreSQL đánh dấu transaction aborted nên câu ném lỗi phải là câu **cuối** của test; test cascade cần `em.clear()` trước khi xoá (nếu không Hibernate báo lỗi trước khi tới DB) và sau khi xoá (nếu không `findById` trả bản cũ); `Instant.now()` có nano giây nhưng `TIMESTAMPTZ` chỉ giữ micro giây nên cần `truncatedTo(MICROS)` khi so sánh. Test mới nên có "mutation check" (tạm phá logic, test phải đỏ rồi hoàn tác).
- Lưu ý: M14 — **cache hit bỏ qua toàn bộ thân method**, kể cả các check quyền/trạng thái bên trong. Với `@Cacheable` thì việc evict đúng là hàng rào DUY NHẤT. Phát hiện: `TopicCacheEvictionListener` từng chỉ evict `topics`/`topicDetails` nên sau khi xoá/archive Topic, `GET /api/topics/{id}/flashcards` vẫn trả flashcard cũ từ Redis (lỗi có sẵn từ M5). Đã vá: evict thêm `flashcardsByTopic::{topicId}`. Quy tắc mới: khi dữ liệu cache phụ thuộc thuộc tính của entity cha (trạng thái Topic), event của cha phải evict cache của con.
- Lưu ý: M14 — nguyên tắc "mặc định đóng" cho điều kiện lọc quyền hiển thị: entity mới mặc định `DRAFT` (Service đặt tường minh), `Specification` tách hai điểm vào `forLearners(...)` (cố định `PUBLISHED`, không có tham số status nên không thể gọi nhầm) và `forAdmin(..., status)`; query phía người học dùng derived query có tên nói rõ (`findByIdAndStatusAndTopic_Status`). Query không lọc status chỉ còn tồn tại cho Admin. Query phía người học không còn dùng thì xoá hẳn (compiler sẽ chỉ chỗ còn gọi).
- Lưu ý: M14 — Flyway ghi checksum từng migration đã chạy; **không sửa migration đã áp dụng vào DB thật**, phải thêm migration mới. Viết xong file SQL hoàn chỉnh rồi mới chạy bất cứ thứ gì có thể chạm DB thật (`spring-boot:run`); `mvnw test` an toàn vì dùng container tạo mới.
- Lưu ý: M14 — JPQL nối chuỗi Java thiếu khoảng trắng (`"... >= :minScore" + "AND ..."` → `:minScoreAND`) làm Spring không dựng được context, nên MỌI test `@DataJpaTest`/`@SpringBootTest` đỏ kể cả test không liên quan. Khi test hàng loạt đỏ vô lý, đọc lỗi khởi động context trước.

---

## 2. YÊU CẦU CHỨC NĂNG (FUNCTIONAL REQUIREMENTS)

### FR-1: Authentication & Authorization `[MVP]`
- FR-1.1: Đăng ký tài khoản (email, username, password) — mặc định role `USER`
- FR-1.2: Đăng nhập bằng **username hoặc email** (thử khớp `username` trước, nếu không có mới thử `email` — cùng 1 field input, không cần 2 ô riêng), trả JWT access token + refresh token. **ĐÃ CHỐT 23/09/2026 (M9)**
- FR-1.3: Middleware/filter xác thực JWT cho mọi API cần bảo vệ
- FR-1.4: Phân quyền endpoint theo role (`/api/admin/**` chỉ ADMIN)
- FR-1.5: Mã hóa mật khẩu bằng BCrypt
- FR-1.6 *(mới, M9)*: User xem lại thông tin profile của chính mình (`GET /api/users/me`)
- FR-1.7 *(mới, M9)*: User đổi mật khẩu, yêu cầu xác nhận đúng mật khẩu hiện tại trước khi đổi (`POST /api/users/me/password`)

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
  - **Cache key thực tế — ĐÃ CHỐT 29/09/2026 (M10):** `topics::{level|ALL}:{sort}:{size}` (VD `topics::BEGINNER:POPULAR:9`, `topics::ALL:NEWEST:20`), `topicDetails::{id}`, `flashcardsByTopic::{topicId}`. Chỉ cache khi `page = 0` **và** không có `keyword` (search tự do không cache, đúng tinh thần "chỉ cache query có lợi ích rõ ràng" ở trên). Định dạng key mới khác hẳn key cũ M5 (`topics::{size}`), nên không có rủi ro đọc nhầm entry cũ còn sót trong Redis chờ hết TTL.
  - Khi dữ liệu nguồn thay đổi (Admin CUD Topic/Flashcard), invalidate các cache liên quan
  - Không bắt buộc cache mọi biến thể search/filter/pagination — chỉ cache các query có lợi ích rõ ràng (VD: trang đầu, không filter), tránh biến Redis thành hàng nghìn cache key khó kiểm soát khi có FR-9 (search/filter)
  - **Dependency Topic ↔ Flashcard — ĐÃ CHỐT tại M5:** `TopicResponse` hiện **không** có field tổng hợp từ Flashcard (không có `flashcardCount` hay tương tự), nên cập nhật Flashcard **không** cần invalidate chéo cache Topic — 2 cache (`topics`/`topicDetails` và `flashcardsByTopic`) hoàn toàn độc lập. Nếu sau này `TopicResponse` được bổ sung field tổng hợp từ Flashcard, phải quay lại bổ sung evict chéo ở `FlashcardService` (evict thêm cache `topics`/`topicDetails` khi Flashcard CUD).
  - **CẬP NHẬT M14 (06/10/2026):** kết luận "2 cache độc lập" ở trên chỉ đúng khi `TopicResponse` không phụ thuộc Flashcard. Từ M14, danh sách Flashcard của một Topic phụ thuộc **trạng thái Topic** (Topic không `PUBLISHED` thì 404), nên `TopicChangedEvent(topicId != null)` evict thêm `flashcardsByTopic::{topicId}` (cạnh `topics` và `topicDetails::{id}`). Event tạo mới (`topicId = null`) chỉ clear `topics`. `TopicResponse` có thêm field `status`; sau khi triển khai nên `FLUSHALL` Redis dev một lần.

- **ĐÃ CHỐT 23/09/2026 (M9):** `GET /api/topics`, `GET /api/topics/{id}`, `GET /api/topics/{id}/flashcards`, `GET /api/topics/{id}/quizzes` chuyển thành **public** (không cần JWT) — cho phép khách xem trước khi đăng ký, đúng tinh thần trang chủ mời chào người dùng mới. Các hành động sâu hơn (làm Quiz, Chat AI, xem Progress cá nhân) vẫn bắt buộc đăng nhập như cũ. (Từ M14 chỉ trả nội dung `PUBLISHED`, xem FR-10.)

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

- **ĐÃ CHỐT 23/09/2026 (M9):** tên Quiz phải **duy nhất trong phạm vi 1 Topic**, không phân biệt hoa/thường (`"Quiz A"` và `"quiz a"` coi là trùng). Ràng buộc ở 2 lớp: `UNIQUE INDEX` trên `(topic_id, LOWER(title))` ở DB (migration V7) + check tường minh ở Service, trả `409 Conflict` khi trùng.
- **ĐÃ CHỐT 23/09/2026 (M9):** thêm `GET /api/admin/quizzes/{quizId}/questions` — response riêng (`AdminQuizQuestionResponse`) **có kèm** `correctAnswer`, chỉ dùng nội bộ cho Admin (điền sẵn form sửa câu hỏi). Khác hẳn `QuizQuestionPublicResponse` (User) luôn ẩn trường này theo đúng FR-4.5 — không nới lỏng FR-4.5, chỉ thêm 1 lối riêng dành cho ADMIN.

### FR-5: Theo dõi tiến độ `[MVP]`
- FR-5.1: User xem được tiến độ học của mình: `UserProgress` theo từng Topic (trạng thái hiện tại), và có thể xem lại lịch sử các lần làm quiz qua `QuizAttempt`
- FR-5.2: User chỉ được xem tiến độ/lịch sử của chính mình — không cho truyền `userId` để xem của người khác (áp dụng cùng nguyên tắc FR-2.7)
- FR-5.3: (Mở rộng, không bắt buộc) Thống kê tổng quan: số từ đã học, số quiz đã làm
- FR-5.4: Công thức tính `UserProgress.progressPercent`. **ĐÃ CHỐT 03/09/2026 (M3):** xem công thức đầy đủ ở mục 8. **CẬP NHẬT M14:** cả tử số (số Quiz "đạt") lẫn mẫu số (tổng số Quiz của Topic) chỉ đếm Quiz có `status = PUBLISHED` (`countAchievedQuizzesInTopic(..., status)` và `countByTopic_IdAndStatus`), để `progressPercent` không bao giờ vượt 100 khi có Quiz bị archive. Giá trị đã lưu không tính lại hàng loạt, chỉ cập nhật ở lần nộp bài kế tiếp.
- FR-5.5 *(mới, M9)*: `GET /api/users/me/quiz-attempts?page=&size=` — lịch sử làm bài **tổng hợp mọi Quiz** của chính User, sort mới nhất trước. Khác với `GET /api/quizzes/{id}/attempts` (chỉ giới hạn theo 1 Quiz cụ thể, đã có từ M3) — dùng cho trang Progress hiển thị hoạt động gần đây không phân biệt Quiz nào.

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
  - Giới hạn dung lượng: 5MB/file, chặn ở 2 lớp độc lập — `spring.servlet.multipart.max-file-size` (Spring tự chặn trước) và tự check lại thủ công trong `FileStorageService` (không tin 1 lớp validate duy nhất). Vượt giới hạn Spring multipart trả `413` (M13).
  - Kiểm tra nội dung file thật: dùng `javax.imageio.ImageIO` đọc thử ảnh (`ImageReader.read()`) — nếu không đọc được hoặc `formatName` không thuộc `jpeg`/`png`, từ chối dù `Content-Type` header client khai đúng. Không dùng Apache Tika (không cần thiết ở quy mô hiện tại)
  - Filename: `UUID.randomUUID()` + extension xác định từ **format ảnh thật đã đọc được** (`ImageReader.getFormatName()`), không dựa vào Content-Type header hay tên file gốc — do đó path traversal bị loại trừ hoàn toàn bằng thiết kế (UUID không thể chứa `../`)
  - Serve file: `WebMvcConfig` map `/uploads/**` ra thư mục thật qua `addResourceHandlers`, route này `permitAll()` trong `SecurityConfig` (ảnh cần xem được qua thẻ `<img>` không cần đính JWT)
  - File cũ bị ghi đè khi upload ảnh mới, hoặc khi xoá hẳn Topic/Flashcard/User, đều được dọn khỏi đĩa qua `FileDeletionEvent`/`FileDeletionListener` (`@TransactionalEventListener(AFTER_COMMIT)`, cùng pattern cache eviction đã chốt ở M5) — tránh race condition xoá file trước khi transaction DB thực sự commit
  - **Chỉ hỗ trợ đúng JPEG/PNG, không mở rộng thêm GIF/BMP/WBMP** dù `ImageIO` hỗ trợ sẵn — quyết định có chủ đích: các decoder ít phổ biến hơn (đặc biệt BMP/WBMP) có bề mặt tấn công lớn hơn JPEG/PNG khi xử lý file dị dạng, trong khi nhu cầu sản phẩm (ảnh minh hoạ tĩnh cho Flashcard/Topic/Avatar) không cần các format này
  - **Bug phát sinh sau khi chốt, đã fix:** `dto/auth/UserResponse.java` (dùng chung cho login/register/avatar upload) ban đầu không có field `avatarUrl` — khiến `POST /api/users/me/avatar` trả về 200 kèm file lưu đúng trên đĩa, nhưng response không cho biết URL ảnh vừa upload, vi phạm trực tiếp FR-6.2. Đã thêm `avatarUrl` vào `UserResponse`, nên giờ response của login/register cũng có thêm field này (giá trị `null` nếu user chưa từng upload avatar)
  - **Phạm vi (M12):** toàn bộ quy tắc ở FR-6 chỉ áp dụng cho **ảnh**. Audio của Dictation có quy tắc validate riêng (magic bytes MP3/WAV/OGG/M4A, 10MB, không dùng `ImageIO`), xem FR-7 — cùng nguyên tắc "không tin `Content-Type`/đuôi file do client gửi".

---

## 2.1. MỞ RỘNG SAU MVP `[v1.1-v1.2]` (Milestone M10–M14)

Sau khi hoàn thành MVP (M1–M9), tham khảo thêm sản phẩm Parroto, đã chọn 3 nhóm tính năng **ưu tiên làm trước** vì giá trị học Backend cao và độ phức tạp vừa sức để tiếp cận ngay sau MVP. Toàn bộ các nhóm còn lại (Shadowing, Voice Chat, Exam đầy đủ, YouTube Generator, Notification, Gamification...) **vẫn nằm trong kế hoạch**, được ghi ở mục 9 — Định hướng mở rộng dài hạn, làm sau khi nền tảng đã vững.

### FR-7: Dictation `[v1.1]` — ĐÃ IMPLEMENT 03/10/2026 (M12)
Người học nghe audio rồi gõ lại câu nghe được, hệ thống so sánh với transcript và chỉ ra lỗi.
- FR-7.1: Admin tạo `DictationLesson` thuộc 1 Topic (title, transcript, level); audio **upload riêng** sau khi tạo (xem quyết định về nguồn audio bên dưới)
- FR-7.2: User nghe lesson (audio của `DictationLesson`) — chỉ thấy bài đã có audio
- FR-7.3: User nhập nội dung nghe được
- FR-7.4: Hệ thống so sánh input với transcript, tính accuracy (%)
- FR-7.5: Highlight từ đúng/sai/thiếu/thừa trong kết quả trả về
- FR-7.6: Cho phép nghe lại, không giới hạn
- FR-7.7: Lưu kết quả từng lần làm vào `DictationResult`
- FR-7.8 *(bổ sung ngoài phạm vi gốc)*: danh mục bài Dictation cho người học — tìm kiếm, lọc theo Topic/Level/tiến độ, sắp xếp, phân trang, kèm số liệu cá nhân (số lần làm, điểm cao nhất, lần làm gần nhất)

**ĐÃ CHỐT 03/10/2026 (M12) — nguồn audio:** Admin **upload file** (MP3/WAV/OGG/M4A, tối đa 10MB) qua `POST /api/admin/dictation/{id}/audio`, khác `Flashcard.audioUrl` (URL external do Admin nhập). Lý do: mỗi Topic sẽ có rất nhiều bài, nhập link từng bài rất bất tiện. `mediaUrl` nullable cho tới khi upload; bài chưa có audio chỉ Admin thấy. Định dạng nhận dạng bằng **magic bytes** (không tin `Content-Type` hay đuôi file client gửi; đuôi file lưu lấy từ nội dung nhận dạng được). Giới hạn riêng `app.upload.audio-max-file-size-mb` = 10; `spring.servlet.multipart.max-file-size` nâng lên 10MB nhưng ảnh vẫn bị giới hạn 5MB ở tầng Service. Thay audio hoặc xoá bài thì file cũ được dọn qua `FileDeletionEvent` (AFTER_COMMIT) như ảnh Topic.

**ĐÃ CHỐT 03/10/2026 (M12) — không lộ transcript:** mọi response phía User trước khi nộp bài (`DictationLessonResponse`, `DictationCatalogItem`) **không có `transcript`**; transcript chỉ xuất hiện trong response của `submit`. Admin có endpoint riêng trả `transcript` để điền form sửa.

**ĐÃ CHỐT 03/10/2026 (M12) — thuật toán so sánh (FR-7.4/7.5), `DictationComparator`** (static pure function, không phụ thuộc Spring):
- Chuẩn hoá (**đổi ở M13, 05/10/2026**): hạ chữ thường, `’` → `'`, mọi ký tự không phải chữ/số (`\p{L}\p{N}`)/`'`/khoảng trắng — kể cả `-` và dấu câu — được thay bằng **khoảng trắng** (không còn xoá), cắt `'` ở đầu/cuối mỗi từ, tách theo khoảng trắng.
- So khớp mức từ bằng LCS (bảng quy hoạch động hướng suffix; quy ước khi hoà: ưu tiên bỏ từ của transcript trước, có test khoá lại). Giữa hai từ khớp liên tiếp: ghép từng cặp theo vị trí thành `WRONG` (`word` = từ user gõ, `expected` = từ transcript), phần transcript dư là `MISSING`, phần user dư là `EXTRA`.
- `accuracy = round1(100 × số từ CORRECT / max(số từ transcript, số từ user))` (làm tròn 1 chữ số thập phân) — gõ thừa nhiều từ không thể lấy điểm cao.
- Giới hạn: tối đa 1000 từ mỗi bên (`DictationComparator.MAX_WORDS`) và `@Size(max = 5000)` ký tự, vì bảng LCS tốn O(n×m). Admin cũng bị kiểm tra transcript (không rỗng sau chuẩn hoá, ≤ 1000 từ) khi tạo/sửa. Input chuẩn hoá ra rỗng → `400`.
- Giới hạn chấp nhận (M13): dấu đứng giữa hai chữ cũng tách từ (`hello,world` → `hello world`, 2 từ — đây là mục đích của thay đổi), nhưng hệ quả là `1,000` → `1` `000`, `3.5` → `3` `5`, `U.S.A.` → `u s a`. Số viết bằng chữ số và bằng chữ coi là khác nhau (`5` ≠ `five`). Vì vậy Admin nên viết số không có dấu phân cách (`1000`) và viết tắt không dùng dấu chấm (`USA`); hướng dẫn này đặt ở form Admin. Giới hạn 1000 từ được đếm **sau** chuẩn hoá. Dữ liệu cũ không cần migrate (`DictationResult.accuracy` lưu tại thời điểm làm bài, không tính lại).
- `DictationResult.userInput` lưu **nguyên văn** bản người dùng gõ; `accuracy` kiểu `Double`; `createdAt` do server lấy từ `Clock`.

**ĐÃ CHỐT 03/10/2026 (M12) — ownership & quy tắc xoá:** `DictationResult` chỉ lấy theo `userId` của JWT. Xoá Lesson đã có Result → `409`; xoá Topic mà **bất kỳ Lesson nào** của nó đã có Result → `409` (retrofit `TopicService.deleteTopic()` — Service cũ từ M2, dễ bỏ sót nhất của milestone, xem thêm mục 1.4). `dictation_lessons.topic_id` là `ON DELETE CASCADE` (nội dung Admin), `dictation_results` không cascade (lịch sử cá nhân). Vì cascade ở DB chỉ xoá record, `deleteTopic()` đọc danh sách `mediaUrl` của các Lesson **trước khi xoá** rồi publish một `FileDeletionEvent` cho mỗi file.

**ĐÃ CHỐT 03/10/2026 (M12) — danh mục `GET /api/dictation/lessons` (FR-7.8):** cần JWT. Tham số: `keyword` (≤ 50 ký tự, tìm trong tên bài **và tên Topic**, escape `%`/`_`/`\`), `topicId`, `level`, `progress` (`NEW` = người dùng hiện tại chưa làm bao giờ / `PRACTICED` = đã làm ít nhất một lần), `sort` (`newest` mặc định = `id` giảm dần vì `DictationLesson` không có `createdAt` / `title` / `recent` = bài vừa luyện lên đầu, bài chưa làm xuống cuối), `page`, `size`. Chỉ trả bài đã có audio. Mỗi phần tử có thêm `attempts`, `bestAccuracy`, `lastAttemptAt` của **chính người dùng đang đăng nhập** (chưa làm: `0`, `null`, `null`). Lọc/sắp xếp chạy ở server bằng `Specification` (`DictationLessonSpecifications`); số liệu cá nhân của cả trang lấy bằng **một** câu `GROUP BY` (không N+1; trang rỗng không gọi truy vấn thống kê). Không cache. Endpoint cũ `GET /api/topics/{id}/dictation` vẫn giữ nguyên. Hai bẫy khi viết `Specification` này (Sort ghi đè `orderBy`, `NULL` lên đầu khi `DESC`) ghi ở mục 1.4.

**Mã HTTP:** create/update/upload của Admin trả `200` (giống nhóm Admin hiện có); `submit` trả `201`; xoá trả `204`.

*Không làm trong v1.1: điều chỉnh tốc độ audio, AI giải thích lỗi chi tiết — để lại backlog.*

### FR-8: Vocabulary & SRS (Spaced Repetition) `[v1.1]`
Nâng cấp Flashcard hiện có thành hệ thống ôn từ vựng cá nhân hóa.
- FR-8.1: User lưu 1 Flashcard vào từ vựng cá nhân (`UserVocabulary`)
- FR-8.2: User đánh dấu "đã nhớ" / "chưa nhớ" sau mỗi lần ôn
- FR-8.3: Hệ thống tính `nextReviewAt` theo thuật toán SRS đơn giản. **Thuật toán cụ thể sẽ được quyết định khi implement M11** (không commit SM-2 hay bất kỳ thuật toán chuẩn nào ngay trong Requirements — có thể tự thiết kế kiểu khoảng cách tăng dần 1/3/7/14/30 ngày, đơn giản hơn và vẫn đủ để hiểu bản chất SRS)
- FR-8.4: API trả về danh sách từ cần ôn hôm nay (`nextReviewAt <= now`)
- FR-8.5: Từ bị đánh "chưa nhớ" nhiều lần được ưu tiên xuất hiện lại sớm hơn
- **ĐÃ CHỐT 29/09/2026 (M11) — thuật toán SRS (FR-8.3):** thang khoảng cách cố định 5 bậc `interval_level` (0–4) tương ứng 1/3/7/14/30 ngày, không dùng SM-2. Lưu từ mới: `status=NEW`, `intervalLevel=0`, `nextReviewAt = now` (đến hạn ôn ngay). Đánh "đã nhớ": `nextReviewAt = now + thang[intervalLevel hiện tại]`, `status` chuyển `KNOWN` nếu đang ở bậc 4 (30 ngày) trước khi tăng, ngược lại `LEARNING`, rồi `intervalLevel = min(intervalLevel + 1, 4)`. Đánh "chưa nhớ": reset `intervalLevel = 0`, `difficulty++`, `nextReviewAt = now + 1 ngày`, `status = LEARNING`. Toàn bộ tính bằng `Instant` thuần (không chia theo ranh giới ngày dương lịch), qua 1 bean `Clock` inject vào Service để unit test được với thời gian cố định.
- **ĐÃ CHỐT 29/09/2026 (M11) — FR-8.5 (ưu tiên từ hay quên):** không thêm cơ chế riêng, chỉ dùng `ORDER BY difficulty DESC, nextReviewAt ASC, id ASC` khi trả `GET /api/vocabulary/today` — từ bị đánh "chưa nhớ" nhiều lần có `difficulty` cao hơn nên nổi lên đầu danh sách.
- **ĐÃ CHỐT 29/09/2026 (M11) — lưu trùng:** `POST /api/vocabulary/{flashcardId}/save` trả `409 Conflict` nếu Flashcard đó đã có trong sổ từ của chính User (check `existsBy` trước, đồng thời bắt `DataIntegrityViolationException` theo tên constraint `uq_user_vocabulary_user_flashcard` để chặn race condition khi double-click).
- **ĐÃ CHỐT 29/09/2026 (M11) — retrofit rule "không cascade xoá lịch sử cá nhân" (mục 2.3):** `UserVocabulary` tham chiếu `Flashcard` (mà `Flashcard` tham chiếu `Topic`), nên cả `FlashcardService.deleteFlashcard()` lẫn `TopicService.deleteTopic()` đều phải thêm check `UserVocabularyRepository.existsByFlashcard_Id()`/`existsByFlashcard_Topic_Id()`, trả `409` nếu đã có User lưu từ liên quan — áp dụng đúng nguyên tắc đã có ở mục 2.3, chỉ là bổ sung thêm 1 điều kiện chặn mới phát sinh từ M11.
- **ĐÃ CHỐT 29/09/2026 (M11) — bổ sung ngoài phạm vi ban đầu, phát sinh khi FE test thực tế:** 3 endpoint mới không có trong bản Requirements gốc của FR-8, xem mục 5 (API Endpoints) để biết chi tiết: `GET /api/vocabulary/topics/{topicId}/saved-ids` (kiểm tra nhanh các Flashcard nào trong 1 Topic đã được User lưu, để FE hiện đúng trạng thái nút Save khi quay lại trang), `GET /api/vocabulary?page=&size=` (xem toàn bộ sổ từ đã lưu, không giới hạn theo hạn ôn — thiếu sót của bản gốc, không có cách nào xem lại sổ từ đầy đủ nếu chỉ có `/today`), `DELETE /api/vocabulary/{id}` (xoá 1 từ khỏi sổ, dùng ownership qua `findByIdAndUser_Id` như các API khác, trả 404 nếu không phải chủ sở hữu).

### FR-9: Search & Filter & Pagination `[v1.1]`
Áp dụng cho danh sách Topic/Flashcard khi dữ liệu lớn dần. Giữ đúng 4 tiêu chí này, chưa cần thêm filter khác (skill/difficulty/duration...) vì dữ liệu hiện tại chưa đủ lớn để các filter đó có ý nghĩa thực tế.
- FR-9.1: Search Topic theo từ khóa (title/description)
- FR-9.2: Filter theo `level`
- FR-9.3: Pagination áp dụng cho các API danh sách có khả năng tăng lớn; danh sách nhỏ có thể không cần pagination
- FR-9.4: Sort theo mới nhất / phổ biến (dựa trên lượt học)
- **ĐÃ CHỐT 29/09/2026 (M10):** `sort` chỉ nhận đúng 3 giá trị `newest` (mặc định) / `popular` / `title`, không phân biệt hoa/thường. Bỏ hẳn ý định sort theo `level` (Enum lưu String, sort trực tiếp ra sai thứ tự cấp độ — xem lưu ý mục 1.4) và sort Z-A (không cần thiết cho catalogue nhỏ).
- **ĐÃ CHỐT 29/09/2026 (M10) — "lượt học" cho `sort=popular`:** đếm số **người học riêng biệt** của Topic đó, dựa trên số bản ghi `UserProgress` (đã có `UNIQUE(user, topic)` sẵn từ M3, không cần counter/entity mới). Không tính lượt xem `GET /api/topics/{id}` (endpoint public, đếm ở đó sẽ ghi DB trên mỗi lượt đọc của khách và dễ bị spam). Đánh đổi đã biết: thứ hạng `popular` có thể "cũ" tối đa bằng TTL cache list (10 phút) sau khi có hoạt động học mới, vì tạo/update `UserProgress` không trigger evict cache `topics`.
- **ĐÃ CHỐT 29/09/2026 (M10) — index:** KHÔNG thêm index mới cho `Topic.title`/`level` ở M10 (khác dự kiến ban đầu ở mục 2.3) — B-tree không hỗ trợ `LIKE '%keyword%'` (wildcard đứng đầu), `level` chỉ có 3 giá trị nên index gần như vô ích, và `UNIQUE(title, level)` đã tự tạo sẵn 1 index dùng được một phần. Để dành lại nếu sau này cần, sẽ dùng `pg_trgm` (GIN) cho search thay vì B-tree thường.
- **ĐÃ CHỐT 29/09/2026 (M10) — giới hạn keyword:** `keyword` tối đa 50 ký tự, vượt quá trả `400`. Ký tự đặc biệt của SQL `LIKE` (`%`, `_`, `\`) được escape trước khi query, để gõ đúng các ký tự này không bị hiểu nhầm thành wildcard.

**Phạm vi áp dụng cụ thể — tránh nhầm lẫn đâu cần search đầy đủ, đâu chỉ cần phân trang:**
- **Search + Filter + Sort + Pagination đầy đủ:** `GET /api/topics` (đây là danh sách chính người dùng duyệt/tìm, xứng đáng đầu tư đầy đủ) và `GET /api/dictation/lessons` (M12 — danh mục bài Dictation, xem FR-7.8)
- **Chỉ Pagination đơn giản (page, size), không cần search/filter:** `GET /api/conversations`, `GET /api/quizzes/{id}/attempts`, `GET /api/vocabulary/today` (v1.1), lịch sử `DictationResult` (v1.1) — đây là các danh sách cá nhân, thường không lớn và không cần tìm kiếm phức tạp
- **Không cần pagination (danh sách nhỏ, cố định theo 1 Topic):** `GET /api/topics/{id}/flashcards`, `GET /api/topics/{id}/quizzes`

### FR-10: Trạng thái nội dung Draft / Published / Archived `[v1.2]` — ĐÃ IMPLEMENT 06/10/2026 (M14)
Thay cách làm "chỉ được xoá khi chưa có dữ liệu phụ thuộc" (mục 2.3) bằng một lối thoát có chủ đích: Admin **ẩn** nội dung đã có lịch sử học mà không mất dữ liệu, và soạn nháp trước khi công bố.
- FR-10.1: `Topic`, `Quiz`, `DictationLesson` có cột `status` (`DRAFT`/`PUBLISHED`/`ARCHIVED`, `@Enumerated(STRING)`). `Flashcard` **không** có status riêng, thừa hưởng từ Topic cha (Admin hiếm khi cần nháp từng thẻ; sửa thẻ đã có PUT).
- FR-10.2: **Trạng thái hiệu lực** = bản thân `PUBLISHED` VÀ Topic cha `PUBLISHED`. Không lưu lan trạng thái xuống con (archive Topic không ghi `ARCHIVED` cho N Quiz) để khôi phục Topic không làm mất thông tin Quiz nào vốn đã `DRAFT`.
- FR-10.3: Ai thấy gì. Khách/User chỉ thấy nội dung có trạng thái hiệu lực `PUBLISHED`; nội dung khác trả `404` (theo mục 2.3, không tiết lộ sự tồn tại). Admin thấy mọi trạng thái qua endpoint `/api/admin/**` riêng (không cache). Với endpoint danh sách, nội dung không `PUBLISHED` đơn giản không xuất hiện trong kết quả (không có `404`); `404` chỉ áp dụng khi truy cập trực tiếp một tài nguyên theo id hoặc theo Topic cha.
  - Lọc theo trạng thái hiệu lực: `GET /api/topics`, `/api/topics/{id}`, `/api/topics/{id}/flashcards`, `/api/topics/{id}/quizzes`, `GET /api/quizzes/{id}`, `POST /api/quizzes/{id}/submit`, `GET /api/topics/{id}/dictation`, `GET /api/dictation/lessons`, `POST /api/dictation/{id}/submit`, `POST /api/vocabulary/{flashcardId}/save`.
  - **Không lọc (lịch sử cá nhân vẫn xem được sau khi archive):** `GET /api/users/me/progress`, `GET /api/users/me/quiz-attempts`, `GET /api/quizzes/{id}/attempts`, toàn bộ `/api/vocabulary/*` trừ `save` (sổ từ vẫn ôn được vì SRS không được làm mất từ đã lưu), `GET /api/dictation/{id}/results`. Các `getMyResults` dùng `existsById` không lọc status là CÓ CHỦ ĐÍCH.
- FR-10.4: Giá trị mặc định. Entity và Service đặt `DRAFT` tường minh khi Admin tạo mới (hệ quả: tạo xong phải bấm Publish mới hiện với người học). Migration V11: dòng cũ → `PUBLISHED` (nội dung đang chạy không biến mất), `CHECK (status IN (...))` tên `ck_<bảng>_status`, rồi `DROP DEFAULT` để Entity luôn tự đặt giá trị. Không thêm index cho `status` (chỉ 3 giá trị, cùng lập luận với `level` ở FR-9).
- FR-10.5: Đổi trạng thái qua endpoint riêng `PATCH /api/admin/{topics|quizzes|dictation}/{id}/status` body `{status}` (không qua `PUT`, để sửa tiêu đề không vô tình đổi trạng thái). **Cho phép chuyển tự do giữa cả ba giá trị** (ĐÃ CHỐT 06/10/2026): lý do dùng archive là ẩn nội dung đã có lịch sử, và archive nhầm phải khôi phục được ngay. Điều kiện duy nhất, chỉ áp cho `PUBLISHED`: Quiz phải có ≥ 1 câu hỏi, Dictation phải có audio, nếu không `409`. Topic không có điều kiện. Giá trị lạ → `400`.
- FR-10.6: Archive và xoá cứng cùng tồn tại. Archive luôn được phép. Xoá cứng giữ nguyên quy tắc mục 2.3 (chưa có dữ liệu phụ thuộc, nếu không `409`); message `409` gợi ý archive thay vì xoá (với Flashcard gợi ý archive Topic cha).
- FR-10.7: Bất biến "Quiz `PUBLISHED` luôn có ≥ 1 câu hỏi": cổng vào là điều kiện publish (FR-10.5), cổng ra là `DELETE /api/admin/questions/{id}` trả `409` nếu đó là câu cuối của Quiz đang `PUBLISHED`. Quiz `DRAFT`/`ARCHIVED` xoá câu cuối được. **Giới hạn đã biết:** hai Admin cùng xoá hai câu cuối ở hai transaction song song có thể làm Quiz rỗng (race condition, chấp nhận vì chỉ Admin dùng; cách vá nếu cần: khoá bản ghi Quiz bằng `PESSIMISTIC_WRITE` trong `deleteQuestion`).
- FR-10.8: Cache. `TopicChangedEvent` (AFTER_COMMIT) phát khi đổi trạng thái Topic, listener evict `topics`, `topicDetails::{id}` và `flashcardsByTopic::{id}` (xem FR-3.5). Quiz và Dictation chưa có cache nên chưa phát event.
- FR-10.9: Response: `TopicResponse` và `QuizSummaryResponse` có thêm `status` (phía người học luôn là `PUBLISHED` nên không lộ gì); `AdminDictationLessonResponse` có thêm `status`.

---

## 2.2. QUY TẮC OWNERSHIP & SECURITY (áp dụng cho các entity gắn với dữ liệu cá nhân — liên quan FR-2, FR-5, FR-7, FR-8)

Nguyên tắc chung: **mọi entity gắn với 1 User cụ thể chỉ được chính User đó (qua JWT) truy cập** — không bao giờ tin tưởng `userId`/`ownerId` do client truyền lên qua query param hay body. Tổng hợp lại để tránh rải rác, thiếu sót khi code:

| Entity | Quy tắc ownership | Áp dụng ở |
|---|---|---|
| `Conversation` / `Message` | Chỉ chủ sở hữu (`conversation.user == JWT.userId`) mới xem/gửi được | FR-2.7 |
| `QuizAttempt` | Chỉ chủ sở hữu mới xem lịch sử làm quiz của mình | FR-5.2 |
| `UserProgress` | Chỉ chủ sở hữu mới xem tiến độ của mình | FR-5.2 |
| `UserVocabulary` (v1.1) | Chỉ chủ sở hữu mới xem/sửa sổ từ vựng cá nhân | mở rộng theo FR-5.2 |
| `DictationResult` (v1.1, M12) | Chỉ chủ sở hữu mới xem kết quả dictation của mình; số liệu cá nhân trong danh mục (`attempts`, `bestAccuracy`, `lastAttemptAt`) cũng chỉ tính theo user trong JWT | mở rộng theo FR-5.2, FR-7.8 |

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
- MVP chọn cách đơn giản: Admin **chỉ được `DELETE` Topic/Quiz/Flashcard/Question/Dictation Lesson khi resource đó chưa có dữ liệu phụ thuộc** (chưa có `QuizAttempt`/`UserProgress`/`UserVocabulary`/`DictationResult` nào tham chiếu tới, kể cả gián tiếp: Topic bị chặn nếu Flashcard hoặc Dictation Lesson của nó đã có dữ liệu cá nhân) — nếu đã có, API trả `409 Conflict`
- **ĐÃ THỰC HIỆN ở M14 (FR-10):** cơ chế trạng thái nội dung `DRAFT`/`PUBLISHED`/`ARCHIVED` đã được thêm như một lối thoát bổ sung, KHÔNG thay thế quy tắc `409` ở trên: xoá cứng vẫn bị chặn khi có dữ liệu phụ thuộc, Admin dùng archive để ẩn nội dung đó mà không mất lịch sử.

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
| NFR-3 | Có Global Exception Handler (`@RestControllerAdvice`, kế thừa `ResponseEntityExceptionHandler`) xử lý lỗi thống nhất; lỗi `401`/`403` từ tầng Security cũng trả cùng shape `{message, status, timestamp}` (M13) |
| NFR-4 | Có Validation (`@Valid`, Bean Validation) cho input |
| NFR-5 | Viết Unit Test (JUnit/Mockito) cho Service layer, tối thiểu các luồng chính. **Hoàn thành 13/09/2026 (M8):** 110 test case trên 10 class (5 Service chính bắt buộc + `FileStorageServiceImpl`/`ChatServiceImpl` optional + `JwtUtil`/2 `CacheEvictionListener` phát sinh trong lúc làm). Phạm vi: Unit test thuần, Mockito mock toàn bộ Repository/dependency, không dùng `@DataJpaTest`/Testcontainers (để dành M9 nếu cần integration test). Coverage tính theo nhánh logic (happy path + mọi nhánh throw exception), không chạy theo %. **Cập nhật M12 (03/10/2026):** tổng 198 test trên 14 class, chi tiết ở `Session_Summary.md` mục 6. **Cập nhật M14 (06/10/2026):** tổng **339 test**, gồm cả integration test trên PostgreSQL thật (Testcontainers: `DictationRepositoryTest`, `TopicRepositoryTest`, `QuizRepositoryTest`, `QuizAttemptRepositoryTest`), `GlobalExceptionHandlerTest` (MockMvc standalone), `TopicQueryParserTest`. Còn nợ: unit test cho `JsonAccessDeniedHandler`/`JsonAuthenticationEntryPoint`.|
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
 ├─ imageUrl: String
 ├─ createdAt: Instant   — ĐÃ CHỐT 29/09/2026 (M10), thêm qua migration V8, dùng cho sort=newest (FR-9.4); dòng cũ backfill = NOW() lúc migrate
 └─ status: Enum(DRAFT, PUBLISHED, ARCHIVED)   — M14, migration V11, mặc định khi tạo = DRAFT (FR-10)
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
 └─ audioUrl: String (nullable)   — CHỐT: URL external do Admin tự nhập (VD: link từ điển online có sẵn audio phát âm), KHÔNG phải file do hệ thống tự lưu trữ. FR-6 (file upload) chỉ áp dụng cho ảnh — không mở rộng sang audio ở MVP để tránh kéo thêm phạm vi không cần thiết (ngoại lệ từ M12: audio của Dictation là file upload thật, xem FR-7). (M14: không có status riêng, thừa hưởng trạng thái từ Topic, xem FR-10.1)

Quiz
 ├─ id: Long
 ├─ topic: Topic (ManyToOne)     — CHỐT: 1 Topic có thể có nhiều Quiz (VD: Travel → Vocabulary Quiz, Grammar Quiz, Conversation Quiz)
 ├─ title: String
 └─ status: Enum(DRAFT, PUBLISHED, ARCHIVED)   — M14, migration V11 (FR-10)
  [DB constraint: UNIQUE INDEX (topic_id, LOWER(title)) — chốt 23/09/2026 (M9), chặn trùng tên Quiz (không phân biệt hoa/thường) trong cùng 1 Topic]

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

DictationLesson — ĐÃ IMPLEMENT 03/10/2026 (M12), migration V10
 ├─ id: Long
 ├─ topic: Topic (ManyToOne, LAZY; FK ON DELETE CASCADE — nội dung Admin quản lý, giống Flashcard)
 ├─ title: String
 ├─ mediaUrl: String (nullable — NULL cho tới khi Admin upload audio; đường dẫn tương đối dạng /uploads/dictation/<uuid>.mp3)
 ├─ transcript: String (Text)
 ├─ level: Enum(BEGINNER, INTERMEDIATE, ADVANCED) — @Enumerated(EnumType.STRING). Không có createdAt: "mới nhất" = id lớn nhất
 └─ status: Enum(DRAFT, PUBLISHED, ARCHIVED) — M14, migration V11 (FR-10). Bài chưa có audio không publish được

DictationResult — ĐÃ IMPLEMENT 03/10/2026 (M12), migration V10
 ├─ id: Long
 ├─ user: User (ManyToOne; FK KHÔNG cascade — lịch sử cá nhân)
 ├─ lesson: DictationLesson (ManyToOne; FK KHÔNG cascade)
 ├─ userInput: String (Text — nguyên văn người dùng gõ)
 ├─ accuracy: Double (làm tròn 1 chữ số thập phân)
 └─ createdAt: Instant (server gán qua Clock)
 Index: (user_id, lesson_id, created_at DESC) cho lịch sử + truy vấn thống kê; lesson_id cho existsByLesson_Id / existsByLesson_Topic_Id

 UserVocabulary          — ĐÃ IMPLEMENT 29/09/2026 (M11), migration V9
 ├─ id: Long
 ├─ user: User (ManyToOne)
 ├─ flashcard: Flashcard (ManyToOne)
 ├─ status: Enum(NEW, LEARNING, KNOWN)
 ├─ reviewCount: Integer
 ├─ difficulty: Integer          — số lần bị đánh "chưa nhớ", dùng để ưu tiên sắp xếp (FR-8.5)
 ├─ intervalLevel: Integer       — bậc hiện tại trên thang 1/3/7/14/30 ngày (0..4), xem thuật toán SRS ở FR-8.3
 ├─ lastReviewedAt: Instant (nullable)
 └─ nextReviewAt: Instant
 [DB constraint: UNIQUE(user_id, flashcard_id) — 1 User chỉ có tối đa 1 bản ghi ôn tập cho mỗi Flashcard]
 [Index: (user_id, next_review_at) — phục vụ trực tiếp truy vấn GET /api/vocabulary/today]

```

**Lưu ý khi implement:** cân nhắc kỹ Lazy vs Eager Loading giữa các quan hệ trên (Phase 3) để tránh N+1, đặc biệt khi load `Conversation` kèm `Message`, hoặc `Topic` kèm `Flashcard`.

---

## 5. API ENDPOINTS

**Quy ước lỗi chung (M13):** mọi lỗi trả `{message, status, timestamp}`. `400` (body hỏng/sai kiểu/thiếu tham số/validate), `401` (thiếu hoặc sai JWT), `403` (sai role), `404`, `405`, `409`, `413` (upload vượt giới hạn dung lượng), `415`, `500` (message cố định `Internal server error`), `503` (AI provider).

### Auth
| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Đăng ký |
| POST | `/api/auth/login` | Public | Đăng nhập, trả JWT |
| POST | `/api/auth/refresh` | Public | Làm mới access token bằng refresh token (không cấp lại refresh token mới — dùng lại token cũ tới khi hết hạn 7 ngày) |

### Chat `[MVP]`
| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| POST | `/api/conversations` | USER | Tạo conversation mới |
| GET | `/api/conversations?page=&size=` | USER | Danh sách conversation của user, sort theo `updatedAt` giảm dần, phân trang đơn giản (không search — theo phạm vi FR-9) |
| GET | `/api/conversations/{id}` | USER | Chi tiết 1 conversation (title, thời gian) — chỉ nếu thuộc về user hiện tại (FR-2.7), ngược lại trả 404 (theo convention mục 2.3) |
| GET | `/api/conversations/{id}/messages` | USER | Lịch sử tin nhắn — chỉ nếu conversation thuộc về user hiện tại (FR-2.7), ngược lại trả 404 (theo convention mục 2.3) |
| POST | `/api/conversations/{id}/messages` | USER | Gửi tin nhắn qua REST — chỉ lưu Message của USER, KHÔNG trả về câu trả lời AI (câu trả lời AI luôn qua WebSocket, xem dòng WS bên dưới). Tồn tại làm phương án dự phòng — FE hiện tại (M9) không dùng route này để gửi tin, dùng STOMP publish thay thế hoàn toàn |
| WS | `/ws/chat` (STOMP) | USER | Gửi/nhận tin nhắn realtime — connection phải authenticate bằng JWT, backend kiểm tra ownership conversation trước khi xử lý send/subscribe (FR-2.7); backend tự đính kèm context lịch sử hội thoại (FR-2.8) |

### Topic & Flashcard (Admin quản lý nội dung) `[MVP]`
| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| POST | `/api/admin/topics` | ADMIN | Tạo topic |
| PUT | `/api/admin/topics/{id}` | ADMIN | Sửa topic |
| DELETE | `/api/admin/topics/{id}` | ADMIN | Xóa topic. `409` nếu đã có `UserProgress`, `QuizAttempt`, `UserVocabulary` (qua flashcard của topic) hoặc `DictationResult` (qua lesson của topic). Sau commit, ảnh topic và audio của các Dictation Lesson trong topic được dọn khỏi đĩa. Message 409 gợi ý archive thay vì xoá (FR-10.6). |
| POST | `/api/admin/topics/{id}/image` | ADMIN | Upload ảnh topic |
| POST | `/api/admin/topics/{id}/flashcards` | ADMIN | Thêm flashcard vào topic |
| PUT | `/api/admin/flashcards/{id}` | ADMIN | Sửa flashcard |
| DELETE | `/api/admin/flashcards/{id}` | ADMIN | Xóa flashcard |
| POST | `/api/admin/flashcards/{id}/image` | ADMIN | Upload ảnh minh họa cho flashcard |

### Topic & Flashcard (User học) `[MVP]`
| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| GET | `/api/topics?page=&size=&keyword=&level=&sort=` | **Public** | Danh sách topic — search/filter/sort/pagination đầy đủ theo FR-9, ĐÃ IMPLEMENT 29/09/2026 (M10). `sort`: `newest` (mặc định) / `popular` / `title`. `keyword` tối đa 50 ký tự, tìm trong `title`+`description`, không phân biệt hoa/thường |
| GET | `/api/topics/{id}` | **Public** | Chi tiết 1 topic. Chỉ Topic `PUBLISHED`, nếu không `404` (FR-10.3) |
| GET | `/api/topics/{id}/flashcards` | **Public** | Danh sách flashcard theo topic. Topic phải `PUBLISHED`, nếu không `404`; cache bị evict khi đổi trạng thái Topic |

### Quiz (Admin quản lý nội dung) `[MVP]`
| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| POST | `/api/admin/topics/{id}/quizzes` | ADMIN | Tạo quiz mới thuộc topic |
| PUT | `/api/admin/quizzes/{id}` | ADMIN | Sửa thông tin quiz |
| DELETE | `/api/admin/quizzes/{id}` | ADMIN | Xóa quiz. Message 409 gợi ý archive thay vì xoá (FR-10.6). |
| POST | `/api/admin/quizzes/{quizId}/questions` | ADMIN | Thêm câu hỏi mới (bao gồm `correctAnswer`) vào quiz |
| GET | `/api/admin/quizzes/{quizId}/questions` | ADMIN | Xem lại danh sách câu hỏi **kèm** `correctAnswer` (chỉ dùng điền sẵn form sửa) |
| PUT | `/api/admin/questions/{id}` | ADMIN | Sửa câu hỏi |
| DELETE | `/api/admin/questions/{id}` | ADMIN | Xóa câu hỏi. `409` nếu đó là câu hỏi cuối của Quiz đang `PUBLISHED` (FR-10.7) |

### Quiz (User làm bài) `[MVP]`
| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| GET | `/api/topics/{id}/quizzes` | **Public** | Danh sách quiz thuộc 1 topic (kèm `questionCount` — thêm M9, để FE báo trước quiz rỗng) |
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
| GET | `/api/users/me` | USER | Xem thông tin profile của chính mình |
| POST | `/api/users/me/avatar` | USER | Upload/cập nhật avatar cá nhân, dùng chung rule validate với FR-6 |
| POST | `/api/users/me/password` | USER | Đổi mật khẩu — yêu cầu đúng mật khẩu hiện tại (FR-1.7) |
| GET | `/api/users/me/quiz-attempts?page=&size=` | USER | Lịch sử làm bài tổng hợp mọi Quiz, mới nhất trước (FR-5.5) |

### Dictation — ĐÃ IMPLEMENT 03/10/2026 (M12) (FR-7)
| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| GET | `/api/admin/topics/{id}/dictation` | ADMIN | *(bổ sung ngoài phạm vi gốc)* Danh sách bài của topic, CÓ `transcript`, gồm cả bài chưa có audio — dùng điền form sửa |
| POST | `/api/admin/topics/{id}/dictation` | ADMIN | Tạo bài (`title`, `transcript`, `level`), `mediaUrl` = null (audio upload riêng). `200` |
| PUT | `/api/admin/dictation/{id}` | ADMIN | Sửa `title`/`transcript`/`level` (KHÔNG đụng `mediaUrl`) |
| DELETE | `/api/admin/dictation/{id}` | ADMIN | Xoá bài. `204`; `409` nếu đã có `DictationResult`. File audio dọn sau commit. Message 409 gợi ý archive thay vì xoá (FR-10.6). |
| POST | `/api/admin/dictation/{id}/audio` | ADMIN | *(bổ sung ngoài phạm vi gốc)* Upload audio multipart `file` (MP3/WAV/OGG/M4A, ≤ 10MB). Thay audio thì xoá file cũ sau commit |
| GET | `/api/topics/{id}/dictation` | USER (cần JWT) | Bài đã có audio của topic, KHÔNG có `transcript` (không phân trang — nhỏ). Chỉ bài `PUBLISHED` có audio của Topic `PUBLISHED` |
| GET | `/api/dictation/lessons?keyword=&topicId=&level=&progress=&sort=&page=&size=` | USER | *(bổ sung ngoài phạm vi gốc)* Danh mục bài kèm số liệu cá nhân (FR-7.8) |
| POST | `/api/dictation/{id}/submit` | USER | Body `{userInput}`. `201`. Trả accuracy + từng từ (`CORRECT`/`WRONG`/`MISSING`/`EXTRA`) + transcript, lưu `DictationResult` |
| GET | `/api/dictation/{id}/results?page=&size=` | USER | Lịch sử kết quả của chính mình cho bài này, mới nhất trước |

### Admin — đọc dữ liệu mọi trạng thái & đổi trạng thái — ĐÃ IMPLEMENT 06/10/2026 (M14) (FR-10)
| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| GET | `/api/admin/topics?page=&size=&keyword=&level=&sort=&status=` | ADMIN | Danh sách Topic mọi trạng thái, không cache. `status` rỗng = tất cả; giá trị lạ `400`. `keyword`/`level`/`sort` giống `GET /api/topics` (dùng chung `TopicQueryParser`) |
| GET | `/api/admin/topics/{id}` | ADMIN | Chi tiết Topic ở mọi trạng thái |
| GET | `/api/admin/topics/{id}/flashcards` | ADMIN | Flashcard của Topic ở mọi trạng thái, không cache |
| GET | `/api/admin/topics/{id}/quizzes` | ADMIN | Quiz mọi trạng thái, kèm `questionCount` và `status` |
| PATCH | `/api/admin/topics/{id}/status` | ADMIN | Body `{status}`. Chuyển tự do. Phát `TopicChangedEvent` |
| PATCH | `/api/admin/quizzes/{id}/status` | ADMIN | Body `{status}`. `409` nếu publish Quiz chưa có câu hỏi |
| PATCH | `/api/admin/dictation/{id}/status` | ADMIN | Body `{status}`. `409` nếu publish bài chưa có audio |

### Vocabulary/SRS — ĐÃ IMPLEMENT 29/09/2026 (M11) (FR-8)
| Method | Endpoint | Role | Mô tả |
|---|---|---|---|
| POST | `/api/vocabulary/{flashcardId}/save` | USER | Lưu từ vào sổ từ vựng cá nhân. `201` khi thành công, `409` nếu đã lưu rồi |
| PATCH | `/api/vocabulary/{id}/review` | USER | Đánh dấu đã/chưa nhớ sau khi ôn, `id` là id bản ghi `UserVocabulary` (không phải `flashcardId`). Body `{remembered: boolean}` |
| GET | `/api/vocabulary/today?page=&size=` | USER | Danh sách từ cần ôn hôm nay (`nextReviewAt <= now`), sort `difficulty DESC, nextReviewAt ASC, id ASC` |
| GET | `/api/vocabulary?page=&size=` | USER | *(bổ sung ngoài phạm vi gốc)* Toàn bộ sổ từ đã lưu, không lọc theo hạn ôn, mới lưu gần nhất trước |
| GET | `/api/vocabulary/topics/{topicId}/saved-ids` | USER | *(bổ sung ngoài phạm vi gốc)* Set `flashcardId` đã lưu của User trong 1 Topic, dùng cho FE hiện đúng trạng thái nút Save |
| DELETE | `/api/vocabulary/{id}` | USER | *(bổ sung ngoài phạm vi gốc)* Xoá 1 từ khỏi sổ cá nhân, `204` khi thành công |

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

*(Đây là cấu trúc **dự kiến** lúc thiết kế ban đầu, không phải snapshot thực tế — cấu trúc project thật tại từng thời điểm được track ở `Session_Summary.md` mục 5 (Frontend) và mục 6 (Backend), cập nhật sau mỗi milestone.)*

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
| M9 | Hoàn thiện `admin.html`, polish UI toàn bộ, test tổng thể end-to-end | **Hoàn thành 23/09/2026** — chi tiết đầy đủ ở mục 10 (mới, Frontend Design System) |
| **M10** | **(v1.1)** Search/Filter/Pagination cho Topic (FR-9) | **Hoàn thành 29/09/2026** |
| **M11** | **(v1.1)** Vocabulary & SRS — lưu từ, thuật toán ôn tập (FR-8) | **Hoàn thành 29/09/2026** — bổ sung 3 endpoint ngoài phạm vi gốc (xem mục 5, Vocabulary/SRS) |
| **M12** | **(v1.1)** Dictation — nghe, so sánh transcript, tính accuracy (FR-7) | **Hoàn thành 03/10/2026** — 9 endpoint (6 gốc + 3 bổ sung: danh sách Admin, upload audio, danh mục), migration V10, retrofit `TopicService.deleteTopic()`; tổng 198 test toàn dự án |
| **M13** | **(hardening)** `GlobalExceptionHandler` kế thừa `ResponseEntityExceptionHandler` (vá gốc lỗi 500 nhầm), JSON body cho `401`/`403`, tokenizer Dictation (dấu câu → khoảng trắng), dọn `innerHTML` ở `admin.html`/`vocabulary.html`, bộ integration test Testcontainers | **Hoàn thành 05/10/2026** |
| **M14** | **(v1.2)** Trạng thái nội dung Draft/Published/Archived (FR-10): migration V11, `PATCH .../status`, 4 endpoint đọc Admin, lọc theo trạng thái hiệu lực, vá cache, UI trạng thái ở `admin.html` | **Hoàn thành 06/10/2026** — tổng 339 test |

*Thứ tự này ưu tiên CRUD/domain logic (Auth → Topic → Quiz) trước, rồi mới tới 2 phần "khó" là tích hợp AI và Redis/WebSocket — tránh việc học nhiều kỹ thuật khó cùng lúc ở M2 như bản trước. Mỗi milestone backend đều có bước test Postman + dựng mini-frontend ngay sau đó, thay vì dồn toàn bộ tích hợp frontend vào 1 milestone cuối (dễ gây debug integration dồn cục, khó xác định lỗi nằm ở đâu). M1–M9 là MVP bắt buộc; M10–M14 làm ngay sau khi MVP chạy ổn định. Các tính năng mở rộng khác (mục 9) vẫn nằm trong kế hoạch dài hạn, làm sau M14 hoặc song song với việc quay lại Project 2 tùy thời gian thực tế.*

---

## 8. GHI CHÚ MỞ — QUYẾT ĐỊNH TẠI MILESTONE TƯƠNG ỨNG (KHÔNG CHẶN TIẾN ĐỘ HIỆN TẠI)

Các điểm dưới đây **cố ý chưa khóa cứng** ngay trong Requirements — không phải vì thiếu sót, mà vì quyết định quá sớm khi chưa code tới sẽ dễ phải sửa lại. Mỗi điểm đã gắn milestone cụ thể sẽ chốt khi tới lượt:

| Điểm còn mở | Chốt tại | Ghi chú |
|---|---|---|
| Database: PostgreSQL hay MySQL | ~~M1~~ **Đã chốt 01/09/2026: PostgreSQL** | Không ảnh hưởng thiết kế tổng thể, chỉ khác driver/config; PostgreSQL mở khóa dùng JSONB cho `QuizQuestion.options` |
| Có dùng Refresh Token hay chỉ Access Token đơn giản | ~~M1~~ **Đã chốt 01/09/2026: cả Access + Refresh Token** | Refresh Token lưu DB để có thể revoke, không chỉ dựa vào thời gian hết hạn |
| Kiểu lưu `QuizQuestion.options` (JSONB / `@ElementCollection`) | ~~M3~~ **Đã chốt 03/09/2026: `@ElementCollection`** | Không dùng JSONB dù PostgreSQL hỗ trợ — ưu tiên database-independent, đơn giản hơn cho quy mô hiện tại |
| Công thức tính `UserProgress.progressPercent` và điều kiện `COMPLETED` (FR-5.4) | ~~M3~~ **Đã chốt 03/09/2026** | 1 Quiz được coi là "đạt" ⟺ tồn tại ít nhất 1 QuizAttempt của User cho Quiz đó có score ≥ 70 progressPercent = round(100 × (số Quiz "đạt") / (tổng số Quiz thuộc Topic)) status = COMPLETED  ⟺  (số Quiz "đạt") == (tổng số Quiz thuộc Topic)  VÀ  tổng số Quiz > 0 status = IN_PROGRESS trong mọi trường hợp còn lại (đã có ít nhất 1 lần hoạt động nhưng chưa COMPLETED). (M14: cả hai vế chỉ đếm Quiz `PUBLISHED`, xem FR-5.4) |
| Định nghĩa "lượt học" dùng cho `sort=popular` (FR-9.4) | ~~M10~~ **Đã chốt 29/09/2026** | Đếm số người học riêng biệt qua `UserProgress` (đã có `UNIQUE(user, topic)` sẵn), không thêm counter/entity mới. Xem chi tiết ở FR-9.4 |
| Cách tạo `Conversation.title` | ~~M4~~ **Đã chốt 04/09/2026** | Tự động lấy từ nội dung `Message` đầu tiên của User trong Conversation, cắt tối đa 50 ký tự (thêm "..." nếu bị cắt). Chỉ set 1 lần, không ghi đè ở các message sau |
| Số lượng message lịch sử đính kèm làm AI context (FR-2.8) | ~~M4~~ **Đã chốt 04/09/2026** | N = 10 message gần nhất (bao gồm cả message User vừa gửi). Config qua `app.ai.context-message-limit` trong `application.yml`, không hardcode |
| Chọn cụ thể model AI trên xKiro | ~~M4~~ **Đã chốt 04/09/2026** | `qwen/qwen3.6-plus:free` — tier free để test không tốn phí, multilingual tốt (hợp cho response tiếng Việt + tiếng Anh). Model ID dạng `vendor/model`, đổi được qua config nếu cần chất lượng cao hơn |
| Format JSON chuẩn cho phản hồi AI (`reply`/`correction`/`explanation`) | ~~M4~~ **Đã chốt 04/09/2026** | Ép qua system prompt yêu cầu model trả đúng 1 JSON object 3 field. Parse bằng Jackson, có fallback: nếu parse lỗi (model trả kèm text/markdown fence dù đã cấm), coi toàn bộ raw text là `reply`, `correction`/`explanation` = null — không throw 500 vì lỗi format của model |
| Thuật toán SRS cụ thể (FR-8.3) | ~~M11~~ **Đã chốt 29/09/2026** | Thang khoảng cách cố định 5 bậc 1/3/7/14/30 ngày qua cột `intervalLevel`, không dùng SM-2. Chi tiết đầy đủ ở FR-8.3 |
| Nguồn audio của Dictation (FR-7.1) | ~~M12~~ **Đã chốt 03/10/2026: upload file** | Không dùng URL external như `Flashcard.audioUrl`; magic bytes, 10MB, dọn file qua `FileDeletionEvent`. Chi tiết ở FR-7 |
| Thuật toán so sánh Dictation + công thức accuracy (FR-7.4/7.5) | ~~M12~~ **Đã chốt 03/10/2026** | LCS mức từ, 4 nhãn `CORRECT/WRONG/MISSING/EXTRA`, `accuracy = round1(100 × đúng / max(n_transcript, n_user))`, ≤ 1000 từ. Chi tiết ở FR-7 |
| Quyền + cache của `GET /api/topics/{id}/dictation` và danh mục Dictation | ~~M12~~ **Đã chốt 03/10/2026** | Cần JWT (không `permitAll` như các `GET /api/topics/*`), response không có `transcript`, không cache (dữ liệu nhỏ và theo từng user) |
| Chiến lược Cache Eviction & Phòng ngừa Race Condition | ~~M5~~ **Đã chốt 05/09/2026** | Không dùng @CacheEvict trực tiếp trên method có @Transactional để tránh race condition evict-trước-commit (Redis bị xóa trước khi DB commit, dẫn đến request đọc lại đúng lúc đó sẽ cache lại dữ liệu cũ). Giải pháp: Dùng @TransactionalEventListener(phase = AFTER_COMMIT) lắng nghe Domain Event do Service phát ra sau khi DB update thành công, rồi xóa cache thủ công qua CacheManager. Nếu Transaction bị Rollback, event xóa cache hoàn toàn bị hủy, giữ an toàn tuyệt đối cho Redis Cache. |
| Kiến trúc WebSocket Streaming (FR-2.5/FR-2.7) | ~~M6~~ **Đã chốt 08/09/2026** | **Ownership (FR-2.7) áp dụng 2 điểm khác REST:** xác thực JWT ở STOMP CONNECT (qua `ChannelInterceptor`, không qua `JwtAuthenticationFilter` cũ vì STOMP frame sau CONNECT không đi qua HTTP filter chain), check ownership `Conversation` ở STOMP SUBSCRIBE (method `chatService.isOwner()` mới thêm). **Lỗi ở tầng ChannelInterceptor/subscribe callback không đi qua `@MessageExceptionHandler`** (chỉ bắt exception đồng bộ trong `@MessageMapping`) — cần `StompSubProtocolErrorHandler` riêng để trả STOMP ERROR frame rõ ràng trước khi Spring đóng kết nối (đóng kết nối là hành vi đúng chuẩn STOMP, không tránh được). **Tách reply/meta khi stream:** xem chi tiết ở FR-2.5. **Known limitation chấp nhận:** `save()` JPA là blocking call chạy trên Reactor event loop thread trong `sendMessageStream()` — chấp nhận ở quy mô hiện tại, cân nhắc `Schedulers.boundedElastic()` nếu traffic tăng lớn. |
| Chiến lược xử lý lỗi toàn cục (M13) | ~~M13~~ **Đã chốt 05/10/2026** | Kế thừa `ResponseEntityExceptionHandler`, ghi đè format và 3 message theo hợp đồng cũ; `DataIntegrityViolationException` lạ giữ 500; `5xx` message cố định; upload quá cỡ 413. Chi tiết ở mục 1.4 |
| Phạm vi `status` và quy tắc hiển thị (M14) | ~~M14~~ **Đã chốt 06/10/2026** | 3 entity có status, Flashcard thừa hưởng Topic; trạng thái hiệu lực = bản thân + Topic cha `PUBLISHED`; lịch sử cá nhân vẫn xem được. Chi tiết FR-10 |
| Cho phép chuyển trạng thái tự do hay chặn một số cạnh (M14) | ~~M14~~ **Đã chốt 06/10/2026: tự do** | Archive nhầm phải khôi phục dễ; điều kiện duy nhất áp cho `PUBLISHED` (Quiz có câu hỏi, Dictation có audio) |
| Giá trị `status` của dữ liệu cũ và dữ liệu mới (M14) | ~~M14~~ **Đã chốt 06/10/2026** | Dòng cũ `PUBLISHED` (V11), dòng tạo mới `DRAFT` |


---

## 9. ĐỊNH HƯỚNG MỞ RỘNG DÀI HẠN (v2+ — SAU M14)

Tham khảo từ [parroto.app](https://parroto.app/vi) (28/08/2026). Đây là **roadmap thật sự**, không phải danh sách loại bỏ — vẫn muốn làm, nhưng **sau khi nền tảng (M1–M14) đã vững**, vì mỗi nhóm dưới đây đòi hỏi kỹ thuật/hạ tầng riêng khá nặng, nên cần Backend đã chắc mới bắt đầu để tránh vỡ kiến trúc giữa chừng. Thứ tự trong bảng là gợi ý độ ưu tiên/độ khó tăng dần khi quay lại:

| Nhóm | Ghi chú kỹ thuật khi triển khai |
|---|---|
| ~~Admin Content Management (status Draft/Published/Archived)~~ | **ĐÃ LÀM ở M14 (FR-10)**. Phần còn lại nếu cần: lịch hẹn công bố, lịch sử thay đổi trạng thái |
| User Profile mở rộng (statistics, achievements) | Mở rộng tự nhiên từ entity `User`, ghép cùng Gamification |
| Gamification (XP, streak, leaderboard, achievement) | Cần thêm entity `Achievement`, `UserAchievement`; leaderboard nên cache Redis (sorted set) |
| Learning Profile & Personalized Path | Cần đủ dữ liệu tích lũy từ Progress/Vocab để gợi ý có ý nghĩa — nên làm sau khi có user thật dùng thử |
| Notification hệ thống | Cần Spring Scheduler + tích hợp email/push, thêm hạ tầng gửi thư |
| Exam Preparation (IELTS/TOEIC/TOEFL) | Entity `Exam → ExamPart → Question`, chủ yếu là CRUD nội dung lớn + Timer/Auto-save |
| AI Lesson Generator từ YouTube | Cần kiểm tra kỹ giới hạn API lấy transcript YouTube trước khi làm, rủi ro kỹ thuật/pháp lý |
| Shadowing (chấm phát âm, intonation, rhythm) | Cần tích hợp Speech-to-Text API + xử lý audio — nên khảo sát provider (Whisper API, Google STT...) trước |
| Community Speaking / Voice Chat (WebRTC) | Khó nhất — cần signaling server, WebRTC, matching queue; nên làm cuối cùng |

*Khi bắt đầu bất kỳ nhóm nào ở trên, quay lại bổ sung FR/entity/API chi tiết vào tài liệu này trước khi code — giữ đúng nguyên tắc "thiết kế trước, code sau" đã áp dụng xuyên suốt.*

## 10. FRONTEND DESIGN SYSTEM (chốt tại M9)

Tài liệu tham chiếu bắt buộc khi code thêm trang mới — giữ đúng convention dưới đây để trang mới không lệch hẳn với 10 trang hiện có.

### 10.1. Bảng màu (Duolingo-inspired)

Token giữ nguyên tên (Material Design 3 style: `primary`/`secondary`/`tertiary`/`error` + biến thể `-container`/`-fixed`/`-fixed-dim`/`on-*`), chỉ đổi giá trị màu. Định nghĩa qua **CSS custom property dạng RGB triplet** (không kèm hàm `rgb()`) trong `css/style.css`, ánh xạ vào Tailwind qua `tailwind-config.js` bằng cú pháp `rgb(var(--color-x) / <alpha-value>)` — cách này giúp 1 lần đổi `:root.dark` là đổi màu toàn site, không phải sửa lại từng trang.

- `primary` = xanh lá (#58CC02 sáng / #7AE02D tối) — CTA chính, hoàn thành, tiến độ
- `secondary` = xanh dương (#1CB0F6 sáng) — thông tin phụ, AI Tutor avatar
- `tertiary` = cam (#FF9600 sáng) — thành tích/gợi ý tích cực (VD: Grammar Tip)
- `error` = đỏ (#FF4B4B sáng)
- Nền: trắng thuần ở light mode, xám đen ở dark mode (không dùng xanh nhạt như bản gốc M1-M8)

Giá trị đầy đủ nằm trong khối `:root { --color-*: ... }` và `:root.dark { --color-*: ... }` ở đầu `css/style.css`.

### 10.2. Component signature — "nút nhấn 3D"

Đặc trưng riêng của phong cách này, áp dụng cho mọi nút hành động chính (không áp cho nút phụ/Cancel):
- Class `btn-3d` (định nghĩa trong `style.css`: `border-bottom-width: 4px`, `active:` giảm về 0 + dịch `translate-y`)
- Kèm border màu đậm hơn nền 1 bậc, dùng token riêng `*-shadow` (VD: `border-primary-shadow` cho nút `bg-primary`)
- Bo góc lớn hơn mặc định cũ: `rounded-2xl`/`rounded-xl` thay vì `rounded-lg`

Ví dụ chuẩn: `class="btn-3d px-6 py-3 bg-primary border-primary-shadow text-on-primary rounded-2xl hover:brightness-105 transition-all"`

### 10.3. Dark mode

- Cơ chế: class `.dark` trên `<html>`, toggle qua `js/theme.js` (`getTheme()`/`applyTheme()`/`toggleTheme()`/`initTheme()`), lưu lựa chọn ở `localStorage` key `linguistai_theme`.
- **Script chống nháy bắt buộc** ở đầu `<head>` của **mọi trang**, đặt trước `tailwind-config.js`:
```html
<script>
  if (localStorage.getItem('linguistai_theme') === 'dark' ||
      (!localStorage.getItem('linguistai_theme') && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
    document.documentElement.classList.add('dark');
  }
</script>
```
- Component nào dùng thư viện ngoài có palette riêng (VD: Tailwind Typography `prose` cho Markdown AI chat) phải thêm `dark:prose-invert` tường minh — không tự động ăn theo token màu của project.

### 10.4. Cấu trúc khung sườn trang đã đăng nhập (app shell)

Mọi trang cần sidebar+topbar (tức là mọi trang trừ `index.html`) **bắt buộc** đúng cấu trúc `<body>` sau:
```html
<body class="...">
    <div id="app-sidebar-container"></div>
    <div id="app-content" class="min-h-screen flex flex-col">
        <div id="app-topbar-container"></div>

        <main class="flex-grow ...">
            <!-- nội dung trang -->
        </main>

        <div id="app-footer-container"></div>
    </div>

    <script type="module">
        import { checkAuth } from './js/auth.js'; // hoặc checkAdmin cho trang admin
        import { initNavBar } from './js/navbar.js';
        import { initFooter } from './js/footer.js';

        checkAuth();
        initNavBar('<activePage>'); // 'chat' | 'topics' | 'vocabulary' | 'dictation' | 'progress' | 'admin' | '' (profile không có tab active)
        initFooter();
        // ...
    </script>
</body>
```
3 `<div>` container (`app-sidebar-container`, `app-topbar-container`, `app-footer-container`) là **id bắt buộc** — `navbar.js`/`footer.js` tìm đúng các id này để render vào, đổi tên sẽ khiến trang mất trắng điều hướng mà không có lỗi rõ ràng.

`index.html` là **ngoại lệ** — không dùng `footer.js` (footer riêng, nhiều cột, vì đây là trang marketing công khai cần nhiều thông tin hơn các trang app nội bộ) và có `<header>` cố định riêng thay vì sidebar (vì đây là trang landing, không phải app có nhiều mục điều hướng).

Trang PUBLIC (khách xem được, như `topics.html`/`topic-detail.html`) bỏ hẳn dòng `checkAuth()` — không gọi hàm này, để `navbar.js` tự nhận diện không có `user` và render đúng nhánh khách.

### 10.5. `navbar.js` — sidebar thu gọn được + phân nhánh khách/đã đăng nhập

- Nếu `getUser()` trả `null` (khách chưa đăng nhập): trang có đủ 2 khung `#app-sidebar-container` + `#app-topbar-container` (như `topic-detail.html`) → `renderGuestShell()`: **vẫn hiện sidebar đầy đủ** để khách thấy các chức năng. Mục công khai (Topics) là link thường; mục cần tài khoản (Chat, My Words, Dictation, Progress) là nút có biểu tượng ổ khoá, bấm vào mở modal đăng nhập kèm `redirectTo` = trang đích (xem 10.6). Có thẻ "Unlock everything" cuối sidebar, không hiện mục Admin. Trang không có đủ 2 khung (ví dụ landing page) → `renderGuestNavBar()`: topbar đơn giản như cũ, nút "Log In".
- Nếu đã đăng nhập → sidebar trái (`#appSidebar`, có thể thu gọn còn icon qua nút mũi tên, trạng thái lưu `localStorage` key `linguistai_sidebar_collapsed`) + topbar riêng (theme toggle, avatar, logout). Trên mobile, sidebar biến thành drawer trượt ra (mở qua hamburger trên topbar).
- **Lưu ý xung đột:** `chat.html` có sidebar riêng thứ 2 (danh sách Conversation, `#conversationsSidebar`) — đây KHÔNG phải sidebar điều hướng chung, chỉ nội dung riêng của trang Chat, có hamburger mở/đóng độc lập nằm trong Chat Header (khác hamburger mở sidebar điều hướng chung nằm trên topbar).

### 10.6. Modal Login/Register dùng chung (`authModal.js`)

- Không còn `login.html`/`register.html` riêng — mọi trang khách truy cập được (`index.html`, `topics.html`, `topic-detail.html`) gọi `initAuthModal()` 1 lần lúc load, chèn modal vào cuối `<body>`.
- Mở modal: gắn `data-open-login`/`data-open-register` vào bất kỳ `<button>` nào (authModal.js tự quét toàn trang), hoặc dispatch `window.dispatchEvent(new CustomEvent('open-auth-modal', { detail: 'login' }))` từ nơi khác (navbar khách dùng cách này).
- **`redirectTo` (M12):** event `open-auth-modal` nhận `detail` là chuỗi (`'login'`/`'register'`) **hoặc** `{ mode, redirectTo }`. Có `redirectTo` thì đăng nhập thành công sẽ chuyển tới đó thay vì `chat.html` (mặc định). Điểm đến chỉ sống trong lúc modal đang mở: đóng modal (✕/Esc/bấm nền) thì bị xoá, mỗi lần mở đều đặt lại — mở không kèm `redirectTo` luôn về `chat.html`. Chỉ nhận tên trang `.html` tương đối kèm query (regex `SAFE_REDIRECT`), giá trị khác (URL tuyệt đối, `//host`) bị bỏ qua để chặn open redirect. Dùng ở `topic-detail.html` (thẻ Quiz/Dictation, nút lưu từ) và sidebar khách.
- `checkAuth()`/`checkAdmin()` khi chưa đăng nhập → redirect `index.html?auth=login` (tự mở modal qua query param) — **không** dùng `?auth=login` cho hành động logout chủ động (chỉ về `index.html` trơn).

### 10.7. Module JS dùng chung khác

| File | Vai trò |
|---|---|
| `config.js` | `API_BASE_URL` (điểm duy nhất cần sửa khi đổi domain Backend/ngrok URL) + `imageSrc(path)` (nối domain vào đường dẫn ảnh tương đối Backend trả về) + `audioSrc(path)` (M12 — như `imageSrc` nhưng trả chuỗi rỗng khi không có đường dẫn, vì audio rỗng phải là rỗng, không có placeholder) |
| `ui.js` | Hàm dựng giao diện dùng chung: `el(tag, class, text)` (tạo phần tử bằng `textContent`, dữ liệu server/người dùng KHÔNG đi qua `innerHTML`), `buildLevelBadge`, `buildPagerButton`/`buildPageInfo`, `createSelect` (dropdown tự vẽ có ARIA + bàn phím, thay `<select>` gốc; dùng ở `topics.html`, `admin.html`, `dictation.html`), `createFilterBar`, `createSearchBox`, `LEVEL_FILTER_OPTIONS`. **M14:** `STATUS_FILTER_OPTIONS`, `buildStatusBadge(status)` (icon + chữ + viền, không chỉ dựa vào màu: Draft xám nét đứt, Published xanh lá đặc, Archived cam đặc), `buildStatusSelect({current, label, onChange, onError})` (dropdown đổi trạng thái trong dòng danh sách; menu `position: fixed` gắn vào `<body>` để không bị bảng/khung `overflow` cắt, tự lật lên trên khi sát mép dưới, chỉ một menu mở tại một thời điểm; `onChange` async, ném lỗi thì giữ nguyên giá trị cũ) |
| `topicPicker.js` | `createTopicPicker({ container, onChange, source, ... })` — combobox chọn Topic có tìm kiếm, tối đa 8 kết quả. **M14:** tham số `source`: `'public'` (mặc định, gọi `GET /api/topics`, dùng ở `dictation.html`) hoặc `'admin'` (gọi `GET /api/admin/topics`, thấy cả Topic nháp/archived kèm huy hiệu trạng thái, dùng ở `admin.html`). API: `getValue`, `setValue`, `setValueById`, `focus`, `destroy` |
| `toast.js` | `showToast(message, type)` — dùng cho hành động tức thời (submit, upload, xoá). KHÔNG dùng cho lỗi/rỗng của 1 khối nội dung chính — trường hợp đó viết inline text ngay tại khối đó (người dùng luôn thấy, không trôi mất sau 3 giây) |
| `chat.js` | `connectChat()`/`subscribeConversation()`/`sendChatMessage()`/`unsubscribeConversation()` — STOMP over SockJS, tách riêng khỏi `chat.html` |
| `footer.js` | `initFooter()` — footer dùng chung cho mọi trang app (trừ `index.html`) |
| `theme.js` | Dark/light mode, xem mục 10.3 |
| `api.js` | Engine `request()` gắn `err.status` vào lỗi (M14) để trang phân biệt `404` ("không còn tồn tại") với lỗi khác. Hàm Admin dùng endpoint `/api/admin/**` (`adminGetTopics`, `adminGetTopic`, `adminGetFlashcards`, `adminGetQuizzes`, `adminChange{Topic|Quiz|Dictation}Status`), KHÔNG dùng lại API public vì API public giờ ẩn bản nháp |

### 10.8. Skeleton loading

Mọi khu vực nội dung chính chờ API phải có skeleton (khung `div.skeleton` — shimmer animation định nghĩa trong `style.css`) thay vì "Loading..." chữ hoặc để trắng, hiển thị ngay khi bắt đầu gọi API, tự bị ghi đè khi có data thật.

### 10.9. Backlog và giới hạn đã biết (FE + BE), chưa xử lý

- ~~`topics.html`: 3 ô Search/Level/Sort hiện chưa có tác dụng lọc thật~~ **Đã nối thật 29/09/2026 (M10)** — có debounce 300ms cho ô search, guard chống response trả về trễ ghi đè kết quả mới hơn, `maxlength=50` khớp giới hạn Backend.
- `vocabulary.html` (M11): tab "Due Today" giữ `queue` trong bộ nhớ trình duyệt, không tự đồng bộ real-time nếu dữ liệu đổi từ nguồn khác (VD: xoá 1 từ ở tab "All Words" khi từ đó đang nằm trong `queue` của tab Due — đã vá bằng cách đồng bộ thủ công `queue` ngay trong `handleDeleteWord`, nhưng đây vẫn là state 2 nơi cần giữ khớp tay, không phải nguồn dữ liệu single source of truth thật sự).
- Accessibility nâng cao (skip link, `focus-visible` toàn diện, `aria-hidden` cho icon trang trí) mới chỉ áp dụng đầy đủ cho `index.html` (qua audit Impeccable) — các trang còn lại (kể cả `vocabulary.html`, `dictation.html`) chưa rà theo cùng chuẩn, để dành đợt polish sau nếu cần. (Các component M12 `createSelect`/`topicPicker` đã có ARIA combobox/listbox + bàn phím.)
- Dictation: thẻ `<audio>` không gửi được header `ngrok-skip-browser-warning` như `fetch` (đã test phát được qua ngrok trên trình duyệt desktop); `.m4a` chưa kiểm chứng trên Safari — nếu lỗi, bỏ M4A khỏi whitelist `FileStorageServiceImpl.detectAudioExtension`.
- Dictation backend: nếu transaction rollback sau khi `storeAudio` đã ghi file thì file mới thành mồ côi (cùng giới hạn đã biết với `updateTopicImage` ở M7, tần suất rất thấp); file văn bản UTF-16 (bắt đầu `FF FE`) lọt qua nhận dạng MP3 frame-sync (chỉ Admin upload, chỉ phục vụ như audio nên rủi ro thấp).
- `progress.html`: thẻ Topic đã archive vẫn bấm được, vào `topic-detail.html` sẽ thấy "This topic is no longer available" (xử lý bằng `error.status === 404`). Cải tiến để dành: thêm cờ `topicAvailable` vào `UserProgressResponse` để thẻ xám đi và gắn nhãn "Archived" ngay trên danh sách.
- `progress.html` còn dựng thẻ bằng `innerHTML` (tên Topic do Admin nhập), chưa chuyển sang `el()`. `topics.html` cũng còn dựng thẻ Topic bằng `innerHTML`.
- Không có test tự động cho Controller và FE; các luồng đó kiểm bằng bộ Postman và kiểm tay.
- Chưa có unit test cho `JsonAccessDeniedHandler`/`JsonAuthenticationEntryPoint`; chưa có mutation check cho quy tắc "câu hỏi cuối" (`<= 1` → `<= 0`).
- Race condition khi hai Admin xoá hai câu hỏi cuối của cùng Quiz `PUBLISHED` (xem FR-10.7).

---

*Tài liệu này sẽ được cập nhật trạng thái Milestone khi tiến độ thay đổi. Dùng làm tài liệu tham chiếu chính trong suốt quá trình code Project 1.*
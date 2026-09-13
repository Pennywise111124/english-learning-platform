package com.example.englishlearningplatform.ai;

import com.example.englishlearningplatform.ai.dto.XkiroChatRequest;
import com.example.englishlearningplatform.ai.dto.XkiroChatResponse;
import com.example.englishlearningplatform.ai.dto.XkiroChatStreamChunk;
import com.example.englishlearningplatform.config.AiProperties;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AiClientImpl implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClientImpl.class);

    // Delimiter cố định để tách "reply thô" (gửi realtime) khỏi phần JSON meta
    // (correction/explanation, chỉ dùng sau khi stream xong) — tránh việc phải
    // stream JSON dở dang ra Client (sẽ lộ cú pháp {"reply": " thô lên UI).
    private static final String STREAM_DELIMITER = "\n---META---\n";

    private static final String SYSTEM_PROMPT = """
            You are an English tutor chatting with a Vietnamese learner.
            Always reply with a SINGLE JSON object and NOTHING else (no markdown fences, no extra text).
            The JSON object must have exactly these keys:
            - "reply": your natural conversational reply to the user, in English.
            - "correction": if the user's last message had a grammar/word-choice error, the corrected
              sentence; otherwise null.
            - "explanation": if "correction" is not null, a short explanation in Vietnamese of why the
              correction was needed; otherwise null.
            Do not wrap the JSON in markdown code fences. Do not add commentary before or after the JSON.
            """;

    // Khác SYSTEM_PROMPT ở chỗ: reply là PLAIN TEXT tự nhiên (để stream ra
    // Client từng chữ trông tự nhiên), KHÔNG bọc JSON — chỉ phần meta (sau
    // delimiter) mới là JSON.
    private static final String SYSTEM_PROMPT_STREAM = """
            You are an English tutor chatting with a Vietnamese learner.
            First, write your natural conversational reply to the user in plain English text
            (NOT JSON, no markdown fences) — this is what the user reads directly.
            Then, on a new section, write EXACTLY this delimiter on its own line: ---META---
            After the delimiter, output a SINGLE JSON object with exactly these keys:
            - "correction": if the user's last message had a grammar/word-choice error, the corrected
              sentence; otherwise null.
            - "explanation": if "correction" is not null, a short explanation in Vietnamese of why the
              correction was needed; otherwise null.
            Do not add anything after the JSON object. Do not wrap the JSON in markdown code fences.
            """;

    private final WebClient xkiroWebClient;
    private final AiProperties props;
    private final JsonMapper jsonMapper;

    public AiClientImpl(WebClient xkiroWebClient, AiProperties props, JsonMapper jsonMapper) {
        this.xkiroWebClient = xkiroWebClient;
        this.props = props;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public AiChatResult chat(List<AiChatMessage> history) {
        List<Map<String, String>> messages = buildMessages(SYSTEM_PROMPT, history);
        XkiroChatRequest requestBody = new XkiroChatRequest(props.getModel(), messages, false);

        XkiroChatResponse response;
        try {
            response = xkiroWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(XkiroChatResponse.class)
                    .retryWhen(Retry.backoff(props.getMaxRetries(), Duration.ofMillis(300))
                            .filter(this::isRetryable)
                            .onRetryExhaustedThrow((spec, signal) -> new AiProviderException(
                                    "xKiro không phản hồi sau " + props.getMaxRetries() + " lần thử lại",
                                    signal.failure())))
                    .block();
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gọi xKiro thất bại: {}", e.getMessage());
            throw new AiProviderException("Không thể lấy phản hồi từ AI provider", e);
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new AiProviderException("xKiro trả về response rỗng/không hợp lệ");
        }

        String rawContent = response.choices().get(0).message().content();
        return parseAiContent(rawContent);
    }

    @Override
    public Flux<AiStreamEvent> chatStream(List<AiChatMessage> history) {
        List<Map<String, String>> messages = buildMessages(SYSTEM_PROMPT_STREAM, history);
        XkiroChatRequest requestBody = new XkiroChatRequest(props.getModel(), messages, true);

        // State chia sẻ giữa các lần onNext — AN TOÀN vì Reactor đảm bảo onNext
        // của 1 subscription luôn chạy TUẦN TỰ (serialized), không parallel,
        // với 1 luồng SSE HTTP/1 duy nhất.
        StringBuilder pendingBuffer = new StringBuilder(); // chưa emit, có thể chứa 1 phần delimiter
        StringBuilder replyBuffer = new StringBuilder(); // toàn bộ reply đã emit (để build kết quả cuối)
        StringBuilder metaBuffer = new StringBuilder(); // phần sau delimiter, tích luỹ để parse JSON cuối
        AtomicBoolean delimiterPassed = new AtomicBoolean(false);

        Flux<AiStreamEvent> chunkEvents = xkiroWebClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                })
                .map(ServerSentEvent::data)
                .filter(Objects::nonNull)
                .takeWhile(data -> !"[DONE]".equals(data))
                .<String>handle((rawJson, sink) -> {
                    String delta = parseDeltaContent(rawJson);
                    if (delta != null && !delta.isEmpty()) {
                        sink.next(delta);
                    }
                })
                .concatMap(delta -> processDelta(delta, pendingBuffer, replyBuffer, metaBuffer, delimiterPassed));

        Flux<AiStreamEvent> finalEvent = Flux.defer(() -> {
            List<AiStreamEvent> events = new ArrayList<>();

            if (pendingBuffer.length() > 0) {
                String leftover = pendingBuffer.toString();
                replyBuffer.append(leftover);
                events.add(new AiStreamEvent.ReplyChunk(leftover));
                pendingBuffer.setLength(0);
            }

            AiChatResult result = buildFinalResult(replyBuffer.toString(), metaBuffer.toString());
            events.add(new AiStreamEvent.Complete(result));

            return Flux.fromIterable(events);
        });

        return chunkEvents.concatWith(finalEvent)
                .onErrorMap(this::wrapStreamError);
    }

    /**
     * Xử lý 1 mảnh delta mới nhận được: gộp vào pendingBuffer, dò delimiter.
     * Nếu delimiter đã qua rồi (delimiterPassed=true) -> mọi delta về sau đều
     * thuộc phần meta, không emit gì cho Client nữa.
     * Nếu chưa qua -> chỉ emit phần "chắc chắn an toàn" (không thể là 1 phần
     * của delimiter bị cắt ngang giữa 2 chunk liên tiếp), giữ lại
     * (độ dài delimiter - 1) ký tự cuối trong buffer để chờ chunk tiếp theo.
     */
    private Flux<AiStreamEvent> processDelta(String delta, StringBuilder pendingBuffer,
            StringBuilder replyBuffer, StringBuilder metaBuffer, AtomicBoolean delimiterPassed) {

        if (delimiterPassed.get()) {
            metaBuffer.append(delta);
            return Flux.empty();
        }

        pendingBuffer.append(delta);
        int delimiterIndex = pendingBuffer.indexOf(STREAM_DELIMITER);

        if (delimiterIndex >= 0) {
            String beforeDelimiter = pendingBuffer.substring(0, delimiterIndex);
            String afterDelimiter = pendingBuffer.substring(delimiterIndex + STREAM_DELIMITER.length());

            pendingBuffer.setLength(0);
            delimiterPassed.set(true);
            metaBuffer.append(afterDelimiter);

            if (beforeDelimiter.isEmpty()) {
                return Flux.empty();
            }
            replyBuffer.append(beforeDelimiter);
            return Flux.just(new AiStreamEvent.ReplyChunk(beforeDelimiter));
        }

        int safeLength = pendingBuffer.length() - (STREAM_DELIMITER.length() - 1);
        if (safeLength <= 0) {
            return Flux.empty(); // buffer còn ngắn hơn delimiter, chưa dám chắc an toàn
        }

        String safeToEmit = pendingBuffer.substring(0, safeLength);
        pendingBuffer.delete(0, safeLength);
        replyBuffer.append(safeToEmit);
        return Flux.just(new AiStreamEvent.ReplyChunk(safeToEmit));
    }

    private AiChatResult buildFinalResult(String replyText, String metaText) {
        String reply = replyText.trim();

        if (metaText.isBlank()) {
            // Model không theo đúng format (không có delimiter/meta) — coi như
            // không có correction, KHÔNG throw lỗi vì lỗi format của model
            // (đúng tinh thần fallback đã áp dụng ở parseAiContent()).
            return new AiChatResult(reply, null, null);
        }

        try {
            String cleaned = stripMarkdownFence(metaText);
            MetaOnly meta = jsonMapper.readValue(cleaned, MetaOnly.class);
            return new AiChatResult(reply, meta.correction(), meta.explanation());
        } catch (Exception e) {
            log.warn("Không parse được JSON meta từ model (stream). Raw meta: {}", metaText);
            return new AiChatResult(reply, null, null);
        }
    }

    // Record cục bộ chỉ dùng để parse phần JSON meta (không có "reply" vì
    // reply đã tách riêng ở luồng streaming).
    private record MetaOnly(String correction, String explanation) {
    }

    private String parseDeltaContent(String rawJson) {
        try {
            XkiroChatStreamChunk chunk = jsonMapper.readValue(rawJson, XkiroChatStreamChunk.class);
            if (chunk.choices() == null || chunk.choices().isEmpty()) {
                return null;
            }
            return chunk.choices().get(0).delta().content();
        } catch (Exception e) {
            log.warn("Không parse được 1 chunk SSE từ xKiro, bỏ qua chunk này: {}", rawJson);
            return null;
        }
    }

    private AiProviderException wrapStreamError(Throwable ex) {
        if (ex instanceof AiProviderException e) {
            return e;
        }
        log.error("Lỗi giữa lúc stream từ xKiro: {}", ex.getMessage());
        return new AiProviderException("Không thể stream phản hồi từ AI provider", ex);
    }

    private List<Map<String, String>> buildMessages(String systemPrompt, List<AiChatMessage> history) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (AiChatMessage m : history) {
            messages.add(Map.of("role", m.role(), "content", m.content()));
        }
        return messages;
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientResponseException wcre) {
            return wcre.getStatusCode().is5xxServerError();
        }
        return true;
    }

    private AiChatResult parseAiContent(String rawContent) {
        String cleaned = stripMarkdownFence(rawContent);
        try {
            return jsonMapper.readValue(cleaned, AiChatResult.class);
        } catch (Exception e) {
            log.warn("Không parse được JSON từ model, fallback dùng raw text làm reply. Raw: {}", rawContent);
            return new AiChatResult(rawContent == null ? "" : rawContent.trim(), null, null);
        }
    }

    private String stripMarkdownFence(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}
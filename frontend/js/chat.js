/**
 * chat.js — WebSocket (STOMP over SockJS) client cho AI Chat streaming.
 * Yêu cầu chat.html đã load 2 thư viện global trước: SockJS, StompJs (qua CDN).
 */

import { getToken } from './auth.js';
import { API_BASE_URL } from './config.js';

let client = null;
let connectedPromise = null;
let typingSub = null;
let doneSub = null;
let errorSub = null;

export function connectChat({ onError, onDisconnected } = {}) {
  if (connectedPromise) return connectedPromise;

  connectedPromise = new Promise((resolve, reject) => {
    client = new StompJs.Client({
      webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws/chat`),
      connectHeaders: { Authorization: `Bearer ${getToken()}` },
      reconnectDelay: 5000,
      onConnect: () => {
        // Kênh lỗi riêng cho user, subscribe 1 lần duy nhất cho cả phiên (không theo từng conversation)
        errorSub = client.subscribe('/user/queue/errors', (frame) => {
          if (!onError) return;
          try { onError(JSON.parse(frame.body)); } catch { onError({ message: frame.body }); }
        });
        resolve();
      },
      onStompError: (frame) => {
        const msg = frame.headers?.message || 'Lỗi kết nối WebSocket';
        reject(new Error(msg));
      },
      onWebSocketClose: () => {
        connectedPromise = null;
        if (onDisconnected) onDisconnected();
      },
    });
    client.activate();
  });

  return connectedPromise;
}

// Gọi mỗi khi chuyển sang xem 1 conversation khác — tự huỷ subscribe cũ trước khi subscribe mới
export function subscribeConversation(conversationId, { onTypingChunk, onDone } = {}) {
  unsubscribeConversation();

  if (!client || !client.connected) {
    console.error('[chat.js] Chưa kết nối WebSocket — gọi connectChat() và đợi resolve trước');
    return;
  }

  typingSub = client.subscribe(`/topic/conversations/${conversationId}/typing`, (frame) => {
    if (onTypingChunk) onTypingChunk(frame.body); // text thô, không phải JSON
  });

  doneSub = client.subscribe(`/topic/conversations/${conversationId}`, (frame) => {
    if (!onDone) return;
    try { onDone(JSON.parse(frame.body)); } catch { /* ignore */ }
  });
}

export function unsubscribeConversation() {
  if (typingSub) { typingSub.unsubscribe(); typingSub = null; }
  if (doneSub) { doneSub.unsubscribe(); doneSub = null; }
}

export function sendChatMessage(conversationId, content) {
  if (!client || !client.connected) {
    throw new Error('WebSocket chưa kết nối, thử lại sau giây lát');
  }
  client.publish({
    destination: `/app/chat.sendMessage/${conversationId}`,
    body: JSON.stringify({ content }),
  });
}

export function disconnectChat() {
  unsubscribeConversation();
  if (errorSub) { errorSub.unsubscribe(); errorSub = null; }
  if (client) { client.deactivate(); client = null; }
  connectedPromise = null;
}

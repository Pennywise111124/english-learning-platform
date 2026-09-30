/**
 * api.js — LinguistAI API Layer (REAL BACKEND)
 * Contract: đối chiếu trực tiếp từ /v3/api-docs của Backend, không suy đoán.
 */

import { getToken, getRefreshToken, setToken, getUser, setUser, clearAuth } from './auth.js';
import { API_BASE_URL } from './config.js';

const BASE_URL = API_BASE_URL;

// ─── Core request engine (JSON + multipart), tự refresh khi 401 ───────────────

let refreshPromise = null;

async function doRefresh() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return false;

  if (!refreshPromise) {
    refreshPromise = fetch(`${BASE_URL}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'ngrok-skip-browser-warning': 'true' },
      body: JSON.stringify({ refreshToken })
    })
      .then(res => { if (!res.ok) throw new Error('refresh failed'); return res.json(); })
      .then(data => { setToken(data.accessToken); return true; })
      .catch(() => false)
      .finally(() => { refreshPromise = null; });
  }
  return refreshPromise;
}

function buildQuery(params) {
  if (!params) return '';
  const entries = Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '');
  if (entries.length === 0) return '';
  return `?${new URLSearchParams(entries).toString()}`;
}

async function request(path, { method = 'GET', body, isMultipart = false, params } = {}, _isRetry = false) {
  const token = getToken();
  const headers = { 'ngrok-skip-browser-warning': 'true' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  if (!isMultipart) headers['Content-Type'] = 'application/json';

  const url = `${BASE_URL}${path}${buildQuery(params)}`;

  const res = await fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : (isMultipart ? body : JSON.stringify(body))
  });

  // Không tự refresh cho chính các endpoint /api/auth/** (tránh vòng lặp) hoặc khi đã retry 1 lần rồi
  if (res.status === 401 && !_isRetry && !path.startsWith('/api/auth/')) {
    const refreshed = await doRefresh();
    if (refreshed) return request(path, { method, body, isMultipart, params }, true);
    clearAuth();
    window.location.href = 'index.html?auth=login';
    throw new Error('Unauthorized');
  }
  if (res.status === 401) {
    clearAuth();
    window.location.href = 'index.html?auth=login';
    throw new Error('Unauthorized');
  }

  if (!res.ok) {
    let msg = `HTTP ${res.status}`;
    try { const j = await res.json(); msg = j.message || msg; } catch { /* ignore */ }
    throw new Error(msg);
  }

  // DELETE và vài mutation admin trả 200 nhưng không có body
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

// ─── Auth ──────────────────────────────────────────────────────────────────

export async function login(username, password) {
  return request('/api/auth/login', { method: 'POST', body: { username, password } });
}

export async function register(email, username, password) {
  await request('/api/auth/register', { method: 'POST', body: { username, email, password } });
  // Backend trả 200 rỗng, không tự login — giữ nguyên hành vi cũ để register.html không cần sửa
  return { message: 'Registration successful! Please login.' };
}

// ─── Topics (User) ───────────────────────────────────────────────────────────

// GET /api/topics hỗ trợ keyword / level / sort (M10). Giá trị rỗng bị buildQuery() tự loại.
// sort: 'newest' (mặc định) | 'popular' | 'title'
export async function getTopics({ keyword, level, sort, page = 0, size = 20 } = {}) {
  return request('/api/topics', { params: { keyword, level, sort, page, size } });
}

export async function getTopicDetail(id) {
  return request(`/api/topics/${id}`);
}

export async function getFlashcards(topicId) {
  return request(`/api/topics/${topicId}/flashcards`);
}

export async function getQuizzesByTopic(topicId) {
  return request(`/api/topics/${topicId}/quizzes`);
}

// ─── Quizzes (User) ───────────────────────────────────────────────────────────

export async function getQuizDetail(quizId) {
  return request(`/api/quizzes/${quizId}`);
}

// answers PHẢI có dạng [{ questionId, answer }] — khớp SubmitAnswerItem của Backend
export async function submitQuiz(quizId, answers) {
  return request(`/api/quizzes/${quizId}/submit`, { method: 'POST', body: { answers } });
}

export async function getQuizAttempts(quizId, page = 0, size = 20) {
  return request(`/api/quizzes/${quizId}/attempts`, { params: { page, size } });
}

// ─── Progress ────────────────────────────────────────────────────────────────

export async function getProgress() {
  return request('/api/users/me/progress');
}

// ─── Vocabulary / SRS (M11) ──────────────────────────────────────────────────

export async function saveVocabulary(flashcardId) {
  return request(`/api/vocabulary/${flashcardId}/save`, { method: 'POST' });
}

export async function getAllVocabulary(page = 0, size = 20) {
  return request('/api/vocabulary', { params: { page, size } });
}

export async function reviewVocabulary(id, remembered) {
  return request(`/api/vocabulary/${id}/review`, { method: 'PATCH', body: { remembered } });
}

export async function getVocabularyToday(page = 0, size = 20) {
  return request('/api/vocabulary/today', { params: { page, size } });
}

export async function getSavedFlashcardIds(topicId) {
  return request(`/api/vocabulary/topics/${topicId}/saved-ids`);
}

export async function deleteVocabulary(id) {
  return request(`/api/vocabulary/${id}`, { method: 'DELETE' });
}

// ─── Chat / Conversations (REST — WebSocket streaming nằm ở chat.js riêng) ────

export async function getConversations(page = 0, size = 20) {
  return request('/api/conversations', { params: { page, size } });
}

export async function getConversation(id) {
  return request(`/api/conversations/${id}`);
}

export async function getMessages(conversationId) {
  return request(`/api/conversations/${conversationId}/messages`);
}

export async function createConversation() {
  return request('/api/conversations', { method: 'POST' });
}

// Lưu ý: đây chỉ lưu tin nhắn USER qua REST, KHÔNG trả về câu trả lời AI —
// câu trả lời AI đến qua kênh WebSocket (xem chat.js). Trang chat.html không nên
// coi response của hàm này là tin nhắn AI.
export async function sendMessage(conversationId, content) {
  return request(`/api/conversations/${conversationId}/messages`, { method: 'POST', body: { content } });
}

// ─── Profile ─────────────────────────────────────────────────────────────────

export async function getProfile() {
  return request('/api/users/me');
}

export async function uploadAvatar(file) {
  const formData = new FormData();
  formData.append('file', file);
  const updatedUser = await request('/api/users/me/avatar', { method: 'POST', body: formData, isMultipart: true });
  setUser(updatedUser); // cập nhật cache localStorage để navbar/profile hiện avatar mới ngay, không cần load lại
  return updatedUser;
}

export async function changePassword(currentPassword, newPassword) {
  return request('/api/users/me/password', { method: 'POST', body: { currentPassword, newPassword } });
}

export async function getMyRecentQuizAttempts(page = 0, size = 5) {
  return request('/api/users/me/quiz-attempts', { params: { page, size } });
}

// ─── Admin — Topics ───────────────────────────────────────────────────────────
// LƯU Ý: Backend KHÔNG có endpoint list riêng cho Admin — dùng chung GET /api/topics.

export async function adminGetTopics() {
  return request('/api/topics', { params: { page: 0, size: 100 } });
}

// LƯU Ý: TopicCreateRequest KHÔNG có field imageUrl — ảnh phải upload riêng
// qua adminUploadTopicImage() SAU KHI tạo topic thành công.
export async function adminCreateTopic({ title, description, level }) {
  return request('/api/admin/topics', { method: 'POST', body: { title, description, level } });
}

export async function adminUpdateTopic(id, { title, description, level }) {
  return request(`/api/admin/topics/${id}`, { method: 'PUT', body: { title, description, level } });
}

export async function adminDeleteTopic(id) {
  return request(`/api/admin/topics/${id}`, { method: 'DELETE' });
}

export async function adminUploadTopicImage(id, file) {
  const formData = new FormData();
  formData.append('file', file);
  return request(`/api/admin/topics/${id}/image`, { method: 'POST', body: formData, isMultipart: true });
}

// ─── Admin — Flashcards ───────────────────────────────────────────────────────
// LƯU Ý: FlashcardCreateRequest/UpdateRequest cũng KHÔNG có imageUrl — cùng cơ chế như Topic.

export async function adminCreateFlashcard(topicId, { word, meaning, example, audioUrl }) {
  return request(`/api/admin/topics/${topicId}/flashcards`, { method: 'POST', body: { word, meaning, example, audioUrl } });
}

export async function adminUpdateFlashcard(id, { word, meaning, example, audioUrl }) {
  return request(`/api/admin/flashcards/${id}`, { method: 'PUT', body: { word, meaning, example, audioUrl } });
}

export async function adminDeleteFlashcard(id) {
  return request(`/api/admin/flashcards/${id}`, { method: 'DELETE' });
}

export async function adminUploadFlashcardImage(id, file) {
  const formData = new FormData();
  formData.append('file', file);
  return request(`/api/admin/flashcards/${id}/image`, { method: 'POST', body: formData, isMultipart: true });
}

// ─── Admin — Quizzes & Questions ───────────────────────────────────────────────

export async function adminGetQuestionsForQuiz(quizId) {
  return request(`/api/admin/quizzes/${quizId}/questions`);
}

export async function adminCreateQuiz(topicId, { title }) {
  return request(`/api/admin/topics/${topicId}/quizzes`, { method: 'POST', body: { title } });
}

export async function adminUpdateQuiz(id, { title }) {
  return request(`/api/admin/quizzes/${id}`, { method: 'PUT', body: { title } });
}

export async function adminDeleteQuiz(id) {
  return request(`/api/admin/quizzes/${id}`, { method: 'DELETE' });
}

export async function adminCreateQuestion(quizId, { question, options, correctAnswer }) {
  return request(`/api/admin/quizzes/${quizId}/questions`, { method: 'POST', body: { question, options, correctAnswer } });
}

export async function adminUpdateQuestion(id, { question, options, correctAnswer }) {
  return request(`/api/admin/questions/${id}`, { method: 'PUT', body: { question, options, correctAnswer } });
}

export async function adminDeleteQuestion(id) {
  return request(`/api/admin/questions/${id}`, { method: 'DELETE' });
}


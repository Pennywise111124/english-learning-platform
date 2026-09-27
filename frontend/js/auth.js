/**
 * auth.js — LinguistAI JWT Auth Helpers
 * Handles token storage, user session, and route guards.
 */

const TOKEN_KEY         = 'linguistai_token';
const REFRESH_TOKEN_KEY = 'linguistai_refresh_token';
const USER_KEY          = 'linguistai_user';

export function saveAuth(loginResponse) {
  localStorage.setItem(TOKEN_KEY, loginResponse.accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, loginResponse.refreshToken);
  localStorage.setItem(USER_KEY, JSON.stringify(loginResponse.user));
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function setToken(newAccessToken) {
  localStorage.setItem(TOKEN_KEY, newAccessToken);
}

export function getUser() {
  const u = localStorage.getItem(USER_KEY);
  try { return u ? JSON.parse(u) : null; } catch { return null; }
}

// Gọi sau khi avatar (hoặc thông tin user) đổi, để navbar/profile hiển thị đúng
// mà không cần gọi lại GET /api/users/me.
export function setUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function isLoggedIn() {
  return !!getToken();
}

export function checkAuth() {
  if (!isLoggedIn()) {
    window.location.href = 'index.html?auth=login';
  }
}

export function checkAdmin() {
  if (!isLoggedIn()) {
    window.location.href = 'index.html?auth=login';
    return;
  }
  const user = getUser();
  if (!user || user.role !== 'ADMIN') {
    window.location.href = 'index.html';
  }
}

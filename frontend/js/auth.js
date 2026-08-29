/**
 * auth.js — LinguistAI JWT Auth Helpers
 * Handles token storage, user session, and route guards.
 */

const TOKEN_KEY = 'linguistai_token';
const USER_KEY  = 'linguistai_user';

export function saveAuth(loginResponse) {
  localStorage.setItem(TOKEN_KEY, loginResponse.accessToken);
  localStorage.setItem(USER_KEY, JSON.stringify(loginResponse.user));
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function getUser() {
  const u = localStorage.getItem(USER_KEY);
  try { return u ? JSON.parse(u) : null; } catch { return null; }
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function isLoggedIn() {
  return !!getToken();
}

export function checkAuth() {
  if (!isLoggedIn()) {
    window.location.href = 'login.html';
  }
}

export function checkAdmin() {
  if (!isLoggedIn()) {
    window.location.href = 'login.html';
    return;
  }
  const user = getUser();
  if (!user || user.role !== 'ADMIN') {
    window.location.href = 'index.html';
  }
}

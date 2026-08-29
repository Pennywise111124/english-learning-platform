/**
 * api.js — LinguistAI API Layer
 * Generic fetch wrapper with JWT injection and error handling.
 * Re-exports all mock functions — swap internals here when backend is ready.
 */

import { getToken, clearAuth } from './auth.js';

const BASE_URL = 'http://localhost:8080';

export async function apiFetch(path, options = {}) {
  const token   = getToken();
  const headers = { 'Content-Type': 'application/json', ...options.headers };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}${path}`, { ...options, headers });

  if (res.status === 401) {
    clearAuth();
    window.location.href = 'login.html';
    throw new Error('Unauthorized');
  }

  if (!res.ok) {
    let msg = `HTTP ${res.status}`;
    try { const j = await res.json(); msg = j.message || msg; } catch { /* ignore */ }
    throw new Error(msg);
  }

  return res.json();
}

// Re-export all mock API functions.
// When backend is ready: replace mockApi.js calls with apiFetch() calls here.
export * from './mockApi.js';

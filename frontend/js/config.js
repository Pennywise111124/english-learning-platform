/**
 * config.js — Mặc định '' = cùng origin (production qua nginx).
 * Dev qua ngrok: chạy trong Console của trình duyệt:
 *   localStorage.setItem('linguistai_api_base', 'https://xxx.ngrok-free.dev')
 * Quay lại cùng origin: localStorage.removeItem('linguistai_api_base')
 */
function readOverride() {
  try { return localStorage.getItem('linguistai_api_base'); } catch { return null; }
}

export const API_BASE_URL = (readOverride() ?? '').replace(/\/+$/, '');

export function imageSrc(path) {
  return path ? `${API_BASE_URL}${path}` : 'https://picsum.photos/seed/placeholder/300/200';
}

export function audioSrc(path) {
  return path ? `${API_BASE_URL}${path}` : '';
}

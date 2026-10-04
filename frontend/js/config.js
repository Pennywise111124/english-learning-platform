/**
 * config.js — Nguồn cấu hình chung, DUY NHẤT cần sửa mỗi khi đổi URL ngrok.
 */

export const API_BASE_URL = 'https://escapable-drank-skedaddle.ngrok-free.dev';

export function imageSrc(path) {
  return path ? `${API_BASE_URL}${path}` : 'https://picsum.photos/seed/placeholder/300/200';
}

export function audioSrc(path) {
  return path ? `${API_BASE_URL}${path}` : '';
}

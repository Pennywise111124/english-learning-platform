// achievements.js: thông báo "Achievement unlocked" ở app shell.
// Hiện mọi achievement đã mở khoá mà chưa đóng; chỉ biến mất khi người dùng bấm X.

import * as api from './api.js';
import { el } from './ui.js';

const shown = new Set();     // mã đang hiển thị, tránh tạo thẻ trùng
let container = null;
let busy = false;
let pending = false;         // có yêu cầu làm mới đến trong lúc đang tải
let started = false;

function ensureContainer() {
  if (container) return container;
  container = el('div', 'fixed bottom-4 right-4 z-[9998] flex flex-col gap-3 w-[min(22rem,calc(100vw-2rem))] pointer-events-none');
  container.setAttribute('role', 'status');
  container.setAttribute('aria-live', 'polite');
  document.body.append(container);
  return container;
}

function buildCard(a) {
  const card = el('div', 'pointer-events-auto flex items-start gap-3 bg-surface-container-lowest border-2 border-tertiary rounded-2xl p-4 shadow-lg');

  const iconBox = el('div', 'w-12 h-12 bg-tertiary-fixed rounded-2xl flex items-center justify-center flex-shrink-0');
  const icon = el('span', 'material-symbols-outlined text-tertiary', 'emoji_events');
  icon.style.fontVariationSettings = "'FILL' 1";
  icon.setAttribute('aria-hidden', 'true');
  iconBox.append(icon);

  const text = el('div', 'flex-1 min-w-0');
  text.append(
    el('p', 'font-label-sm text-label-sm text-tertiary uppercase font-bold', 'Achievement unlocked'),
    el('p', 'font-headline-md text-headline-md text-on-surface break-words', a.title),
    el('p', 'font-body-sm text-body-sm text-on-surface-variant', a.description)
  );

  const close = el('button', 'text-on-surface-variant hover:bg-surface-container-high rounded-full p-1 flex items-center justify-center flex-shrink-0');
  close.type = 'button';
  close.setAttribute('aria-label', `Dismiss achievement: ${a.title}`);
  const x = el('span', 'material-symbols-outlined text-[20px]', 'close');
  x.setAttribute('aria-hidden', 'true');
  close.append(x);

  close.addEventListener('click', async () => {
    close.disabled = true;
    try {
      await api.dismissAchievement(a.code);
      card.remove();
      shown.delete(a.code);
    } catch (error) {
      console.error('[dismissAchievement]', error);
      close.disabled = false; // giữ thẻ lại để người dùng thử lại
    }
  });

  card.append(iconBox, text, close);
  return card;
}

export async function refreshAchievements() {
  if (busy) { pending = true; return; }
  busy = true;
  try {
    const list = await api.getAchievements();
    const box = ensureContainer();
    for (const a of list) {
      if (a.unlocked && !a.dismissed && !shown.has(a.code)) {
        shown.add(a.code);
        box.append(buildCard(a));
      }
    }
  } catch (error) {
    console.error('[refreshAchievements]', error);
  } finally {
    busy = false;
    if (pending) { pending = false; refreshAchievements(); }
  }
}

export function initAchievementPopups() {
  if (started) return;
  started = true;
  ensureContainer();
  refreshAchievements();
  window.addEventListener('learning-activity', refreshAchievements);
}

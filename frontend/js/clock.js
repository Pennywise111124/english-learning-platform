import { getUser } from './auth.js';
import { el } from './ui.js';

// Múi giờ đã lưu của người dùng; chưa có (hoặc sai) thì undefined = dùng múi giờ trình duyệt
function userZone() {
    const tz = getUser()?.timezone;
    if (!tz) return undefined;
    try {
        new Intl.DateTimeFormat('en-GB', { timeZone: tz });
        return tz;
    } catch {
        return undefined;
    }
}

export function initClock(container) {
    if (!container) return () => {};

    const wrap = el('div', 'flex items-center gap-1.5 px-3 py-1.5 rounded-full border-2 border-outline-variant text-on-surface-variant font-label-md text-label-md');
    const icon = el('span', 'material-symbols-outlined', 'schedule');
    icon.style.fontSize = '18px';
    const time = el('span', 'tabular-nums');
    wrap.appendChild(icon);
    wrap.appendChild(time);
    container.appendChild(wrap);

    function tick() {
        const zone = userZone();
        const now = new Date();
        const parts = new Intl.DateTimeFormat('en-GB', {
            timeZone: zone, hour: '2-digit', minute: '2-digit', hourCycle: 'h23'
        }).formatToParts(now);
        const h = Number(parts.find(p => p.type === 'hour').value);
        const m = Number(parts.find(p => p.type === 'minute').value);

        time.textContent = String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0');

        const left = 24 * 60 - (h * 60 + m);
        const zoneName = zone || Intl.DateTimeFormat().resolvedOptions().timeZone;
        wrap.title = `Your day resets in ${Math.floor(left / 60)}h ${left % 60}m (${zoneName})`;
    }

    tick();
    const timer = setInterval(tick, 15000);
    return () => clearInterval(timer); // gọi để dừng nếu cần
}

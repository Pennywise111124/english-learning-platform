// navbar.js — App shell: sidebar (thu gọn được) + topbar.
// - Đã đăng nhập: sidebar đầy đủ, avatar, đăng xuất.
// - Khách: vẫn thấy sidebar; mục cần đăng nhập có ổ khoá, bấm vào sẽ mở hộp thoại đăng nhập.
// - Trang không có app shell (ví dụ landing page): thanh điều hướng đơn giản như cũ.
// Vẫn export initNavBar(activePage) để không phải đổi cách gọi ở các trang.

import { getUser, clearAuth } from './auth.js';
import { imageSrc } from './config.js';
import { getTheme, toggleTheme } from './theme.js';

const SIDEBAR_COLLAPSED_KEY = 'linguistai_sidebar_collapsed';
const SIDEBAR_WIDTH_EXPANDED = 256; // px
const SIDEBAR_WIDTH_COLLAPSED = 80; // px

// public: true = khách dùng được; false = cần đăng nhập
const NAV_ITEMS = [
  { id: 'chat', label: 'Chat', href: 'chat.html', icon: 'forum', public: false },
  { id: 'topics', label: 'Topics', href: 'topics.html', icon: 'style', public: true },
  { id: 'vocabulary', label: 'My Words', href: 'vocabulary.html', icon: 'bookmark', public: false },
  { id: 'dictation', label: 'Dictation', href: 'dictation.html', icon: 'headphones', public: false },
  { id: 'progress', label: 'Progress', href: 'progress.html', icon: 'monitoring', public: false }
];
const ADMIN_NAV_ITEM = { id: 'admin', label: 'Admin', href: 'admin.html', icon: 'admin_panel_settings', public: false };

function openLoginModal(redirectTo) {
  window.dispatchEvent(new CustomEvent('open-auth-modal', { detail: { mode: 'login', redirectTo } }));
}

export function initNavBar(activePage = '') {
  const user = getUser();

  if (!user) {
    const hasAppShell = document.getElementById('app-sidebar-container') && document.getElementById('app-topbar-container');
    if (hasAppShell) renderGuestShell(activePage);
    else renderGuestNavBar();
    return;
  }

  const navItems = [...NAV_ITEMS];
  if (user.role === 'ADMIN') navItems.push(ADMIN_NAV_ITEM);

  const avatarUrl = user.avatarUrl ? imageSrc(user.avatarUrl) : 'https://picsum.photos/seed/default/200/200';
  const isCollapsed = localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === '1';

  renderSidebar(navItems, activePage, isCollapsed, false);
  renderTopbar(avatarUrl);
  applySidebarWidth(isCollapsed);

  window.addEventListener('resize', () => applySidebarWidth(localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === '1'));
}

function renderGuestShell(activePage) {
  const isCollapsed = localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === '1';
  renderSidebar(NAV_ITEMS, activePage, isCollapsed, true);
  renderGuestTopbar();
  applySidebarWidth(isCollapsed);

  window.addEventListener('resize', () => applySidebarWidth(localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === '1'));
}

// ─── Sidebar ─────────────────────────────────────────────────────

function renderSidebar(navItems, activePage, isCollapsed, isGuest) {
  const container = document.getElementById('app-sidebar-container');
  if (!container) { console.error('[NavBar] Missing #app-sidebar-container'); return; }

  const navLinksHTML = navItems.map(item => {
    // Khách + mục cần đăng nhập: nút có ổ khoá, bấm vào mở hộp thoại đăng nhập
    if (isGuest && !item.public) {
      return `
        <button type="button" data-gated="${item.id}" data-href="${item.href}" title="Log in to use ${item.label}" aria-label="${item.label} (log in required)"
          class="text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface flex items-center gap-3 px-4 py-3 rounded-2xl transition-colors nav-label-wrap w-full text-left">
          <span class="material-symbols-outlined flex-shrink-0" aria-hidden="true">${item.icon}</span>
          <span class="nav-label font-label-md text-label-md whitespace-nowrap flex-1">${item.label}</span>
          <span class="nav-label material-symbols-outlined text-[18px] opacity-70" aria-hidden="true">lock</span>
        </button>
      `;
    }

    const isActive = item.id === activePage;
    const activeClass = isActive
      ? 'bg-primary-fixed text-on-primary-fixed font-bold'
      : 'text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface';
    return `
      <a href="${item.href}" class="${activeClass} flex items-center gap-3 px-4 py-3 rounded-2xl transition-colors nav-label-wrap" title="${item.label}">
        <span class="material-symbols-outlined flex-shrink-0" aria-hidden="true">${item.icon}</span>
        <span class="nav-label font-label-md text-label-md whitespace-nowrap">${item.label}</span>
      </a>
    `;
  }).join('');

  const guestCardHTML = isGuest ? `
      <div class="nav-label p-3 border-t-2 border-outline-variant flex-shrink-0">
        <div class="bg-primary-fixed rounded-2xl p-4 flex flex-col gap-2">
          <p class="font-label-md text-label-md font-bold text-on-primary-fixed">Unlock everything</p>
          <p class="font-body-sm text-body-sm text-on-primary-fixed">Log in to chat with AI, save words, practice dictation and track your progress.</p>
          <button id="sidebarLoginBtn" type="button" class="btn-3d mt-1 px-4 py-2 rounded-xl bg-primary border-primary-shadow text-on-primary font-label-md text-label-md hover:brightness-105 transition-all active:scale-95">
            Log in
          </button>
        </div>
      </div>
  ` : '';

  container.innerHTML = `
    <!-- Backdrop cho drawer trên mobile -->
    <div id="appSidebarBackdrop" class="hidden fixed inset-0 bg-on-surface/40 z-40 md:hidden"></div>

    <aside id="appSidebar" class="fixed left-0 top-0 h-screen z-50 bg-surface-container-lowest border-r-2 border-outline-variant flex flex-col -translate-x-full md:translate-x-0 transition-transform duration-200">
      <div class="sidebar-header h-16 flex items-center justify-between px-4 border-b-2 border-outline-variant flex-shrink-0">
        <a href="index.html" class="sidebar-logo font-headline-md text-headline-md font-extrabold text-primary whitespace-nowrap overflow-hidden flex items-center" title="LinguistAI">🦉<span class="sidebar-logo-text ml-1">LinguistAI</span></a>
        <button id="sidebarCollapseBtn" class="hidden md:flex text-on-surface-variant hover:bg-surface-container-high rounded-full p-1.5 items-center justify-center flex-shrink-0" title="Collapse sidebar">
          <span class="material-symbols-outlined text-[20px]">chevron_left</span>
        </button>
        <button id="appSidebarCloseBtn" class="md:hidden text-on-surface-variant hover:bg-surface-container-high rounded-full p-1.5 flex items-center justify-center">
          <span class="material-symbols-outlined text-[20px]">close</span>
        </button>
      </div>
      <nav class="flex-grow overflow-y-auto p-3 flex flex-col gap-1">
        ${navLinksHTML}
      </nav>
      ${guestCardHTML}
    </aside>
  `;

  const sidebarEl = document.getElementById('appSidebar');
  if (isCollapsed) sidebarEl.classList.add('sidebar-collapsed');

  // Mobile drawer open/close
  document.getElementById('appSidebarCloseBtn').addEventListener('click', closeMobileNavDrawer);
  document.getElementById('appSidebarBackdrop').addEventListener('click', closeMobileNavDrawer);

  // Collapse toggle (desktop only)
  document.getElementById('sidebarCollapseBtn').addEventListener('click', () => {
    const nowCollapsed = !sidebarEl.classList.contains('sidebar-collapsed');
    sidebarEl.classList.toggle('sidebar-collapsed', nowCollapsed);
    localStorage.setItem(SIDEBAR_COLLAPSED_KEY, nowCollapsed ? '1' : '0');
    applySidebarWidth(nowCollapsed);
  });

  // Khách: mục bị khoá và thẻ "Unlock everything" đều mở hộp thoại đăng nhập
  container.querySelectorAll('[data-gated]').forEach(btn => {
      btn.addEventListener('click', () => { closeMobileNavDrawer(); openLoginModal(btn.dataset.href); });
    });
  const sidebarLoginBtn = document.getElementById('sidebarLoginBtn');
  if (sidebarLoginBtn) {
    sidebarLoginBtn.addEventListener('click', () => { closeMobileNavDrawer(); openLoginModal(); });
  }
}

export function openMobileNavDrawer() {
  document.getElementById('appSidebar')?.classList.remove('-translate-x-full');
  document.getElementById('appSidebarBackdrop')?.classList.remove('hidden');
}

function closeMobileNavDrawer() {
  document.getElementById('appSidebar')?.classList.add('-translate-x-full');
  document.getElementById('appSidebarBackdrop')?.classList.add('hidden');
}

function applySidebarWidth(isCollapsed) {
  const isDesktop = window.innerWidth >= 768;
  const content = document.getElementById('app-content');
  if (content) {
    content.style.marginLeft = isDesktop ? `${isCollapsed ? SIDEBAR_WIDTH_COLLAPSED : SIDEBAR_WIDTH_EXPANDED}px` : '0px';
  }
  const sidebarEl = document.getElementById('appSidebar');
  if (sidebarEl && isDesktop) {
    sidebarEl.style.width = `${isCollapsed ? SIDEBAR_WIDTH_COLLAPSED : SIDEBAR_WIDTH_EXPANDED}px`;
  } else if (sidebarEl) {
    sidebarEl.style.width = `${SIDEBAR_WIDTH_EXPANDED}px`; // luôn full width khi mở dạng drawer mobile
  }
}

// ─── Topbar ──────────────────────────────────────────────────────

function bindThemeToggle() {
  document.getElementById('themeToggleBtn').addEventListener('click', () => {
    const newTheme = toggleTheme();
    document.getElementById('themeToggleBtn').querySelector('.material-symbols-outlined').textContent =
      newTheme === 'dark' ? 'light_mode' : 'dark_mode';
  });
}

function renderTopbar(avatarUrl) {
  const container = document.getElementById('app-topbar-container');
  if (!container) { console.error('[NavBar] Missing #app-topbar-container'); return; }

  const currentTheme = getTheme();

  container.innerHTML = `
    <header class="sticky top-0 z-30 h-16 flex items-center justify-between px-4 md:px-6 bg-surface/90 backdrop-blur-xl border-b-2 border-outline-variant">
      <button id="appSidebarMobileToggle" class="md:hidden text-on-surface-variant hover:bg-surface-container-high rounded-full p-2 flex items-center justify-center">
        <span class="material-symbols-outlined">menu</span>
      </button>
      <div class="flex-grow"></div>
      <div class="flex items-center gap-2">
        <button id="themeToggleBtn" class="text-on-surface-variant hover:bg-surface-container-high rounded-full p-2 flex items-center justify-center transition-colors" title="Toggle dark mode">
          <span class="material-symbols-outlined">${currentTheme === 'dark' ? 'light_mode' : 'dark_mode'}</span>
        </button>
        <button class="hidden md:flex text-on-surface-variant hover:bg-surface-container-high rounded-full p-2 items-center justify-center">
          <span class="material-symbols-outlined">notifications</span>
        </button>
        <div class="w-9 h-9 rounded-full border-2 border-outline-variant flex items-center justify-center ml-1 overflow-hidden cursor-pointer hover:border-primary transition-colors" onclick="window.location.href='profile.html'">
          <img alt="User profile" class="w-full h-full object-cover" src="${avatarUrl}" onerror="this.src='https://picsum.photos/seed/fallback/200/200'"/>
        </div>
        <button id="navbarLogoutBtn" class="text-error hover:bg-error-container rounded-full p-2 flex items-center justify-center ml-1" title="Logout">
          <span class="material-symbols-outlined">logout</span>
        </button>
      </div>
    </header>
  `;

  document.getElementById('appSidebarMobileToggle').addEventListener('click', openMobileNavDrawer);
  bindThemeToggle();

  document.getElementById('navbarLogoutBtn').addEventListener('click', () => {
    if (confirm('Are you sure you want to logout?')) {
      clearAuth();
      window.location.href = 'index.html';
    }
  });
}

// Topbar cho khách trên trang có sidebar: nút menu (mobile), đổi giao diện, đăng nhập
function renderGuestTopbar() {
  const container = document.getElementById('app-topbar-container');
  if (!container) { console.error('[NavBar] Missing #app-topbar-container'); return; }

  const currentTheme = getTheme();

  container.innerHTML = `
    <header class="sticky top-0 z-30 h-16 flex items-center justify-between px-4 md:px-6 bg-surface/90 backdrop-blur-xl border-b-2 border-outline-variant">
      <button id="appSidebarMobileToggle" class="md:hidden text-on-surface-variant hover:bg-surface-container-high rounded-full p-2 flex items-center justify-center" aria-label="Open menu">
        <span class="material-symbols-outlined">menu</span>
      </button>
      <div class="flex-grow"></div>
      <div class="flex items-center gap-2">
        <button id="themeToggleBtn" class="text-on-surface-variant hover:bg-surface-container-high rounded-full p-2 flex items-center justify-center transition-colors" title="Toggle dark mode">
          <span class="material-symbols-outlined">${currentTheme === 'dark' ? 'light_mode' : 'dark_mode'}</span>
        </button>
        <button id="guestLoginBtn" class="btn-3d px-5 py-2.5 rounded-2xl bg-primary border-primary-shadow text-on-primary font-label-md text-label-md hover:brightness-105 transition-all active:scale-95 duration-150">
          Log In
        </button>
      </div>
    </header>
  `;

  document.getElementById('appSidebarMobileToggle').addEventListener('click', openMobileNavDrawer);
  bindThemeToggle();
  document.getElementById('guestLoginBtn').addEventListener('click', () => openLoginModal());
}

// ─── Khách trên trang KHÔNG có app shell (ví dụ landing page): thanh điều hướng đơn giản ───

function renderGuestNavBar() {
  const container = document.getElementById('navbar-container') || document.getElementById('app-topbar-container');
  if (!container) { console.error('[NavBar] No guest navbar container found'); return; }

  container.innerHTML = `
    <nav class="bg-surface/90 backdrop-blur-xl sticky top-0 shadow-sm z-50 border-b border-outline-variant/50">
      <div class="flex justify-between items-center w-full px-margin-mobile md:px-margin-desktop max-w-container-max mx-auto h-16">
        <a href="index.html" class="font-headline-md text-headline-md font-extrabold text-primary focus-visible:outline-2 focus-visible:outline-primary focus-visible:outline-offset-2 rounded">
          LinguistAI 🦉
        </a>
        <button id="guestLoginBtn" class="btn-3d px-5 py-2.5 rounded-2xl bg-primary border-primary-shadow text-on-primary font-label-md text-label-md hover:brightness-105 transition-all active:scale-95 duration-150">
          Log In
        </button>
      </div>
    </nav>
  `;

  document.getElementById('guestLoginBtn').addEventListener('click', () => openLoginModal());
}

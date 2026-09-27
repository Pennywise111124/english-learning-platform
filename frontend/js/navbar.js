// navbar.js — App shell: sidebar (thu gọn được) + topbar, dùng chung cho trang đã đăng nhập.
// Vẫn export initNavBar(activePage) để không phải đổi cách gọi ở các trang.

import { getUser, clearAuth } from './auth.js';
import { imageSrc } from './config.js';
import { getTheme, toggleTheme } from './theme.js';

const SIDEBAR_COLLAPSED_KEY = 'linguistai_sidebar_collapsed';
const SIDEBAR_WIDTH_EXPANDED = 256; // px
const SIDEBAR_WIDTH_COLLAPSED = 80; // px

export function initNavBar(activePage = '') {
  const user = getUser();

  if (!user) {
    renderGuestNavBar();
    return;
  }

  const navItems = [
    { id: 'chat', label: 'Chat', href: 'chat.html', icon: 'forum' },
    { id: 'topics', label: 'Topics', href: 'topics.html', icon: 'style' },
    { id: 'progress', label: 'Progress', href: 'progress.html', icon: 'monitoring' }
  ];

  if (user.role === 'ADMIN') {
    navItems.push({ id: 'admin', label: 'Admin', href: 'admin.html', icon: 'admin_panel_settings' });
  }

  const avatarUrl = user.avatarUrl ? imageSrc(user.avatarUrl) : 'https://picsum.photos/seed/default/200/200';
  const isCollapsed = localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === '1';

  renderSidebar(navItems, activePage, isCollapsed);
  renderTopbar(avatarUrl);
  applySidebarWidth(isCollapsed);

  window.addEventListener('resize', () => applySidebarWidth(localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === '1'));
}

// ─── Sidebar ─────────────────────────────────────────────────────

function renderSidebar(navItems, activePage, isCollapsed) {
  const container = document.getElementById('app-sidebar-container');
  if (!container) { console.error('[NavBar] Missing #app-sidebar-container'); return; }

  const navLinksHTML = navItems.map(item => {
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

  document.getElementById('themeToggleBtn').addEventListener('click', () => {
    const newTheme = toggleTheme();
    document.getElementById('themeToggleBtn').querySelector('.material-symbols-outlined').textContent =
      newTheme === 'dark' ? 'light_mode' : 'dark_mode';
  });

  document.getElementById('navbarLogoutBtn').addEventListener('click', () => {
    if (confirm('Are you sure you want to logout?')) {
      clearAuth();
      window.location.href = 'index.html';
    }
  });
}

// ─── Guest (chưa đăng nhập) — giữ nguyên topbar đơn giản cũ, không có sidebar ───

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

  document.getElementById('guestLoginBtn').addEventListener('click', () => {
    window.dispatchEvent(new CustomEvent('open-auth-modal', { detail: 'login' }));
  });
}

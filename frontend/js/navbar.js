// NavBar Component - Reusable navigation bar for authenticated pages
// Usage: Call initNavBar(activePage) after DOM loads

import { getUser } from './auth.js';

/**
 * Initialize and render the navbar
 * @param {string} activePage - Current active page: 'chat' | 'topics' | 'progress' | 'admin'
 */
export function initNavBar(activePage = '') {
  const user = getUser();

  if (!user) {
    console.error('[NavBar] No user found in localStorage');
    return;
  }

  console.log('[NavBar] Initializing with user:', user);

  const navItems = [
    { id: 'chat', label: 'Chat', href: 'chat.html' },
    { id: 'topics', label: 'Topics', href: 'topics.html' },
    { id: 'progress', label: 'Progress', href: 'progress.html' }
  ];

  // Add Admin link if user is ADMIN
  if (user.role === 'ADMIN') {
    navItems.push({ id: 'admin', label: 'Admin', href: 'admin.html' });
    console.log('[NavBar] ✅ Admin link added');
  }

  const navLinksHTML = navItems.map(item => {
    const isActive = item.id === activePage;
    const activeClass = isActive
      ? 'text-primary font-bold border-b-2 border-primary pb-1'
      : 'text-on-surface-variant hover:text-primary transition-colors';

    return `<a class="${activeClass} font-label-md text-label-md" href="${item.href}">${item.label}</a>`;
  }).join('');

  // Use avatar from user object or fallback
  const avatarUrl = user.avatarUrl || 'https://picsum.photos/seed/default/200/200';
  console.log('[NavBar] Avatar URL:', avatarUrl);

  const navbarHTML = `
    <nav class="bg-surface/80 backdrop-blur-xl sticky top-0 shadow-sm z-50">
      <div class="flex justify-between items-center w-full px-margin-desktop max-w-container-max mx-auto h-16 border-b border-surface-container/80">
        <div class="font-display-lg text-[24px] font-bold text-primary cursor-pointer" onclick="window.location.href='index.html'">
          LinguistAI
        </div>
        <div class="hidden md:flex gap-gutter items-center">
          ${navLinksHTML}
        </div>
        <div class="flex gap-unit items-center">
          <button class="text-on-surface-variant hover:bg-surface-container-high transition-all duration-200 active:scale-95 rounded-full p-2 flex items-center justify-center">
            <span class="material-symbols-outlined">notifications</span>
          </button>
          <div class="w-8 h-8 rounded-full bg-primary-container flex items-center justify-center ml-2 overflow-hidden cursor-pointer" onclick="window.location.href='profile.html'">
            <img alt="User profile" class="w-full h-full object-cover" src="${avatarUrl}" onerror="this.src='https://picsum.photos/seed/fallback/200/200'"/>
          </div>
          <button id="navbarLogoutBtn" class="text-error hover:bg-error/10 transition-all duration-200 active:scale-95 rounded-full p-2 flex items-center justify-center ml-2" title="Logout">
            <span class="material-symbols-outlined">logout</span>
          </button>
        </div>
      </div>
    </nav>
  `;

  // Find the navbar placeholder or existing nav element
  const navbarContainer = document.getElementById('navbar-container');
  const existingNav = document.querySelector('nav');

  if (navbarContainer) {
    navbarContainer.innerHTML = navbarHTML;
    console.log('[NavBar] Rendered into #navbar-container');

    // Attach logout handler after rendering
    const logoutBtn = document.getElementById('navbarLogoutBtn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', () => {
        // Confirm before logout
        if (confirm('Are you sure you want to logout?')) {
          // Import clearAuth dynamically to avoid circular dependency
          import('./auth.js').then(({ clearAuth }) => {
            clearAuth();
            window.location.href = 'login.html';
          });
        }
      });
    }
  } else if (existingNav) {
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = navbarHTML;
    existingNav.replaceWith(tempDiv.firstElementChild);
    console.log('[NavBar] Replaced existing <nav>');

    // Attach logout handler after rendering
    const logoutBtn = document.getElementById('navbarLogoutBtn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', () => {
        if (confirm('Are you sure you want to logout?')) {
          import('./auth.js').then(({ clearAuth }) => {
            clearAuth();
            window.location.href = 'login.html';
          });
        }
      });
    }
  } else {
    console.error('[NavBar] No navbar container or <nav> element found');
  }
}

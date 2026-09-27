/**
 * authModal.js — Modal Login/Sign Up dùng chung cho các trang khách truy cập được
 * (index.html, topics.html, topic-detail.html). Gọi initAuthModal() một lần lúc trang
 * load để chèn modal vào DOM. Mở modal từ bất kỳ đâu bằng cách dispatch CustomEvent
 * 'open-auth-modal' (navbar.js dùng cách này cho nút Login của khách).
 */

import * as api from './api.js';
import { saveAuth } from './auth.js';
import { showToast } from './toast.js';

const MODAL_HTML = `
  <div id="authModal" class="hidden fixed inset-0 bg-on-surface/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
    <div class="bg-surface-container-lowest border-2 border-outline-variant rounded-xl shadow-2xl w-full max-w-md p-stack-md relative">
      <button id="closeAuthModal" class="absolute top-4 right-4 text-on-surface-variant hover:text-on-surface">
        <span class="material-symbols-outlined">close</span>
      </button>

      <div class="flex gap-6 border-b-2 border-outline-variant mb-stack-sm">
        <button id="authTabLogin" type="button" class="pb-3 font-label-md text-label-md border-b-4 border-primary text-primary font-bold transition-colors">Login</button>
        <button id="authTabRegister" type="button" class="pb-3 font-label-md text-label-md border-b-4 border-transparent text-on-surface-variant transition-colors">Sign Up</button>
      </div>

      <div id="authSuccessMessage" class="hidden mb-stack-sm p-3 bg-primary-fixed border-2 border-primary rounded-xl text-on-primary-fixed font-body-md font-semibold text-center">
        Registration successful! Please login.
      </div>

      <form id="loginForm" class="flex flex-col gap-stack-sm">
        <div>
          <label class="block font-label-md text-label-md text-on-surface mb-unit" for="loginUsername">Username or Email address</label>
          <input class="w-full bg-surface-container-low border-2 border-outline-variant rounded-xl px-4 py-3 font-body-md text-body-md text-on-background focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all" id="loginUsername" type="text" required/>
        </div>
        <div>
          <label class="block font-label-md text-label-md text-on-surface mb-unit" for="loginPassword">Password</label>
          <input class="w-full bg-surface-container-low border-2 border-outline-variant rounded-xl px-4 py-3 font-body-md text-body-md text-on-background focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all" id="loginPassword" type="password" required/>
        </div>
        <button class="btn-3d w-full bg-primary border-primary-shadow text-on-primary font-label-md text-label-md rounded-2xl py-3 mt-stack-sm hover:brightness-105 transition-all flex items-center justify-center gap-2" type="submit">
          Login
          <span class="material-symbols-outlined" style="font-size: 18px;">login</span>
        </button>
      </form>

      <form id="registerForm" class="hidden flex flex-col gap-stack-sm">
        <div class="flex flex-col gap-1">
          <label class="font-label-md text-label-md text-on-surface" for="registerUsername">Username</label>
          <input class="w-full bg-surface-container-low border-2 border-outline-variant rounded-xl px-4 py-3 font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary transition-all" id="registerUsername" required type="text"/>
        </div>
        <div class="flex flex-col gap-1">
          <label class="font-label-md text-label-md text-on-surface" for="registerEmail">Email address</label>
          <input class="w-full bg-surface-container-low border-2 border-outline-variant rounded-xl px-4 py-3 font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary transition-all" id="registerEmail" required type="email"/>
        </div>
        <div class="flex flex-col gap-1">
          <label class="font-label-md text-label-md text-on-surface" for="registerPassword">Password</label>
          <input class="w-full bg-surface-container-low border-2 border-outline-variant rounded-xl px-4 py-3 font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary transition-all" id="registerPassword" required type="password" minlength="6"/>
        </div>
        <div class="flex flex-col gap-1">
          <label class="font-label-md text-label-md text-on-surface" for="registerConfirmPassword">Confirm password</label>
          <input class="w-full bg-surface-container-low border-2 border-outline-variant rounded-xl px-4 py-3 font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary transition-all" id="registerConfirmPassword" required type="password" minlength="6"/>
          <p id="registerPasswordError" class="hidden text-error font-label-sm text-label-sm mt-1">Passwords do not match</p>
        </div>
        <button class="btn-3d mt-unit w-full bg-primary border-primary-shadow text-on-primary font-label-md text-label-md py-3 rounded-2xl transition-all" type="submit">
          Sign Up
        </button>
      </form>
    </div>
  </div>
`;

let modalEl = null;

export function initAuthModal() {
  if (document.getElementById('authModal')) return; // tránh chèn trùng nếu lỡ gọi 2 lần
  document.body.insertAdjacentHTML('beforeend', MODAL_HTML);
  modalEl = document.getElementById('authModal');

  const loginForm = document.getElementById('loginForm');
  const registerForm = document.getElementById('registerForm');
  const authTabLogin = document.getElementById('authTabLogin');
  const authTabRegister = document.getElementById('authTabRegister');
  const authSuccessMessage = document.getElementById('authSuccessMessage');

  document.getElementById('closeAuthModal').addEventListener('click', closeAuthModal);
  modalEl.addEventListener('click', (e) => { if (e.target === modalEl) closeAuthModal(); });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && !modalEl.classList.contains('hidden')) closeAuthModal(); });

  authTabLogin.addEventListener('click', () => switchAuthTab('login'));
  authTabRegister.addEventListener('click', () => switchAuthTab('register'));

  document.querySelectorAll('[data-open-login]').forEach(btn => btn.addEventListener('click', () => openAuthModal('login')));
  document.querySelectorAll('[data-open-register]').forEach(btn => btn.addEventListener('click', () => openAuthModal('register')));

  window.addEventListener('open-auth-modal', (e) => openAuthModal(e.detail));

  loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;
    try {
      const response = await api.login(username, password);
      saveAuth(response);
      window.location.href = 'chat.html';
    } catch (error) {
      showToast(error.message || 'Login failed. Please try again.', 'error');
    }
  });

  registerForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('registerUsername').value;
    const email = document.getElementById('registerEmail').value;
    const password = document.getElementById('registerPassword').value;
    const confirmPassword = document.getElementById('registerConfirmPassword').value;
    const errorEl = document.getElementById('registerPasswordError');

    if (password !== confirmPassword) {
      errorEl.classList.remove('hidden');
      return;
    }
    errorEl.classList.add('hidden');

    try {
      await api.register(email, username, password);
      registerForm.reset();
      switchAuthTab('login');
      authSuccessMessage.classList.remove('hidden');
    } catch (error) {
      showToast(error.message || 'Registration failed. Please try again.', 'error');
    }
  });

  function switchAuthTab(mode) {
    const isLogin = mode === 'login';
    loginForm.classList.toggle('hidden', !isLogin);
    registerForm.classList.toggle('hidden', isLogin);

    authTabLogin.classList.toggle('border-primary', isLogin);
    authTabLogin.classList.toggle('text-primary', isLogin);
    authTabLogin.classList.toggle('font-bold', isLogin);
    authTabLogin.classList.toggle('border-transparent', !isLogin);
    authTabLogin.classList.toggle('text-on-surface-variant', !isLogin);

    authTabRegister.classList.toggle('border-primary', !isLogin);
    authTabRegister.classList.toggle('text-primary', !isLogin);
    authTabRegister.classList.toggle('font-bold', !isLogin);
    authTabRegister.classList.toggle('border-transparent', isLogin);
    authTabRegister.classList.toggle('text-on-surface-variant', isLogin);

    authSuccessMessage.classList.add('hidden');
  }

  modalEl._switchAuthTab = switchAuthTab;

  const params = new URLSearchParams(window.location.search);
  if (params.get('auth') === 'login' || params.get('auth') === 'register') {
    openAuthModal(params.get('auth'));
  }
}

export function openAuthModal(mode = 'login') {
  if (!modalEl) return;
  modalEl.classList.remove('hidden');
  document.body.classList.add('overflow-hidden');
  modalEl._switchAuthTab(mode);
}

function closeAuthModal() {
  modalEl.classList.add('hidden');
  document.body.classList.remove('overflow-hidden');
}

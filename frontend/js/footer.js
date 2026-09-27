/**
 * footer.js — Footer dùng chung cho mọi trang.
 * Gọi initFooter() một lần sau khi trang có sẵn <div id="app-footer-container"></div>.
 */

export function initFooter() {
  const container = document.getElementById('app-footer-container');
  if (!container) { console.error('[Footer] Missing #app-footer-container'); return; }

  container.innerHTML = `
    <footer class="w-full border-t-2 border-outline-variant bg-surface-container-lowest mt-auto">
      <div class="flex flex-col md:flex-row justify-between items-center px-gutter md:px-margin-desktop py-stack-md max-w-container-max mx-auto gap-stack-sm">
        <div class="font-headline-md text-headline-md font-extrabold text-primary">
          LinguistAI 🦉
        </div>
        <nav class="flex flex-wrap justify-center gap-gutter">
          <span class="font-label-md text-label-md text-on-surface-variant/60 cursor-not-allowed" role="button" aria-disabled="true" title="Coming soon">Privacy Policy</span>
          <span class="font-label-md text-label-md text-on-surface-variant/60 cursor-not-allowed" role="button" aria-disabled="true" title="Coming soon">Terms of Service</span>
          <span class="font-label-md text-label-md text-on-surface-variant/60 cursor-not-allowed" role="button" aria-disabled="true" title="Coming soon">Help Center</span>
          <span class="font-label-md text-label-md text-on-surface-variant/60 cursor-not-allowed" role="button" aria-disabled="true" title="Coming soon">Contact Us</span>
        </nav>
        <div class="text-on-surface-variant font-label-md">
          © <span id="footerCopyrightYear"></span> LinguistAI. All rights reserved.
        </div>
      </div>
    </footer>
  `;
  document.getElementById('footerCopyrightYear').textContent = new Date().getFullYear();
}

// main.js — imports + wires all cards on DOMContentLoaded
import { session }  from './session.js';
import { enterApp, logout } from './router.js';
import { API }      from './api.js';
import { showAlert } from './util.js';

import { init as initGraphQL }             from './cards/graphql.js';
import { init as initConnectionTest }      from './cards/connectionTest.js';
import { init as initListFolders }         from './cards/listFolders.js';
import { init as initListDocumentClasses } from './cards/listDocumentClasses.js';
import { init as initUserGroups }          from './cards/userGroups.js';
import { init as initDocuments }           from './cards/documents.js';

document.addEventListener('DOMContentLoaded', () => {
  // ── Login ──────────────────────────────────────────────────────────
  document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const username  = document.getElementById('username').value.trim();
    const password  = document.getElementById('password').value;
    const alertEl   = document.getElementById('login-alert');
    alertEl.classList.add('hidden');

    if (!username || !password) {
      showAlert(alertEl, 'Username and password are required.');
      return;
    }

    const btn = document.getElementById('login-btn');
    btn.disabled    = true;
    btn.textContent = 'Signing in…';

    try {
      const res = await fetch(API.login, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.error || `HTTP ${res.status}`);
      }

      const data = await res.json();
      session.save({ ...data, username });
      enterApp();
    } catch (err) {
      showAlert(alertEl, err.message);
    } finally {
      btn.disabled    = false;
      btn.textContent = 'Sign in';
    }
  });

  // ── Logout ─────────────────────────────────────────────────────────
  document.getElementById('logout-btn').addEventListener('click', logout);

  // ── Card init ──────────────────────────────────────────────────────
  initGraphQL();
  initConnectionTest();
  initListFolders();
  initListDocumentClasses();
  initUserGroups();
  initDocuments();

  // ── Restore session on page load ───────────────────────────────────
  if (session.load()) {
    enterApp();
  }
});

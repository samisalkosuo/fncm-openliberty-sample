// main.js — bootstraps the app and mounts all registered cards
import './components/app-header.js';
import { session }   from './session.js';
import { enterApp, logout } from './router.js';
import { API }       from './api.js';
import { showAlert } from './util.js';
import { mountAllCards, runPostLoginCards } from './cards/registry.js';
import { layoutConfig } from './layout-config.js';

// ── Card imports (self-registering) ──
// NOTE: Import order no longer controls display order (that's now controlled by layout-config.js).
//       Cards are shown/hidden and positioned via layout-config.js entries.
//       To add a new card: 1) create js/cards/myFeature.js, 2) import it here, 3) add to layout-config.js.
import './cards/buildingInspectionDocs.js';
import './cards/fileBuildingInspectionDocs.js';
import './cards/connectionTest.js';
import './cards/graphql.js';
import './cards/documents.js';
import './cards/documentDetails.js';
import './cards/listFolders.js';
import './cards/folderTree.js';
import './cards/listDocumentClasses.js';
import './cards/userGroups.js';
import './cards/listDocumentsInFolder.js';
import './cards/folderDetails.js';
import './cards/createBuildingInspectionReportDocument.js';
import './cards/searchBuildingInspection.js';
// ── Add new card imports above this line ──

document.addEventListener('DOMContentLoaded', async () => {
  // ── Restore session first so cards can read state during init ───────
  const hasSession = session.load();

  // ── Mount cards into the grid ───────────────────────────────────────
  mountAllCards(document.querySelector('.card-grid'), layoutConfig);

  // ── Dev convenience: pre-fill login form from server env vars ───────
  try {
    const cfgRes = await fetch('/api/config');
    console.debug(cfgRes);
    if (cfgRes.ok) {
      const cfg = await cfgRes.json();
      console.debug(cfg);
      if (cfg.devUsername) document.getElementById('username').value = cfg.devUsername;
      if (cfg.devPassword) document.getElementById('password').value = cfg.devPassword;
    }
  } catch { /* dev-only; ignore failures */ }

  // ── Login ───────────────────────────────────────────────────────────
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
      runPostLoginCards();
    } catch (err) {
      showAlert(alertEl, err.message);
    } finally {
      btn.disabled    = false;
      btn.textContent = 'Sign in';
    }
  });

  // ── Logout ──────────────────────────────────────────────────────────
  document.getElementById('logout-btn').addEventListener('click', logout);

  // ── Restore session on page load ────────────────────────────────────
  if (hasSession) {
    enterApp();
    runPostLoginCards();
  }
});

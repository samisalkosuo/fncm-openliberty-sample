// cards/connectionTest.js — Connection Test card
import { apiFetch, API } from '../api.js';
import { esc, runCardAction } from '../util.js';
import { registerCard } from './registry.js';

registerCard({
  id: 'connection-test',
  size: 'normal',
  html: () => `
    <div class="card" id="card-connection-test">
      <h2>Connection Test</h2>
      <button id="connection-test-btn">Test connection</button>
      <div id="connection-test-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Loading…
      </div>
      <div id="connection-test-result" class="card-result"></div>
    </div>`,
  runAfterLogin: true,
  init() {
    document.getElementById('connection-test-btn').addEventListener('click', () => {
      this.run();
    });
  },
  async run() {
    await runCardAction('connection-test-spinner', 'connection-test-result', async container => {
      const res = await apiFetch(API.connectionTest);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      container.innerHTML = `
      <p>
      Connection status: <b>${esc(data.status)}</b><br/>
      Domain: <b>${esc(data.domain)}</b><br/>
      Object store: <b>${esc(data.objectStore)}</b><br/>
      </p>
      `;
    });
  },
});

// cards/connectionTest.js — Connection Test card
import { apiFetch, API } from '../api.js';
import { esc } from '../util.js';
import { registerCard } from './registry.js';

registerCard({
  id: 'connection-test',
  size: 'normal',
  html: () => `
    <div class="card" id="card-connection-test">
      <h2>Connection Test</h2>
      <button id="connection-test-btn">Load</button>
      <div id="connection-test-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Loading…
      </div>
      <div id="connection-test-result" class="card-result"></div>
    </div>`,
  init() {
    document.getElementById('connection-test-btn').addEventListener('click', async () => {
      const spinner   = document.getElementById('connection-test-spinner');
      const container = document.getElementById('connection-test-result');
      spinner.classList.remove('hidden');
      container.innerHTML = '';

      try {
        const res = await apiFetch(API.connectionTest);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.text();
        container.innerHTML = `<pre>${data}</pre>`;
      } catch (err) {
        container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
      } finally {
        spinner.classList.add('hidden');
      }
    });
  },
});

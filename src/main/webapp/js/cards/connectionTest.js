// cards/connectionTest.js — Connection Test card
import { apiFetch, API } from '../api.js';
import { esc } from '../util.js';

export function init() {
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
}

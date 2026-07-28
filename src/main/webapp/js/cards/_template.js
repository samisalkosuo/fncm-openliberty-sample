// cards/_template.js — copy and rename for each new card
// 1. Add a <div class="card"> block in index.html
// 2. Copy this file → cards/myFeature.js, implement the click handler
// 3. Import and call init() from main.js
import { apiFetch, API } from '../api.js';
import { esc } from '../util.js';

export function init() {
  document.getElementById('my-btn').addEventListener('click', async () => {
    const spinner   = document.getElementById('my-spinner');
    const container = document.getElementById('my-container');
    spinner.classList.remove('hidden');
    container.innerHTML = '';

    try {
      // 1. call endpoint
      const res = await apiFetch(API.myEndpoint);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      // 2. render result
      const data = await res.text();
      container.innerHTML = `<pre>${esc(data)}</pre>`;
    } catch (err) {
      container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
    } finally {
      // 3. hide spinner
      spinner.classList.add('hidden');
    }
  });
}

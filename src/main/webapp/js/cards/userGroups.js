// cards/userGroups.js — User Groups card
import { apiFetch, API } from '../api.js';
import { esc } from '../util.js';

export function init() {
  document.getElementById('user-groups-btn').addEventListener('click', async () => {
    const spinner   = document.getElementById('user-groups-spinner');
    const container = document.getElementById('user-groups-result');
    spinner.classList.remove('hidden');
    container.innerHTML = '';

    try {
      const res = await apiFetch(API.userGroups);
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

// cards/documents.js — Documents (REST) card
import { apiFetch, API } from '../api.js';
import { esc } from '../util.js';

export function init() {
  document.getElementById('load-docs-btn').addEventListener('click', async () => {
    const spinner   = document.getElementById('docs-spinner');
    const container = document.getElementById('docs-container');
    spinner.classList.remove('hidden');
    container.innerHTML = '';

    try {
      const res = await apiFetch(API.documents);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      const docs  = data.documents ?? [];

      if (docs.length === 0) {
        container.innerHTML = '<p style="color:#57606a;font-size:0.9rem;">No documents found.</p>';
      } else {
        const rows = docs.map(d => `<tr><td>${esc(d.id)}</td><td>${esc(d.name)}</td></tr>`).join('');
        container.innerHTML =
          `<table><thead><tr><th>ID</th><th>Name</th></tr></thead><tbody>${rows}</tbody></table>`;
      }
    } catch (err) {
      container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
    } finally {
      spinner.classList.add('hidden');
    }
  });
}

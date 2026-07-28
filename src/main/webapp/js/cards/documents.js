// cards/documents.js — Documents (REST) card
import { apiFetch, API } from '../api.js';
import { esc } from '../util.js';
import { registerCard } from './registry.js';

registerCard({
  id: 'documents',
  size: 'normal',
  html: () => `
    <div class="card" id="card-documents">
      <h2>Documents <small class="card-subtitle">(via REST)</small></h2>
      <button id="documents-btn">Load</button>
      <div id="documents-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Loading…
      </div>
      <div id="documents-result" class="card-result"></div>
    </div>`,
  init() {
    document.getElementById('documents-btn').addEventListener('click', async () => {
      const spinner   = document.getElementById('documents-spinner');
      const container = document.getElementById('documents-result');
      spinner.classList.remove('hidden');
      container.innerHTML = '';

      try {
        const res = await apiFetch(API.documents);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        const docs  = data.documents ?? [];

        if (docs.length === 0) {
          container.innerHTML = '<p class="text-muted">No documents found.</p>';
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
  },
});

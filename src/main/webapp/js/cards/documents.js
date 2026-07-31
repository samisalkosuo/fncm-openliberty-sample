// cards/documents.js — Documents (REST) card
import { apiFetch, API } from '../api.js';
import { esc, renderWithToggle } from '../util.js';
import { registerCard } from './registry.js';
import { session } from '../session.js';


registerCard({
  id: 'documents',
  size: 'normal',
  html: () => `
    <div class="card" id="card-documents">
      <h2>Documents</h2>
      <button id="documents-btn">Get documents</button>
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
      const REPOSITORY_IDENTIFIER = session.config.repositoryIdentifier;      
      console.log(REPOSITORY_IDENTIFIER);
      try {
        const res = await apiFetch(API.documents);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();

        renderWithToggle(container, data, (el, d) => {
          const docs = d.documents ?? [];
          if (docs.length === 0) {
            el.innerHTML = '<p class="text-muted">No documents found.</p>';
          } else {
            const rows = docs.map(doc =>
              `<tr><td>${esc(doc.id)}</td><td>${esc(doc.name)}</td></tr>`
            ).join('');
            el.innerHTML =
              `<table><thead><tr><th>ID</th><th>Name</th></tr></thead><tbody>${rows}</tbody></table>`;
          }
        });
      } catch (err) {
        container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
      } finally {
        spinner.classList.add('hidden');
      }
    });
  },
});

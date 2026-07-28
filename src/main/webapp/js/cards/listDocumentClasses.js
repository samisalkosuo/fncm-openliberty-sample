// cards/listDocumentClasses.js — List Document Classes card
import { apiFetch, API } from '../api.js';
import { esc, renderJson } from '../util.js';
import { registerCard } from './registry.js';

registerCard({
  id: 'list-document-classes',
  size: 'normal',
  html: () => `
    <div class="card" id="card-list-document-classes">
      <h2>List Document Classes</h2>
      <button id="list-document-classes-btn">Load</button>
      <div id="list-document-classes-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Loading…
      </div>
      <div id="list-document-classes-result" class="card-result"></div>
    </div>`,
  init() {
    document.getElementById('list-document-classes-btn').addEventListener('click', async () => {
      const spinner   = document.getElementById('list-document-classes-spinner');
      const container = document.getElementById('list-document-classes-result');
      spinner.classList.remove('hidden');
      container.innerHTML = '';

      try {
        const res = await apiFetch(API.listDocumentClasses);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        renderJson(container, data);
      } catch (err) {
        container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
      } finally {
        spinner.classList.add('hidden');
      }
    });
  },
});

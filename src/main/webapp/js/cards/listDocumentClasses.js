// cards/listDocumentClasses.js — List Document Classes card
import { apiFetch, API } from '../api.js';
import { renderJson, runCardAction } from '../util.js';
import { registerCard } from './registry.js';

registerCard({
  id: 'list-document-classes',
  size: 'normal',
  html: () => `
    <div class="card" id="card-list-document-classes">
      <h2>List Document Classes</h2>
      <label style="display:inline-flex;align-items:center;gap:0.4em;margin-bottom:0.5em;">
        <input type="checkbox" id="list-document-classes-hidden">
        Include hidden classes
      </label>
      <br>
      <button id="list-document-classes-btn">Load</button>
      <div id="list-document-classes-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Loading…
      </div>
      <div id="list-document-classes-result" class="card-result"></div>
    </div>`,
  init() {
    document.getElementById('list-document-classes-btn').addEventListener('click', () => {
      const includeHidden = document.getElementById('list-document-classes-hidden').checked;
      runCardAction('list-document-classes-spinner', 'list-document-classes-result', async container => {
        const url = API.listDocumentClasses + (includeHidden ? '?includeHidden=true' : '');
        const res = await apiFetch(url);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        renderJson(container, await res.json());
      });
    });
  },
});

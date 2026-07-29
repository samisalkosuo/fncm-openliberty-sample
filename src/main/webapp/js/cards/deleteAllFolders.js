import { apiFetch, API } from '../api.js';
import { esc, renderJson, renderWithToggle } from '../util.js';
import { registerCard } from './registry.js';

registerCard({
  id: 'delete-all-folders',
  size: 'normal',   // 'normal' | 'wide' | 'tall' | 'large' | 'full'
  html: () => `
    <div class="card" id="card-delete-all-folders">
      <h2>Delete All Folders</h2>
      <button id="delete-all-folders-btn" class="button-delete">Delete all folders</button>
      <div id="delete-all-folders-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Loading…
      </div>
      <div id="delete-all-folders-result" class="card-result"></div>
    </div>`,
  init() {
    // To receive events from other cards, subscribe here:
    // subscribe(TOPICS.DOCUMENT_SELECTED, (payload) => { ... });

    document.getElementById('delete-all-folders-btn').addEventListener('click', async () => {
      const spinner   = document.getElementById('delete-all-folders-spinner');
      const container = document.getElementById('delete-all-folders-result');
      spinner.classList.remove('hidden');
      container.innerHTML = '';

      try {
        // 1. call endpoint
        const res = await apiFetch(API.deleteAllFolders,{method:"DELETE"});
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        // 2. render result as expandable JSON tree
        const data = await res.json();
        renderJson(container, data);
        // — or, if you have a custom table/visualization —
        // renderWithToggle(container, data, (el, d) => {
        //   el.innerHTML = `<p>${esc(d.someField)}</p>`;
        // });
      } catch (err) {
        container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
      } finally {
        // 3. hide spinner
        spinner.classList.add('hidden');
      }
    });
  },
});

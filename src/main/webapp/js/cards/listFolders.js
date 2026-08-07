// cards/listFolders.js — List Folders card
import { apiFetch, API } from '../api.js';
import { esc, renderJson } from '../util.js';
import { registerCard } from './registry.js';
import { publish,TOPICS } from '../eventBus.js';

registerCard({
  id: 'list-folders',
  size: 'normal',
  html: () => `
    <div class="card" id="card-list-folders">
      <h2>List Folders</h2>
      <button id="list-folders-btn">Load</button>
      <div id="list-folders-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Loading…
      </div>
      <div id="list-folders-result" class="card-result"></div>
    </div>`,
  init() {
    document.getElementById('list-folders-btn').addEventListener('click', async () => {
      const spinner   = document.getElementById('list-folders-spinner');
      const container = document.getElementById('list-folders-result');
      spinner.classList.remove('hidden');
      container.innerHTML = '';

      try {
        const res = await apiFetch(API.listFolders);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        const folders = data.folders;

        folders.forEach(folder => {
          const a = document.createElement('a');
          a.href = '#';
          a.textContent = folder.path;
          a.addEventListener('click', e => {
            e.preventDefault();
            publish(TOPICS.FOLDER_SELECTED, folder);
          });
          container.appendChild(a);
          container.appendChild(document.createElement('br'));
        });

      } catch (err) {
        container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
      } finally {
        spinner.classList.add('hidden');
      }
    });
  },
});

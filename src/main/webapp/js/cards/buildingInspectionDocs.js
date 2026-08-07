import { apiFetch, API } from '../api.js';
import { renderJson, runCardAction } from '../util.js';
import { registerCard } from './registry.js';

registerCard({
  id: 'addbuildinginspectiondocs',
  size: 'normal',   // 'normal' | 'wide' | 'tall' | 'large' | 'full'
  html: () => `
    <div class="card" id="card-addbuildinginspectiondocs">
      <h2>Building Inspection Documents Setup</h2>
      <p>Create or delete Building Inspection Report document classes and documents.</p>
      <div style="display:flex; flex-direction:column; gap:.5rem; margin-top:.5rem;">
        <button id="addbuildinginspectiondocs-add-btn">Create classes and upload docs</button>
        <button id="addbuildinginspectiondocs-delete-btn" class="button-delete">Delete docs and classes</button>
        <button id="addbuildinginspectiondocs-delete-folders-btn" class="button-delete">Delete all folders</button>
      </div>
      <div id="addbuildinginspectiondocs-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Loading…
      </div>
      <div id="addbuildinginspectiondocs-result" class="card-result"></div>
    </div>`,
  init() {
    // To receive events from other cards, subscribe here:
    // subscribe(TOPICS.DOCUMENT_SELECTED, (payload) => { ... });

    //add button
    document.getElementById('addbuildinginspectiondocs-add-btn').addEventListener('click', () =>
      runCardAction('addbuildinginspectiondocs-spinner', 'addbuildinginspectiondocs-result', async container => {
        const res = await apiFetch(API.addBuildingInspectionDocs, { method: 'POST' });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        renderJson(container, await res.json());
      })
    );

    //delete button
    document.getElementById('addbuildinginspectiondocs-delete-btn').addEventListener('click', () =>
      runCardAction('addbuildinginspectiondocs-spinner', 'addbuildinginspectiondocs-result', async container => {
        const res = await apiFetch(API.addBuildingInspectionDocs, { method: 'DELETE' });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        renderJson(container, await res.json());
      })
    );

    //delete all folders button
    document.getElementById('addbuildinginspectiondocs-delete-folders-btn').addEventListener('click', () =>
      runCardAction('addbuildinginspectiondocs-spinner', 'addbuildinginspectiondocs-result', async container => {
        const res = await apiFetch(API.deleteAllFolders, { method: 'DELETE' });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        renderJson(container, await res.json());
      })
    );
  },
});

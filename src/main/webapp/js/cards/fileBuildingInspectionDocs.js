import { apiFetch, API } from '../api.js';
import { renderJson, runCardAction } from '../util.js';
import { registerCard } from './registry.js';

registerCard({
  id: 'filebuildinginspectiondocs',
  size: 'normal',   // 'normal' | 'wide' | 'tall' | 'large' | 'full'
  html: () => `
    <div class="card" id="card-filebuildinginspectiondocs">
      <h2>File Building Inspection Documents</h2>
      <p>File building inspection reports to folders.</p>
      <button id="filebuildinginspectiondocs-btn">Create folders & file docs</button>      
      <div id="filebuildinginspectiondocs-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Loading…
      </div>
      <div id="filebuildinginspectiondocs-result" class="card-result"></div>
    </div>`,
  init() {
    // To receive events from other cards, subscribe here:
    // subscribe(TOPICS.DOCUMENT_SELECTED, (payload) => { ... });

    //file button
    document.getElementById('filebuildinginspectiondocs-btn').addEventListener('click', () =>
      runCardAction('filebuildinginspectiondocs-spinner', 'filebuildinginspectiondocs-result', async container => {
        const res = await apiFetch(API.fileBuildingInspectionDocs, { method: 'POST' });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        renderJson(container, await res.json());
      })
    );

  },
});

import { apiFetch, API } from '../api.js';
import { esc, renderJson, renderWithToggle } from '../util.js';
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
    document.getElementById('filebuildinginspectiondocs-btn').addEventListener('click', async () => {
      const spinner   = document.getElementById('filebuildinginspectiondocs-spinner');
      const container = document.getElementById('filebuildinginspectiondocs-result');
      spinner.classList.remove('hidden');
      container.innerHTML = '';

      try {
        // 1. call endpoint
        const res = await apiFetch(API.fileBuildingInspectionDocs,{method:"POST"});
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

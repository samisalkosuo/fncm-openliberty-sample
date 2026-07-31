import { apiFetch, API } from '../api.js';
import { esc, renderJson, renderWithToggle } from '../util.js';
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
    document.getElementById('addbuildinginspectiondocs-add-btn').addEventListener('click', async () => {
      const spinner   = document.getElementById('addbuildinginspectiondocs-spinner');
      const container = document.getElementById('addbuildinginspectiondocs-result');
      spinner.classList.remove('hidden');
      container.innerHTML = '';

      try {
        // 1. call endpoint
        const res = await apiFetch(API.addBuildingInspectionDocs,{method:"POST"});
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

    //delete button
    document.getElementById('addbuildinginspectiondocs-delete-btn').addEventListener('click', async () => {
      const spinner   = document.getElementById('addbuildinginspectiondocs-spinner');
      const container = document.getElementById('addbuildinginspectiondocs-result');
      spinner.classList.remove('hidden');
      container.innerHTML = '';

      try {
        // 1. call endpoint
        const res = await apiFetch(API.addBuildingInspectionDocs,{method: "DELETE"});
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

    //delete all folders button
    document.getElementById('addbuildinginspectiondocs-delete-folders-btn').addEventListener('click', async () => {
      const spinner   = document.getElementById('addbuildinginspectiondocs-spinner');
      const container = document.getElementById('addbuildinginspectiondocs-result');
      spinner.classList.remove('hidden');
      container.innerHTML = '';

      try {
        const res = await apiFetch(API.deleteAllFolders,{method:"DELETE"});
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

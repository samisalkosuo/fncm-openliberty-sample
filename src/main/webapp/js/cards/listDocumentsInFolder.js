// cards/listDocumentsInFolder.js — List Documents In Folder card
import { apiFetch, API } from '../api.js';
import { esc, renderWithToggle } from '../util.js';
import { registerCard } from './registry.js';
import { publish, subscribe, TOPICS } from '../eventBus.js';

function buildSubFoldersHtml(subFolders) {
  if (subFolders.length === 0) return '';
  return `<h4>Sub-folders (${subFolders.length})</h4>`
       + `<ul>${subFolders.map(f => `<li>${esc(f.name)}</li>`).join('')}</ul>`;
}

function buildDocumentsTableHtml(docs) {
  if (docs.length === 0) return '<p class="text-muted">No documents found.</p>';
  const rows = docs.map(doc =>
    `<tr data-doc-id="${esc(doc.id)}" data-doc-name="${esc(doc.name)}" data-doc-class="${esc(doc.className)}" data-date-created="${esc(doc.dateCreated)}">
      <td><button class="link-btn" type="button">${esc(doc.name)}</button></td>
      <!--
      <td>${esc(doc.id)}</td>
      <td>${esc(doc.className)}</td>
      <td>${esc(doc.dateCreated ?? '')}</td>
      -->
    </tr>`
  ).join('');
  return `<table>
    <thead><tr><th>ID</th><th>Name</th><th>Class</th><th>Created</th></tr></thead>
    <tbody>${rows}</tbody>
  </table>`;
}

function handleDocumentClick(e) {
  const btn = e.target.closest('.link-btn');
  if (!btn) return;
  const row = btn.closest('tr');
  publish(TOPICS.DOCUMENT_ID, row.dataset.docId);
  publish(TOPICS.DOCUMENT_SELECTED, {
    id:        row.dataset.docId,
    name:      row.dataset.docName,
    className: row.dataset.docClass,
    dateCreated: row.dataset.dateCreated,
  });
}

registerCard({
  id: 'list-documents-in-folder',
  size: 'full',
  html: () => `
    <div class="card" id="card-list-documents-in-folder">
      <h2>List Documents in Folder</h2>
      <div class="form-group">
        <label for="list-documents-in-folder-path">Folder path</label>
        <input id="list-documents-in-folder-path" type="text"
               placeholder="/" />
      </div>
      <button id="list-documents-in-folder-btn">Load</button>
      <div id="list-documents-in-folder-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Loading…
      </div>
      <div id="list-documents-in-folder-result" class="card-result"></div>
    </div>`,
    init() {
      subscribe(TOPICS.FOLDER_SELECTED, (folderData) => {
        document.getElementById('list-documents-in-folder-path').value = folderData.path;
        //this.listDocumentInGivenFolder();
      });

      document.getElementById('list-documents-in-folder-btn').addEventListener('click', async () => {
        this.listDocumentInGivenFolder();
    });
  },
  async listDocumentInGivenFolder() {
      const folder    = document.getElementById('list-documents-in-folder-path').value.trim();
      const spinner   = document.getElementById('list-documents-in-folder-spinner');
      const container = document.getElementById('list-documents-in-folder-result');


      if (!folder) {
        container.innerHTML = `<div class="alert alert-error">Folder path is required.</div>`;
        return;
      }

      spinner.classList.remove('hidden');
      container.innerHTML = '';

      try {
        const res = await apiFetch(
          `${API.listDocumentsInFolder}?folder=${encodeURIComponent(folder)}`
        );
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();

        renderWithToggle(container, data, (el, d) => {
          const folderData = d?.data?.folder;
          if (!folderData) {
            el.innerHTML = '<p class="text-muted">No folder data returned.</p>';
            return;
          }

          const docs       = folderData.containedDocuments?.documents ?? [];
          const subFolders = folderData.subFolders?.folders ?? [];

          el.innerHTML =
            `<p><strong>Path:</strong> ${esc(folderData.pathName ?? folderData.name)}</p>`
            + buildSubFoldersHtml(subFolders)
            + buildDocumentsTableHtml(docs);

          // Delegated click listener — one handler for all document rows
          el.querySelector('tbody')?.addEventListener('click', handleDocumentClick);
        });
      } catch (err) {
        container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
      } finally {
        spinner.classList.add('hidden');
      }

  }
});

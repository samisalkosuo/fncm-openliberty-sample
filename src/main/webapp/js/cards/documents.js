// cards/documents.js — Documents (REST) card
import { GraphQL,apiFetch, API } from '../api.js';
import { esc, renderWithToggle, renderJson } from '../util.js';
import { registerCard } from './registry.js';
import { session } from '../session.js';
import { publish, subscribe, TOPICS } from '../eventBus.js';
import { logout } from '../router.js';


registerCard({
  id: 'documents',
  size: 'normal',
  html: () => `
    <div class="card" id="card-documents">
      <h2>Documents</h2>
      <div style="display:flex;gap:0.5rem;flex-wrap:wrap;margin-bottom:0.5rem">
        <button id="documents-btn">Get documents</button>
        <button id="documents-clear-btn" class="hidden">Clear selection</button>
      </div>
      <div id="documents-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Loading…
      </div>
      <div id="documents-result" class="card-result"></div>
    </div>`,
  init() {
    document.getElementById('documents-btn').addEventListener('click', () => this.loadDocuments());
    document.getElementById('documents-clear-btn').addEventListener('click', () => this.clearSelection());

    // Restore previously selected document after a page refresh
    const savedId = session.getState('selectedDocumentId');
    if (savedId) {
      this.loadDocuments(savedId);
    }
  },

  async loadDocuments(preselectId = null) {
    const spinner   = document.getElementById('documents-spinner');
    const container = document.getElementById('documents-result');
    spinner.classList.remove('hidden');
    container.innerHTML = '';
    try {
      const documentResult = await this.getDocuments();
      const docs = documentResult?.data.documents?.documents ?? [];
      if (docs.length === 0) {
        container.innerHTML = '<p class="text-muted">No documents found.</p>';
        return;
      }

      const sorted = [...docs].sort((a, b) => a.name.localeCompare(b.name));

      const select = document.createElement('select');
      select.id = 'documents-dropdown';

      const placeholder = document.createElement('option');
      placeholder.value = '';
      placeholder.textContent = '— Select a document —';
      placeholder.disabled = true;
      placeholder.selected = !preselectId;
      select.appendChild(placeholder);

      sorted.forEach(doc => {
        const option = document.createElement('option');
        option.value = doc.id;
        option.textContent = esc(doc.name);
        if (preselectId && doc.id === preselectId) option.selected = true;
        select.appendChild(option);
      });

      select.addEventListener('change', () => {
        const selectedId = select.value;
        if (selectedId) this.onDocumentSelected(selectedId);
      });

      const idLabel = document.createElement('p');
      idLabel.id = 'documents-selected-id';
      idLabel.className = 'text-muted';
      container.appendChild(select);
      container.appendChild(idLabel);

      // If restoring a saved selection, update the label and publish the event
      if (preselectId && sorted.some(d => d.id === preselectId)) {
        this.onDocumentSelected(preselectId);
      }
    } catch (err) {
      if (err.status === 401) {
        container.innerHTML = `
          <div class="alert alert-error">
            <strong>401 Unauthorized</strong> — ${esc(err.message)}
            <br><br>
            <button id="documents-relogin-btn">Sign in again</button>
          </div>`;
        document.getElementById('documents-relogin-btn')
          .addEventListener('click', logout);
      } else {
        container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
      }
    } finally {
      spinner.classList.add('hidden');
    }
  },
  onDocumentSelected(documentId) {
    console.debug('Document selected, id:', documentId);
    session.setState('selectedDocumentId', documentId);
    const idLabel = document.getElementById('documents-selected-id');
    if (idLabel) idLabel.textContent = `ID: ${documentId}`;
    document.getElementById('documents-clear-btn').classList.remove('hidden');
    publish(TOPICS.DOCUMENT_ID, documentId);
  },

  clearSelection() {
    session.clearState('selectedDocumentId');
    const select  = document.getElementById('documents-dropdown');
    const idLabel = document.getElementById('documents-selected-id');
    if (select) {
      // Reset to the placeholder option
      select.selectedIndex = 0;
    }
    if (idLabel) idLabel.textContent = '';
    document.getElementById('documents-clear-btn').classList.add('hidden');
    publish(TOPICS.DOCUMENT_CLEARED);
  },
  async getDocuments() {
    const REPOSITORY_IDENTIFIER = session.config.repositoryIdentifier;
    const graphqlQuery = `
    {
  documents(
    repositoryIdentifier: "${REPOSITORY_IDENTIFIER}"
    from: "BuildingInspectionReport"
    orderBy: "DocumentTitle"
    where:"[IsCurrentVersion] = True"
  ) {
    documents {      
      name
      id
    }
  }
}`;

    return GraphQL.execute(graphqlQuery);
    
  }
});

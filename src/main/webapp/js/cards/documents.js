// cards/documents.js — Documents (REST) card
import { GraphQL,apiFetch, API } from '../api.js';
import { esc, renderWithToggle, renderJson } from '../util.js';
import { registerCard } from './registry.js';
import { session } from '../session.js';
import { publish, TOPICS } from '../eventBus.js';
import { logout } from '../router.js';


registerCard({
  id: 'documents',
  size: 'normal',
  html: () => `
    <div class="card" id="card-documents">
      <h2>Documents</h2>
      <button id="documents-btn">Get documents</button>
      <div id="documents-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Loading…
      </div>
      <div id="documents-result" class="card-result"></div>
    </div>`,
  init() {
    document.getElementById('documents-btn').addEventListener('click', async () => {
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
        placeholder.selected = true;
        select.appendChild(placeholder);

        sorted.forEach(doc => {
          const option = document.createElement('option');
          option.value = doc.id;
          option.textContent = esc(doc.name);
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
    });
  },
  onDocumentSelected(documentId) {
    console.log('Document selected, id:', documentId);
    const idLabel = document.getElementById('documents-selected-id');
    if (idLabel) idLabel.textContent = `ID: ${documentId}`;
    publish(TOPICS.DOCUMENT_ID, documentId);
  },
  testFunction(arg) {
    console.log(arg);
  },
  async getDocuments() {
    const REPOSITORY_IDENTIFIER = session.config.repositoryIdentifier;
    var graphqlQuery = `
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
/*
        console.log(graphqlQuery);
            const data = GraphQL.execute(graphqlQuery);
            console.log(data);
            return data;
            */

    return GraphQL.execute(graphqlQuery);
    
  }
});

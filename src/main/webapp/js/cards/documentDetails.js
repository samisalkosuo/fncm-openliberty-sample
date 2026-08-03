// cards/documentDetails.js — Document Details card
//
// Subscribes to TOPICS.DOCUMENT_ID, fetches full document details (including
// custom properties) via GraphQL, and renders them.
import { GraphQL } from '../api.js';
import { esc } from '../util.js';
import { subscribe, TOPICS } from '../eventBus.js';
import { session } from '../session.js';
import { registerCard } from './registry.js';
import { logout } from '../router.js';

registerCard({
  id: 'document-details',
  size: 'normal',
  html: () => `
    <div class="card" id="card-document-details">
      <h2>Document Details</h2>
      <button id="document-details-refresh-btn" class="hidden">Refresh</button>
      <div id="document-details-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Loading…
      </div>
      <div id="document-details-result" class="card-result">
        <p class="text-muted">Select a document from the Documents card to see its details here.</p>
      </div>
    </div>`,
  currentDocumentId: null,
  init() {
    const spinner     = document.getElementById('document-details-spinner');
    const container   = document.getElementById('document-details-result');
    const refreshBtn  = document.getElementById('document-details-refresh-btn');

    refreshBtn.addEventListener('click', () => {
      if (this.currentDocumentId) this.fetchAndRender(this.currentDocumentId);
    });

    subscribe(TOPICS.DOCUMENT_ID, (documentId) => {
      this.currentDocumentId = documentId;
      refreshBtn.classList.remove('hidden');
      this.fetchAndRender(documentId);
    });
  },
  async fetchAndRender(documentId) {
    const spinner   = document.getElementById('document-details-spinner');
    const container = document.getElementById('document-details-result');
    spinner.classList.remove('hidden');
    container.innerHTML = '';
    try {
      const result = await this.getDocumentDetails(documentId);
      const doc = result?.data?.document;
      if (!doc) {
        container.innerHTML = '<p class="text-muted">No document data returned.</p>';
        return;
      }
      const props = doc.properties ?? [];
      const propRows = props.map(p =>
        `<tr><th>${esc(p.id)}</th><td>${esc(p.value ?? '')}</td></tr>`
      ).join('');

      container.innerHTML = `
        <table>
          <tbody>
            <tr><th>Name</th><td>${esc(doc.name)}</td></tr>
            <tr><th>ID</th>  <td>${esc(doc.id)}</td></tr>
          </tbody>
        </table>
        <h3>Custom Properties</h3>
        <table>
          <thead><tr><th>Property</th><th>Value</th></tr></thead>
          <tbody>${propRows}</tbody>
        </table>`;
    } catch (err) {
      if (err.status === 401) {
        container.innerHTML = `
          <div class="alert alert-error">
            <strong>401 Unauthorized</strong> — ${esc(err.message)}
            <br><br>
            <button id="document-details-relogin-btn">Sign in again</button>
          </div>`;
        document.getElementById('document-details-relogin-btn')
          .addEventListener('click', logout);
      } else {
        container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
      }
    } finally {
      spinner.classList.add('hidden');
    }
  },
  async getDocumentDetails(documentId) {
    const repo = session.config.repositoryIdentifier;
    const query = `
      {
        document(
          repositoryIdentifier: "${repo}"
          identifier: "${documentId}"
        ) {
          name
          id
          properties(includes: ["Municipality","InspectionDate","InspectorName","PropertyAddress","ComplianceStatus","BuildingType"]) {
            id
            value
          }
        }
      }`;
    return GraphQL.execute(query);
  },
});

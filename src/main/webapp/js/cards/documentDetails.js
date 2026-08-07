// cards/documentDetails.js — Document Details card
//
// Subscribes to TOPICS.DOCUMENT_ID, fetches full document details (including
// custom properties) via GraphQL, and renders them.  Includes inline editing
// of all six custom properties via the updateDocument GraphQL mutation.
import { GraphQL, apiFetch, API } from '../api.js';
import { esc } from '../util.js';
import { subscribe, TOPICS } from '../eventBus.js';
import { session } from '../session.js';
import { registerCard } from './registry.js';
import { logout } from '../router.js';
import { BUILDING_TYPE_OPTIONS, COMPLIANCE_STATUS_OPTIONS } from './buildingInspectionConstants.js';

// Converts a YYYY-MM-DD date string to the ISO-8601 datetime format that
// FileNet expects: 2026-07-02T00:00:00Z (midnight UTC).
function toFileNetDateTime(date) {
  if (!date) return date;
  // Already contains a time component — pass through unchanged
  if (date.includes('T')) return date;
  return `${date}T00:00:00Z`;
}

registerCard({
  id: 'document-details',
  size: 'normal',
  html: () => `
    <div class="card" id="card-document-details">
      <h2>Document Details</h2>
      <div style="display:flex;gap:0.5rem;flex-wrap:wrap;margin-bottom:0.5rem">
        <button id="document-details-refresh-btn" class="hidden">Refresh</button>
        <button id="document-details-edit-btn"    class="hidden">Edit</button>
      </div>
      <div id="document-details-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Loading…
      </div>
      <div id="document-details-result" class="card-result">
        <p class="text-muted">Select a document from the Documents card to see its details here.</p>
      </div>
      <div id="document-details-edit-form" class="hidden card-result"></div>
    </div>`,

  currentDocumentId: null,
  currentDoc: null,

  init() {
    const refreshBtn = document.getElementById('document-details-refresh-btn');
    const editBtn    = document.getElementById('document-details-edit-btn');

    refreshBtn.addEventListener('click', () => {
      if (this.currentDocumentId) this.fetchAndRender(this.currentDocumentId);
    });

    editBtn.addEventListener('click', () => {
      if (this.currentDoc) this.showEditForm();
    });

    subscribe(TOPICS.DOCUMENT_ID, (documentId) => {
      this.currentDocumentId = documentId;
      refreshBtn.classList.remove('hidden');
      this.fetchAndRender(documentId);
    });

    subscribe(TOPICS.DOCUMENT_CLEARED, () => this.clearDetails());
  },

  clearDetails() {
    this.currentDocumentId = null;
    this.currentDoc = null;

    const container  = document.getElementById('document-details-result');
    const editForm   = document.getElementById('document-details-edit-form');
    const refreshBtn = document.getElementById('document-details-refresh-btn');
    const editBtn    = document.getElementById('document-details-edit-btn');

    editForm.classList.add('hidden');
    editForm.innerHTML = '';
    container.classList.remove('hidden');
    container.innerHTML = '<p class="text-muted">Select a document from the Documents card to see its details here.</p>';
    refreshBtn.classList.add('hidden');
    editBtn.classList.add('hidden');
  },

  // ── Read-only fetch ────────────────────────────────────────────────────────
  async fetchAndRender(documentId) {
    const spinner   = document.getElementById('document-details-spinner');
    const container = document.getElementById('document-details-result');
    const editForm  = document.getElementById('document-details-edit-form');

    // Always return to read-only mode on a fresh fetch
    editForm.classList.add('hidden');
    container.classList.remove('hidden');
    spinner.classList.remove('hidden');
    container.innerHTML = '';
    try {
      const result = await this.getDocumentDetails(documentId);
      const doc = result?.data?.document;
      if (!doc) {
        container.innerHTML = '<p class="text-muted">No document data returned.</p>';
        return;
      }
      this.renderDetails(doc);
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

  // ── Shared read-only renderer (used by fetchAndRender + saveEdit) ──────────
  renderDetails(doc) {
    this.currentDoc = doc;
    const container = document.getElementById('document-details-result');
    const editBtn   = document.getElementById('document-details-edit-btn');

    const props    = doc.properties ?? [];
    const propRows = props.map(p =>
      `<tr><th>${esc(p.id)}</th><td>${esc(p.value ?? '')}</td></tr>`
    ).join('');

    const contentElements = doc.contentElements ?? [];
    const contentItems = contentElements.map(ce =>
      `<li><a href="#" class="download-link" data-doc-id="${esc(doc.id)}" data-name="${esc(ce.retrievalName ?? '')}">${esc(ce.retrievalName ?? '')}</a> <span class="text-muted">${esc(ce.contentType ?? '')}</span></li>`
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
      </table>
      <h3>Content Elements</h3>
      <ul>${contentItems || '<li class="text-muted">None</li>'}</ul>`;

    // Attach download handlers — use apiFetch so the Bearer token is sent
    container.querySelectorAll('.download-link').forEach(link => {
      link.addEventListener('click', async (e) => {
        e.preventDefault();
        const docId = link.dataset.docId;
        const name  = link.dataset.name;
        const url   = `${API.downloadDocument}?documentId=${encodeURIComponent(docId)}&retrievalName=${encodeURIComponent(name)}`;
        try {
          const res = await apiFetch(url);
          if (!res.ok) throw new Error(`Download failed: HTTP ${res.status}`);
          const blob    = await res.blob();
          const objUrl  = URL.createObjectURL(blob);
          const anchor  = document.createElement('a');
          anchor.href     = objUrl;
          anchor.download = name;
          anchor.click();
          URL.revokeObjectURL(objUrl);
        } catch (err) {
          alert(`Could not download "${name}": ${err.message}`);
        }
      });
    });

    editBtn.classList.remove('hidden');
  },

  // ── Edit mode ──────────────────────────────────────────────────────────────
  async showEditForm() {
    const container   = document.getElementById('document-details-result');
    const editForm    = document.getElementById('document-details-edit-form');
    const refreshBtn  = document.getElementById('document-details-refresh-btn');
    const editBtn     = document.getElementById('document-details-edit-btn');

    refreshBtn.classList.add('hidden');
    editBtn.classList.add('hidden');
    container.classList.add('hidden');
    editForm.classList.remove('hidden');
    await this.checkoutDocument(this.currentDoc.id)
    this.renderEditForm(this.currentDoc);
  },
  async checkoutDocument(docId) {
    const checkoutMutation = `
    mutation checkoutDocument($repositoryIdentifier: String!, $documentId: String!) {
    checkoutDocument(
        repositoryIdentifier: $repositoryIdentifier,
        identifier: $documentId,
        )
      {
        id
        name
        contentElements {
          
          contentType
          elementSequenceNumber
          ... on ContentTransfer {
            className
            contentSize
            retrievalName
            downloadUrl
          }
        }
        reservation
        {
          id
          name
          dateCreated
        }
      }
    }
`;
    const result = await GraphQL.execute(checkoutMutation, {
      repositoryIdentifier: session.config.repositoryIdentifier,
      documentId: docId,
    });
    const reservationId = result?.data?.checkoutDocument?.reservation?.id;
    session.setState('reservationId', reservationId);
    console.debug(`Checkout done. Reservation ID: ${reservationId}`);
  },
  async checkinDocument(docId) {
    const checkinMutation = `
mutation checkinDocument($repositoryIdentifier: String!, $documentId: String!) {
  checkinDocument(
    repositoryIdentifier: $repositoryIdentifier,
    identifier: $documentId,
    checkinAction: {}
  ) {
    id
  }
}
`;
    const result = await GraphQL.execute(checkinMutation, {
      repositoryIdentifier: session.config.repositoryIdentifier,
      documentId: docId,
    });
    const newDocId = result?.data?.checkinDocument?.id;
    session.clearState('reservationId');
    console.debug(`Checkin done. Doc ID: ${newDocId}`);
    return newDocId;
  },
  async cancelCheckoutDocument(reservationId) {
    const cancelCheckoutMutation = `
    mutation cancelCheckout($repositoryIdentifier: String!, $reservationId: String!) {
      cancelDocumentCheckout(
        repositoryIdentifier: $repositoryIdentifier,
        identifier: $reservationId
      ) {
        id
      }
    }
  `;
    const result = await GraphQL.execute(cancelCheckoutMutation, {
      repositoryIdentifier: session.config.repositoryIdentifier,
      reservationId,
    });
    const canceledReservationId = result?.data?.cancelDocumentCheckout?.id;
    session.clearState('reservationId');
    console.debug(`Checkout canceled. Canceled Reservation ID: ${canceledReservationId}`);
  },

  // Returns the inner HTML for the content elements <ul> used in the edit form.
  buildContentListHtml(contentElements) {
    const items = (contentElements ?? []).map(ce =>
      `<li>${esc(ce.retrievalName ?? '')}</li>`
    ).join('');
    return items || '<li class="text-muted">None</li>';
  },

  renderEditForm(doc) {
    const editForm = document.getElementById('document-details-edit-form');

    // Build a lookup: property id → current value
    const propMap = {};
    for (const p of (doc.properties ?? [])) propMap[p.id] = p.value ?? '';

    // Helper: text input row
    const textField = (id, label, value) => `
      <div class="form-group">
        <label for="doc-edit-${id}">${esc(label)}</label>
        <input type="text" id="doc-edit-${id}" value="${esc(value)}" />
      </div>`;

    // Helper: date input row (slice to YYYY-MM-DD defensively)
    const dateField = (id, label, value) => `
      <div class="form-group">
        <label for="doc-edit-${id}">${esc(label)}</label>
        <input type="date" id="doc-edit-${id}" value="${esc(String(value ?? '').slice(0, 10))}" />
      </div>`;

    // Helper: select row
    const selectField = (id, label, options, value) => `
      <div class="form-group">
        <label for="doc-edit-${id}">${esc(label)}</label>
        <select id="doc-edit-${id}">
          ${options.map(o => `<option value="${esc(o)}"${o === value ? ' selected' : ''}>${esc(o)}</option>`).join('\n          ')}
        </select>
      </div>`;

    editForm.innerHTML = `
      ${textField('Municipality',    'Municipality',     propMap['Municipality'])}
      ${textField('PropertyAddress', 'Property Address', propMap['PropertyAddress'])}
      ${textField('InspectorName',   'Inspector Name',   propMap['InspectorName'])}
      ${dateField('InspectionDate',  'Inspection Date',  propMap['InspectionDate'])}
      ${selectField('BuildingType',     'Building Type',     BUILDING_TYPE_OPTIONS,     propMap['BuildingType'])}
      ${selectField('ComplianceStatus', 'Compliance Status', COMPLIANCE_STATUS_OPTIONS, propMap['ComplianceStatus'])}

      <h3>Content Elements</h3>
      <ul id="doc-edit-content-list">${this.buildContentListHtml(doc.contentElements)}</ul>
      <div class="form-group" style="display:flex;gap:0.5rem;flex-wrap:wrap;margin-bottom:0.5rem">
        <button id="doc-edit-add-file-btn" style="font-size:0.8rem;padding:0.25rem 0.6rem;background:#f0f4ff;color:#3b5bdb;border:1px solid #aab4e8;border-radius:4px;cursor:pointer">Add new file</button>
        <button id="doc-edit-replace-btn" style="font-size:0.8rem;padding:0.25rem 0.6rem;background:#f0f4ff;color:#3b5bdb;border:1px solid #aab4e8;border-radius:4px;cursor:pointer">Replace file</button>
        <span id="doc-edit-upload-status" class="text-muted" style="align-self:center"></span>
      </div>
      <input type="file" id="doc-edit-file-input" style="display:none" />

      <div id="doc-edit-error"></div>

      <div id="doc-edit-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Saving…
      </div>
      <hr style="margin:1.5rem 0">
      <div class="form-group" style="display:flex;gap:0.5rem;flex-wrap:wrap">
        <button id="doc-edit-save-btn">Save</button>
        <button id="doc-edit-cancel-btn">Cancel</button>
      </div>`;

    document.getElementById('doc-edit-save-btn').addEventListener('click', () => this.saveEdit());
    document.getElementById('doc-edit-cancel-btn').addEventListener('click', () => this.cancelEdit());

    // ── Content element buttons ───────────────────────────────────────────
    let pendingMode = null;
    const fileInput   = document.getElementById('doc-edit-file-input');
    const addBtn      = document.getElementById('doc-edit-add-file-btn');
    const replaceBtn  = document.getElementById('doc-edit-replace-btn');

    addBtn.addEventListener('click', () => { pendingMode = 'add';     fileInput.click(); });
    replaceBtn.addEventListener('click', () => { pendingMode = 'replace'; fileInput.click(); });

    fileInput.addEventListener('change', () => {
      if (!fileInput.files.length) return;
      const file = fileInput.files[0];
      const mode = pendingMode;
      fileInput.value = '';
      this.uploadContentElement(mode, file);
    });
  },

  async uploadContentElement(mode, file) {
    const addBtn      = document.getElementById('doc-edit-add-file-btn');
    const replaceBtn  = document.getElementById('doc-edit-replace-btn');
    const statusEl    = document.getElementById('doc-edit-upload-status');
    const errorEl     = document.getElementById('doc-edit-error');

    addBtn.disabled     = true;
    replaceBtn.disabled = true;
    statusEl.textContent = 'Uploading…';
    errorEl.innerHTML   = '';

    try {
      const reservationId = session.getState('reservationId');
      const formData = new FormData();
      formData.append('documentId',    this.currentDocumentId);
      formData.append('reservationId', reservationId);
      formData.append('mode', mode);
      formData.append('file', file);

      const res = await apiFetch(API.updateContentElement, {
        method:  'POST',
        headers: {},
        body:    formData,
      });
      if (!res.ok) {
        const err = new Error(res.status === 401 ? 'Session expired. Please sign in again.' : `HTTP ${res.status}`);
        err.status = res.status;
        throw err;
      }

      // Re-query the reservation via GraphQL to get the updated content elements
      const result = await this.getDocumentDetails(reservationId);
      const updated = result?.data?.document?.contentElements ?? [];
      this.currentDoc = { ...this.currentDoc, contentElements: updated };

      document.getElementById('doc-edit-content-list').innerHTML =
        this.buildContentListHtml(updated);
      statusEl.textContent = 'Upload successful.';

    } catch (err) {
      if (err.status === 401) {
        errorEl.innerHTML = `<div class="alert alert-error"><strong>401 Unauthorized</strong> — ${esc(err.message)}</div>`;
      } else {
        errorEl.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
      }
      statusEl.textContent = '';
    } finally {
      addBtn.disabled     = false;
      replaceBtn.disabled = false;
    }
  },

  async cancelEdit() {
    await this.cancelCheckoutDocument(session.getState('reservationId'));
    const container  = document.getElementById('document-details-result');
    const editForm   = document.getElementById('document-details-edit-form');
    const refreshBtn = document.getElementById('document-details-refresh-btn');
    const editBtn    = document.getElementById('document-details-edit-btn');
    editForm.classList.add('hidden');
    editForm.innerHTML = '';
    container.classList.remove('hidden');
    refreshBtn.classList.remove('hidden');
    editBtn.classList.remove('hidden');
  },

  // ── Save handler ───────────────────────────────────────────────────────────
  async saveEdit() {
    const errorEl  = document.getElementById('doc-edit-error');
    const spinner  = document.getElementById('doc-edit-spinner');
    const saveBtn  = document.getElementById('doc-edit-save-btn');
    const cancelBtn = document.getElementById('doc-edit-cancel-btn');
    errorEl.innerHTML = '';

    const municipality     = document.getElementById('doc-edit-Municipality').value.trim();
    const propertyAddress  = document.getElementById('doc-edit-PropertyAddress').value.trim();
    const inspectorName    = document.getElementById('doc-edit-InspectorName').value.trim();
    const inspectionDate   = document.getElementById('doc-edit-InspectionDate').value.trim();
    const buildingType     = document.getElementById('doc-edit-BuildingType').value;
    const complianceStatus = document.getElementById('doc-edit-ComplianceStatus').value;

    // Client-side validation
    const missing = [];
    if (!municipality)     missing.push('Municipality');
    if (!propertyAddress)  missing.push('Property Address');
    if (!inspectorName)    missing.push('Inspector Name');
    if (!inspectionDate)   missing.push('Inspection Date');
    if (!buildingType)     missing.push('Building Type');
    if (!complianceStatus) missing.push('Compliance Status');
    if (missing.length) {
      errorEl.innerHTML = `<div class="alert alert-error">Please fill in all required fields: ${esc(missing.join(', '))}.</div>`;
      return;
    }

    spinner.classList.remove('hidden');
    saveBtn.disabled  = true;
    cancelBtn.disabled = true;

    try {
      const payload = {
        documentId:       this.currentDocumentId,
        reservationId:    session.getState('reservationId'),
        municipality,
        propertyAddress,
        inspectorName,
        inspectionDate:   toFileNetDateTime(inspectionDate),
        buildingType,
        complianceStatus,
      };

      const res = await apiFetch(API.checkinDocument, {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify(payload),
      });
      if (!res.ok) {
        const err = new Error(res.status === 401 ? 'Session expired. Please sign in again.' : `HTTP ${res.status}`);
        err.status = res.status;
        throw err;
      }
      const result = await res.json();
      if (result.status !== 'ok') {
        throw new Error(result.message ?? 'Checkin failed.');
      }
      console.debug(`Check in done: ${JSON.stringify(result)}`);
      this.currentDocumentId = result.documentId;

      // Switch back to read-only and reload the newly checked-in document version
      const editForm   = document.getElementById('document-details-edit-form');
      const container  = document.getElementById('document-details-result');
      const refreshBtn = document.getElementById('document-details-refresh-btn');
      editForm.classList.add('hidden');
      editForm.innerHTML = '';
      container.classList.remove('hidden');
      refreshBtn.classList.remove('hidden');
      session.clearState('reservationId');
      await this.fetchAndRender(this.currentDocumentId);

    } catch (err) {
      if (err.status === 401) {
        errorEl.innerHTML = `
          <div class="alert alert-error">
            <strong>401 Unauthorized</strong> — ${esc(err.message)}
            <br><br>
            <button id="doc-edit-relogin-btn">Sign in again</button>
          </div>`;
        document.getElementById('doc-edit-relogin-btn').addEventListener('click', logout);
      } else {
        errorEl.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
      }
    } finally {
      spinner.classList.add('hidden');
      if (saveBtn) saveBtn.disabled  = false;
      if (cancelBtn) cancelBtn.disabled = false;
    }
  },

  // ── GraphQL query ──────────────────────────────────────────────────────────
  async getDocumentDetails(documentId) {
    const query = `
      query($repositoryIdentifier: String!, $documentId: String!) {
        document(
          repositoryIdentifier: $repositoryIdentifier
          identifier: $documentId
        ) {
          name
          id
          properties(includes: ["Municipality","InspectionDate","InspectorName","PropertyAddress","ComplianceStatus","BuildingType"]) {
            id
            value
          }
        contentElements {
          ... on ContentTransfer {
            retrievalName
            contentType
          }
        }

        }
      }`;
    return GraphQL.execute(query, {
      repositoryIdentifier: session.config.repositoryIdentifier,
      documentId,
    });
  },
});

// cards/documentDetails.js — Document Details card
//
// Subscribes to TOPICS.DOCUMENT_ID, fetches full document details (including
// custom properties) via GraphQL, and renders them.  Includes inline editing
// of all six custom properties via the updateDocument GraphQL mutation.
import { GraphQL } from '../api.js';
import { esc } from '../util.js';
import { subscribe, TOPICS } from '../eventBus.js';
import { session } from '../session.js';
import { registerCard } from './registry.js';
import { logout } from '../router.js';

// Choice list values — kept in sync with createBuildingInspectionReportDocument.js
const BUILDING_TYPE_OPTIONS      = ['Unknown', 'Residential', 'Commercial', 'Industrial', 'Public'];
const COMPLIANCE_STATUS_OPTIONS  = ['Unknown', 'Fully Compliant', 'Mostly Compliant', 'Partially Compliant', 'Non-Compliant', 'Requires Follow-up'];

// Escape a string value for safe inlining inside a GraphQL argument string literal.
function escPropValue(v) {
  return String(v ?? '').replace(/\\/g, '\\\\').replace(/"/g, '\\"');
}

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

    editBtn.classList.remove('hidden');
  },

  // ── Edit mode ──────────────────────────────────────────────────────────────
  async showEditForm() {
    const container = document.getElementById('document-details-result');
    const editForm  = document.getElementById('document-details-edit-form');

    container.classList.add('hidden');
    editForm.classList.remove('hidden');
    await this.checkoutDocument(this.currentDoc.id)
    this.renderEditForm(this.currentDoc);
  },

  async checkoutDocument(docId) {
    const repo = session.config.repositoryIdentifier;

    const checkoutMutation = `
    mutation checkoutDocument{
    checkoutDocument(
        repositoryIdentifier:"${escPropValue(repo)}",
        identifier:"${escPropValue(docId)}",
        )
      {
        id
        name
        reservation
        {
          id
          name
          dateCreated
        }
      }
    }
`;
    const result = await GraphQL.execute(checkoutMutation);
    const reservationId = result?.data?.checkoutDocument?.reservation?.id;
    session.setState('reservationId', reservationId);
    console.log(`Checkout done. Reservation ID: ${reservationId}`);
  },
  async cancelCheckoutDocument(reservationId) {
    const repo = session.config.repositoryIdentifier;

    const cancelCheckoutMutation = `
    mutation cancelCheckout {
      cancelDocumentCheckout(
        repositoryIdentifier:"${escPropValue(repo)}",
      identifier:"${escPropValue(reservationId)}",
      ) 
      {
        id
      }
    }
  `;
    const result = await GraphQL.execute(cancelCheckoutMutation);
    const canceledReservationId = result?.data?.cancelDocumentCheckout?.id;
    session.clearState('reservationId');
    console.log(`Checkout canceled. Canceled Reservation ID: ${canceledReservationId}`);
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

      <div id="doc-edit-error"></div>

      <div id="doc-edit-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Saving…
      </div>

      <div class="form-group" style="display:flex;gap:0.5rem;flex-wrap:wrap">
        <button id="doc-edit-save-btn">Save</button>
        <button id="doc-edit-cancel-btn">Cancel</button>
      </div>`;

    document.getElementById('doc-edit-save-btn').addEventListener('click', () => this.saveEdit());
    document.getElementById('doc-edit-cancel-btn').addEventListener('click', () => this.cancelEdit());
  },

  async cancelEdit() {

    await this.cancelCheckoutDocument(session.getState('reservationId'));
    const container = document.getElementById('document-details-result');
    const editForm  = document.getElementById('document-details-edit-form');
    editForm.classList.add('hidden');
    editForm.innerHTML = '';
    container.classList.remove('hidden');
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
      const repo = session.config.repositoryIdentifier;
      const id   = this.currentDocumentId;
      const mutation = ` mutation
        {
          updateDocument(
            repositoryIdentifier: "${escPropValue(repo)}"
            identifier: "${escPropValue(id)}"
            documentProperties: {
              properties: [
                {Municipality: "${escPropValue(municipality)}"}
                {InspectionDate: "${escPropValue(toFileNetDateTime(inspectionDate))}"}
                {InspectorName: "${escPropValue(inspectorName)}"}
                {PropertyAddress: "${escPropValue(propertyAddress)}"}
                {ComplianceStatus: "${escPropValue(complianceStatus)}"}
                {BuildingType: "${escPropValue(buildingType)}"}
              ]
            }
          ) {
            name
            id
            properties(includes: ["Municipality","InspectionDate","InspectorName","PropertyAddress","ComplianceStatus","BuildingType"]) {
              id
              value
            }
          }
        }`;
      const result = await GraphQL.execute(mutation);
      const updated = result?.data?.updateDocument;
      if (!updated) {
        throw new Error('No data returned from updateDocument.');
      }

      // Switch back to read-only and refresh with the mutation response
      const editForm  = document.getElementById('document-details-edit-form');
      const container = document.getElementById('document-details-result');
      editForm.classList.add('hidden');
      editForm.innerHTML = '';
      container.classList.remove('hidden');
      this.renderDetails(updated);

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

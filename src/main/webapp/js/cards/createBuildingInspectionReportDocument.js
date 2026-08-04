// cards/createDocument.js — Create a new document in FileNet
//
//   card wrapper id : card-create-document
//   button id       : create-document-btn
//   spinner id      : create-document-spinner
//   result id       : create-document-result
import { apiFetch, API } from '../api.js';
import { esc, renderJson } from '../util.js';
import { registerCard } from './registry.js';

// Choice list values for buildingType
const BUILDING_TYPE_OPTIONS = ['Unknown','Residential', 'Commercial', 'Industrial', 'Public'];

// Choice list values for complianceStatus
const COMPLIANCE_STATUS_OPTIONS = ['Unknown','Fully Compliant','Mostly Compliant','Partially Compliant','Non-Compliant','Requires Follow-up'];

// ── Test-data helpers ────────────────────────────────────────────────────────
const _pick = arr => arr[Math.floor(Math.random() * arr.length)];

const TEST_MUNICIPALITIES  = ['Helsinki', 'Espoo', 'Vantaa', 'Kuopio', 'Turku', 'Oulu', 'Kouvola', 'Lappeenranta', 'Kotka', 'Salo', 'Jyväskylä', 'Utsjoki', 'Hämeenlinna', 'Savonlinna', 'Tampere',"Rovaniemi"];
const TEST_STREET_TYPES    = ['Katu', 'Tie', 'Kuja', 'Bulevardi', 'Polku'];
const TEST_FIRST_NAMES     = [
  'Mikko', 'Juhani', 'Pekka', 'Matti', 'Antti', 'Jari', 'Timo', 'Kari', 'Sakari', 
  'Anna', 'Maria', 'Laura', 'Sari', 'Tiina', 'Päivi', 'Leena', 'Hanna', 'Sara,'
];
const TEST_LAST_NAMES      = [
  'Virtanen', 'Korhonen', 'Mäkinen', 'Nieminen', 'Mäkelä',
  'Hämäläinen', 'Leinonen', 'Koskinen', 'Heikkinen', 'Järvinen',
];

function fillTestData() {
  // Municipality
  document.getElementById('create-document-municipality').value = _pick(TEST_MUNICIPALITIES);

  // Property address: e.g. "Puistokatu 42"
  document.getElementById('create-document-property-address').value =
    `${_pick(TEST_STREET_TYPES)} ${Math.floor(Math.random() * 100) + 1}`;

  // Inspector name: random first + last
  document.getElementById('create-document-inspector-name').value =
    `${_pick(TEST_FIRST_NAMES)} ${_pick(TEST_LAST_NAMES)}`;

  // Inspection date: random day within the last 120 days
  const daysAgo = Math.floor(Math.random() * 120);
  const d = new Date();
  d.setDate(d.getDate() - daysAgo);
  document.getElementById('create-document-inspection-date').value =
    d.toISOString().slice(0, 10);

  // Building type — skip index 0 ("Unknown") for more realistic data
  const btSelect = document.getElementById('create-document-building-type');
  btSelect.value = _pick(BUILDING_TYPE_OPTIONS.slice(1));

  // Compliance status — skip index 0 ("Unknown")
  const csSelect = document.getElementById('create-document-compliance-status');
  csSelect.value = _pick(COMPLIANCE_STATUS_OPTIONS.slice(1));
}
// ────────────────────────────────────────────────────────────────────────────

registerCard({
  id: 'create-document',
  size: 'normal',
  html: () => `
    <div class="card" id="card-create-document">
      <h2>Create Building Inspection Report Document</h2>
      <p>Fill in the properties and select a file to create a new Building Inspection Report document in FileNet.<br/>
      <span style="font-size:0.66rem">Fields marked <span style="color:red">*</span> are required.<span></p>
      <div class="form-group" style="display:flex;gap:0.5rem;align-items:center;flex-wrap:wrap">
        <button id="create-document-fill-btn" type="button" style="font-size:0.78rem;padding:0.25rem 0.6rem">Fill test data</button>
      </div>

      <div class="form-group">
        <label for="create-document-municipality">Municipality <span style="color:red">*</span></label>
        <input type="text" id="create-document-municipality" placeholder="Enter municipality…" required />
      </div>
      <div class="form-group">
        <label for="create-document-property-address">Property Address <span style="color:red">*</span></label>
        <input type="text" id="create-document-property-address" placeholder="Enter property address…" required />
      </div>
      <div class="form-group">
        <label for="create-document-inspector-name">Inspector Name <span style="color:red">*</span></label>
        <input type="text" id="create-document-inspector-name" placeholder="Enter inspector name…" required />
      </div>
      <div class="form-group">
        <label for="create-document-inspection-date">Inspection Date <span style="color:red">*</span></label>
        <input type="date" id="create-document-inspection-date" required />
      </div>
      <div class="form-group">
        <label for="create-document-building-type">Building Type <span style="color:red">*</span></label>
        <select id="create-document-building-type" required>
          <option value="">— select —</option>
          ${BUILDING_TYPE_OPTIONS.map(o => `<option value="${esc(o)}">${esc(o)}</option>`).join('\n          ')}
        </select>
      </div>
      <div class="form-group">
        <label for="create-document-compliance-status">Compliance Status <span style="color:red">*</span></label>
        <select id="create-document-compliance-status" required>
          <option value="">— select —</option>
          ${COMPLIANCE_STATUS_OPTIONS.map(o => `<option value="${esc(o)}">${esc(o)}</option>`).join('\n          ')}
        </select>
      </div>
      <div class="form-group">
        <label for="create-document-file">File</label>
        <input type="file" id="create-document-file" />
      </div>
      <div class="form-group" style="display:flex;gap:0.5rem;align-items:center;flex-wrap:wrap">
        <button id="create-document-btn">Create Document</button>
      </div>
      <div id="create-document-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Creating…
      </div>
      <div id="create-document-result" class="card-result"></div>
    </div>`,
  init() {
    document.getElementById('create-document-fill-btn').addEventListener('click', fillTestData);
    document.getElementById('create-document-btn').addEventListener('click', async () => {
      const spinner   = document.getElementById('create-document-spinner');
      const container = document.getElementById('create-document-result');
      spinner.classList.remove('hidden');
      container.innerHTML = '';

      try {
        const municipality     = document.getElementById('create-document-municipality').value.trim();
        const propertyAddress  = document.getElementById('create-document-property-address').value.trim();
        const inspectorName    = document.getElementById('create-document-inspector-name').value.trim();
        const inspectionDate   = document.getElementById('create-document-inspection-date').value;
        const buildingType     = document.getElementById('create-document-building-type').value;
        const complianceStatus = document.getElementById('create-document-compliance-status').value;
        const fileInput        = document.getElementById('create-document-file');

        // Mandatory-field validation (browser-side)
        const missing = [];
        if (!municipality)     missing.push('Municipality');
        if (!propertyAddress)  missing.push('Property Address');
        if (!inspectorName)    missing.push('Inspector Name');
        if (!inspectionDate)   missing.push('Inspection Date');
        if (!buildingType)     missing.push('Building Type');
        if (!complianceStatus) missing.push('Compliance Status');
        if (missing.length) {
          throw new Error(`Please fill in all required fields: ${missing.join(', ')}.`);
        }

        // Build multipart/form-data — do NOT set Content-Type; browser sets boundary
        const formData = new FormData();
        formData.append('municipality',     municipality);
        formData.append('propertyAddress',  propertyAddress);
        formData.append('inspectorName',    inspectorName);
        formData.append('inspectionDate',   inspectionDate);
        formData.append('buildingType',     buildingType);
        formData.append('complianceStatus', complianceStatus);
        if (fileInput.files.length > 0)
        {
          formData.append('file', fileInput.files[0]);
        }

        // apiFetch must NOT forward a Content-Type header for multipart requests
        const res = await apiFetch(API.createBuildingInspectionReportDocument, {
          method: 'POST',
          body:   formData,
          // Prevent apiFetch from spreading a stale Content-Type from options.headers
          headers: {},
        });

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

// Made with Bob

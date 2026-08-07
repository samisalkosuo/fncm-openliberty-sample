// cards/createDocument.js — Create a new document in FileNet
//
//   card wrapper id : card-create-document
//   button id       : create-document-btn
//   spinner id      : create-document-spinner
//   result id       : create-document-result
import { apiFetch, API } from '../api.js';
import { esc, renderJson, formTextField, formDateField, formSelectField } from '../util.js';
import { registerCard } from './registry.js';
import { BUILDING_TYPE_OPTIONS, COMPLIANCE_STATUS_OPTIONS } from './buildingInspectionConstants.js';

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
  size: 'wide',
  html: () => `
    <div class="card" id="card-create-document">
      <h2>Create Building Inspection Report Document</h2>
      <p>Fill in the properties and select a file to create a new Building Inspection Report document in FileNet.<br/>
      <span style="font-size:0.66rem">Fields marked <span style="color:red">*</span> are required.<span></p>
      <div class="form-group" style="display:flex;gap:0.5rem;align-items:center;flex-wrap:wrap">
        <button id="create-document-fill-btn" type="button" style="font-size:0.78rem;padding:0.25rem 0.6rem">Fill test data</button>
      </div>

      <h3 style="margin:1rem 0 0.5rem">Custom Properties</h3>

      ${formTextField('create-document-municipality',    'Municipality',     '', { required: true, placeholder: 'Enter municipality…',     attrs: 'required' })}
      ${formTextField('create-document-property-address','Property Address', '', { required: true, placeholder: 'Enter property address…', attrs: 'required' })}
      ${formTextField('create-document-inspector-name',  'Inspector Name',   '', { required: true, placeholder: 'Enter inspector name…',   attrs: 'required' })}
      ${formDateField('create-document-inspection-date', 'Inspection Date',  '', { required: true, attrs: 'required' })}
      ${formSelectField('create-document-building-type',     'Building Type',     BUILDING_TYPE_OPTIONS,    '', { required: true, placeholder: '— select —' })}
      ${formSelectField('create-document-compliance-status', 'Compliance Status', COMPLIANCE_STATUS_OPTIONS, '', { required: true, placeholder: '— select —' })}

      <h3 style="margin:1rem 0 0.5rem">Content Element</h3>
      <div class="form-group" style="display:flex;gap:1.5rem;align-items:center;flex-wrap:wrap">
        <label style="display:flex;gap:0.4rem;align-items:center;cursor:pointer">
          <input type="radio" name="content-type" id="content-type-none" value="none" checked /> None
        </label>
        <label style="display:flex;gap:0.4rem;align-items:center;cursor:pointer">
          <input type="radio" name="content-type" id="content-type-file" value="file" /> Browse file
        </label>
        <label style="display:flex;gap:0.4rem;align-items:center;cursor:pointer">
          <input type="radio" name="content-type" id="content-type-text" value="text" /> Markdown content
        </label>
      </div>

      <div id="content-panel-file" class="form-group" style="display:none">
        <label for="create-document-file">File</label>
        <input type="file" id="create-document-file" />
      </div>

      <div id="content-panel-text" style="display:none">
        <div class="form-group">
          <label for="create-document-content-filename">Filename <span style="color:red">*</span></label>
          <div style="display:flex;align-items:center;gap:0.25rem">
            <input type="text" id="create-document-content-filename" value="content" style="max-width:18rem" placeholder="filename" />
            <span style="color:#57606a">.md</span>
          </div>
        </div>
        <div class="form-group">
          <label for="create-document-content-text">Content text <span style="color:red">*</span></label>
          <textarea id="create-document-content-text" rows="16" style="width:100%;box-sizing:border-box;font-family:monospace;font-size:0.9rem" placeholder="Enter markdown content…"></textarea>
        </div>
      </div>
      <hr style="margin:1.5rem 0">
      <div class="form-group" style="display:flex;gap:0.5rem;align-items:center;flex-wrap:wrap">
        <button id="create-document-btn">Create Document</button>
      </div>
      <div id="create-document-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Creating…
      </div>
      <div id="create-document-result" class="card-result"></div>
    </div>
`,
  init() {
    // Toggle content-element panels based on radio selection
    // MarkdownEditor is initialized lazily on first switch to "Text" mode —
    // it requires the textarea to be visible (non-null tagName check).
    const filePanelEl = document.getElementById('content-panel-file');
    const textPanelEl = document.getElementById('content-panel-text');
    let markdownEditorInitialized = false;
    const syncPanels = () => {
      const selected = document.querySelector('input[name="content-type"]:checked').value;
      filePanelEl.style.display = selected === 'file' ? '' : 'none';
      textPanelEl.style.display = selected === 'text' ? '' : 'none';
      // 'none' — both panels stay hidden; no extra work needed
      if (selected === 'text' && !markdownEditorInitialized) {
        new MarkdownEditor('#create-document-content-text');
        markdownEditorInitialized = true;
      }
    };
    document.getElementById('content-type-none').addEventListener('change', syncPanels);
    document.getElementById('content-type-file').addEventListener('change', syncPanels);
    document.getElementById('content-type-text').addEventListener('change', syncPanels);
    syncPanels(); // enforce default state on mount (none — both panels hidden)

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
        const contentMode      = document.querySelector('input[name="content-type"]:checked').value;
        const fileInput        = document.getElementById('create-document-file');
        const contentText      = document.getElementById('create-document-content-text').value;
        const contentFilename  = (document.getElementById('create-document-content-filename').value.trim() || 'content') + '.md';

        // Mandatory-field validation (browser-side)
        const missing = [];
        if (!municipality)                               missing.push('Municipality');
        if (!propertyAddress)                            missing.push('Property Address');
        if (!inspectorName)                              missing.push('Inspector Name');
        if (!inspectionDate)                             missing.push('Inspection Date');
        if (!buildingType)                               missing.push('Building Type');
        if (!complianceStatus)                           missing.push('Compliance Status');
        if (contentMode === 'text' && !contentText.trim()) missing.push('Content text');
        if (missing.length) {
          throw new Error(`Please fill in all required fields: ${missing.join(', ')}.`);
        }

        if (contentMode === 'file') {
          // ── File mode: existing multipart POST path ──────────────────────────
          const formData = new FormData();
          formData.append('municipality',     municipality);
          formData.append('propertyAddress',  propertyAddress);
          formData.append('inspectorName',    inspectorName);
          formData.append('inspectionDate',   inspectionDate);
          formData.append('buildingType',     buildingType);
          formData.append('complianceStatus', complianceStatus);
          if (fileInput.files.length > 0) {
            formData.append('file', fileInput.files[0]);
          }

          // apiFetch must NOT forward a Content-Type header for multipart requests
          const res = await apiFetch(API.createBuildingInspectionReportDocument, {
            method: 'POST',
            body:   formData,
            headers: {},
          });

          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          const data = await res.json();
          renderJson(container, data);

        } else {
          // ── Text mode: send textarea content as a markdown file to the
          // existing multipart endpoint — the backend's executeMultipart path
          // handles CONTENT_TRANSFER correctly for any file type.
          const textBlob = new Blob([contentText], { type: 'text/markdown; charset=UTF-8' });
          const formData = new FormData();
          formData.append('municipality',     municipality);
          formData.append('propertyAddress',  propertyAddress);
          formData.append('inspectorName',    inspectorName);
          formData.append('inspectionDate',   inspectionDate);
          formData.append('buildingType',     buildingType);
          formData.append('complianceStatus', complianceStatus);
          formData.append('file', textBlob, contentFilename);

          const res = await apiFetch(API.createBuildingInspectionReportDocument, {
            method: 'POST',
            body:   formData,
            headers: {},
          });

          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          const data = await res.json();
          renderJson(container, data);
        }
      } catch (err) {
        container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
      } finally {
        spinner.classList.add('hidden');
      }
    });
  },
});

// Made with Bob

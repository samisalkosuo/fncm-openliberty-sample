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
const COMPLIANCE_STATUS_OPTIONS = ['Unknown','Fully Compliant','Mostly Compliant','Partially Compliant','Non-Compliant','Requires Follow-Up'];

registerCard({
  id: 'create-document',
  size: 'normal',
  html: () => `
    <div class="card" id="card-create-document">
      <h2>Create Document</h2>
      <p>Fill in the properties and select a file to create a new document in FileNet.</p>
      <div class="form-group">
        <label for="create-document-municipality">Municipality</label>
        <input type="text" id="create-document-municipality" placeholder="Enter municipality…" />
      </div>
      <div class="form-group">
        <label for="create-document-property-address">Property Address</label>
        <input type="text" id="create-document-property-address" placeholder="Enter property address…" />
      </div>
      <div class="form-group">
        <label for="create-document-inspector-name">Inspector Name</label>
        <input type="text" id="create-document-inspector-name" placeholder="Enter inspector name…" />
      </div>
      <div class="form-group">
        <label for="create-document-inspection-date">Inspection Date</label>
        <input type="date" id="create-document-inspection-date" />
      </div>
      <div class="form-group">
        <label for="create-document-building-type">Building Type</label>
        <select id="create-document-building-type">
          ${BUILDING_TYPE_OPTIONS.map(o => `<option value="${esc(o)}">${esc(o)}</option>`).join('\n          ')}
        </select>
      </div>
      <div class="form-group">
        <label for="create-document-compliance-status">Compliance Status</label>
        <select id="create-document-compliance-status">
          ${COMPLIANCE_STATUS_OPTIONS.map(o => `<option value="${esc(o)}">${esc(o)}</option>`).join('\n          ')}
        </select>
      </div>
      <div class="form-group">
        <label for="create-document-file">File</label>
        <input type="file" id="create-document-file" />
      </div>
      <button id="create-document-btn">Create Document</button>
      <div id="create-document-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Creating…
      </div>
      <div id="create-document-result" class="card-result"></div>
    </div>`,
  init() {
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

        if (!fileInput.files.length) {
          throw new Error('Please select a file before submitting.');
        }

        // Build multipart/form-data — do NOT set Content-Type; browser sets boundary
        const formData = new FormData();
        formData.append('municipality',     municipality);
        formData.append('propertyAddress',  propertyAddress);
        formData.append('inspectorName',    inspectorName);
        formData.append('inspectionDate',   inspectionDate);
        formData.append('buildingType',     buildingType);
        formData.append('complianceStatus', complianceStatus);
        formData.append('file', fileInput.files[0]);

        // apiFetch must NOT forward a Content-Type header for multipart requests
        const res = await apiFetch(API.createDocument, {
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

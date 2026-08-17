// cards/searchBuildingInspection.js — Search Building Inspection Reports card
import { apiFetch, API } from '../api.js';
import { esc, renderWithToggle, runCardAction } from '../util.js';
import { publish, TOPICS } from '../eventBus.js';
import { registerCard } from './registry.js';

function buildResultsTable(items) {
  if (items.length === 0) {
    return '<p class="text-muted">No documents matched your search.</p>';
  }
  const rows = items.map(doc =>
    `<tr data-doc-id="${esc(doc.id)}"
         data-doc-name="${esc(doc.documentTitle ?? '')}"
         data-doc-class="BuildingInspectionReport"
         data-date-created="${esc(doc.dateCreated ?? '')}">
      <td><button class="link-btn" type="button">${esc(doc.documentTitle || doc.id)}</button></td>
      <td>${esc(doc.municipality ?? '')}</td>
      <td>${esc(doc.propertyAddress ?? '')}</td>
      <td>${esc(doc.inspectorName ?? '')}</td>
      <td>${esc(doc.buildingType ?? '')}</td>
      <td>${esc(doc.complianceStatus ?? '')}</td>
    </tr>`
  ).join('');
  return `<table>
    <thead>
      <tr>
        <th>Title</th>
        <th>Municipality</th>
        <th>Address</th>
        <th>Inspector</th>
        <th>Building Type</th>
        <th>Compliance</th>
      </tr>
    </thead>
    <tbody>${rows}</tbody>
  </table>`;
}

function handleDocumentClick(e) {
  const btn = e.target.closest('.link-btn');
  if (!btn) return;
  const row = btn.closest('tr');
  publish(TOPICS.DOCUMENT_ID, row.dataset.docId);
  publish(TOPICS.DOCUMENT_SELECTED, {
    id:          row.dataset.docId,
    name:        row.dataset.docName,
    className:   row.dataset.docClass,
    dateCreated: row.dataset.dateCreated,
  });
}

registerCard({
  id: 'search-building-inspection',
  size: 'wide',
  html: () => `
    <div class="card" id="card-search-building-inspection">
      <h2>Search Building Inspection Reports</h2>
      <p>Search all custom metadata fields for the entered text<br/>(note: search is case-sensitive).</p>
      <div class="form-group">
        <label for="search-building-inspection-query">Search</label>
        <input id="search-building-inspection-query" type="text"
               placeholder="Municipality, address, inspector, building type…" />
      </div>
      <button id="search-building-inspection-btn">Search</button>
      <div id="search-building-inspection-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Searching…
      </div>
      <div id="search-building-inspection-result" class="card-result"></div>
    </div>`,
  init() {
    const input = document.getElementById('search-building-inspection-query');

    // Allow pressing Enter in the search field to trigger search
    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') this.doSearch();
    });

    document.getElementById('search-building-inspection-btn')
      .addEventListener('click', () => this.doSearch());
  },

  async doSearch() {
    const q = document.getElementById('search-building-inspection-query').value.trim();
    const container = document.getElementById('search-building-inspection-result');

    if (!q) {
      container.innerHTML = `<div class="alert alert-error">Search text is required.</div>`;
      return;
    }

    await runCardAction(
      'search-building-inspection-spinner',
      'search-building-inspection-result',
      async (container) => {
        const res = await apiFetch(`${API.searchBuildingInspection}?q=${encodeURIComponent(q)}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();

        renderWithToggle(container, data, (el, d) => {
          const docs  = d?.documents ?? [];
          const count = d?.count ?? 0;
          el.innerHTML =
            `<p><strong>${count}</strong> result${count !== 1 ? 's' : ''} for <em>${esc(d.query ?? q)}</em></p>`
            + buildResultsTable(docs);

          el.querySelector('tbody')?.addEventListener('click', handleDocumentClick);
        });
      }
    );
  },
});

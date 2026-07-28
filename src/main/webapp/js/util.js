// util.js — shared DOM helpers
export function showAlert(el, msg) {
  el.textContent = msg;
  el.classList.remove('hidden');
}

export function esc(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// ── JSON rendering ────────────────────────────────────────────────────────────

/**
 * Render `data` as an expandable JSON tree inside `container`.
 * All top-level nodes start open. Nested objects/arrays are collapsible.
 */
export function renderJson(container, data) {
  container.innerHTML = '';
  const root = document.createElement('div');
  root.className = 'json-tree';
  root.appendChild(_buildNode(data, true));
  container.appendChild(root);
}

/**
 * Render `data` with a custom view (via `customRenderer`) as the default tab
 * and a JSON tree tab the user can switch to.
 * If `customRenderer` is omitted the JSON tree is shown directly (no toggle).
 *
 * @param {HTMLElement} container
 * @param {*} data
 * @param {((el: HTMLElement, data: *) => void) | null} [customRenderer]
 * @param {string} [customLabel]  label for the custom-view button (default "Table")
 */
export function renderWithToggle(container, data, customRenderer = null, customLabel = 'Table') {
  container.innerHTML = '';

  if (!customRenderer) {
    renderJson(container, data);
    return;
  }

  const customDiv = document.createElement('div');
  const treeDiv   = document.createElement('div');
  treeDiv.classList.add('hidden');

  customRenderer(customDiv, data);
  renderJson(treeDiv, data);

  const bar = document.createElement('div');
  bar.className = 'view-toggle';
  bar.innerHTML = `
    <button class="secondary active" data-view="custom">${esc(customLabel)}</button>
    <button class="secondary"        data-view="json">JSON Tree</button>`;

  bar.addEventListener('click', e => {
    const view = e.target.dataset.view;
    if (!view) return;
    treeDiv.classList.toggle('hidden',   view !== 'json');
    customDiv.classList.toggle('hidden', view !== 'custom');
    bar.querySelectorAll('button').forEach(b =>
      b.classList.toggle('active', b.dataset.view === view)
    );
  });

  container.append(bar, customDiv, treeDiv);
}

// ── internal helpers ──────────────────────────────────────────────────────────

function _buildNode(value, isRoot = false) {
  if (value === null || typeof value !== 'object') return _buildLeaf(value);

  const isArray  = Array.isArray(value);
  const entries  = isArray ? value.map((v, i) => [i, v]) : Object.entries(value);
  const label    = isArray ? `[ ${entries.length} ]` : `{ ${entries.length} }`;

  const details  = document.createElement('details');
  details.open   = isRoot;                       // only root starts open

  const summary  = document.createElement('summary');
  summary.className   = 'json-summary';
  summary.textContent = label;
  details.appendChild(summary);

  for (const [key, val] of entries) {
    const row = document.createElement('div');
    row.className = 'json-row';
    const keySpan = document.createElement('span');
    keySpan.className   = 'json-key';
    keySpan.textContent = isArray ? `[${key}] ` : `${key}: `;
    row.appendChild(keySpan);
    row.appendChild(_buildNode(val));
    details.appendChild(row);
  }
  return details;
}

function _buildLeaf(value) {
  const span = document.createElement('span');
  const type = value === null ? 'null' : typeof value;
  span.className   = `json-val json-${type}`;
  span.textContent = value === null ? 'null' : String(value);
  return span;
}

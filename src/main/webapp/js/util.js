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
 * Render `data` as an expandable JSON tree inside `container`, with a
 * Tree / Raw toggle bar above it.  Tree view is shown by default.
 */
export function renderJson(container, data) {
  container.innerHTML = '';

  // ── tree view ──
  const treeDiv = document.createElement('div');
  treeDiv.className = 'json-tree';
  treeDiv.appendChild(_buildNode(data, true));

  // ── raw view ──
  const rawDiv = document.createElement('div');
  rawDiv.className = 'json-tree json-raw hidden';
  const pre = document.createElement('pre');
  pre.textContent = JSON.stringify(data, null, 2);
  rawDiv.appendChild(pre);

  // ── toggle bar ──
  const bar = document.createElement('div');
  bar.className = 'view-toggle';
  bar.innerHTML = `
    <button class="secondary active" data-view="tree">🌲 Tree</button>
    <button class="secondary"        data-view="raw">{ } Raw</button>`;

  bar.addEventListener('click', e => {
    const view = e.target.dataset.view;
    if (!view) return;
    treeDiv.classList.toggle('hidden', view !== 'tree');
    rawDiv.classList.toggle('hidden',  view !== 'raw');
    bar.querySelectorAll('button').forEach(b =>
      b.classList.toggle('active', b.dataset.view === view)
    );
  });

  // ── copy button ──
  const copyBtn = document.createElement('button');
  copyBtn.className   = 'secondary json-copy-btn';
  copyBtn.textContent = '📋 Copy JSON';
  copyBtn.title       = 'Copy raw JSON to clipboard';
  copyBtn.addEventListener('click', () => {
    navigator.clipboard.writeText(JSON.stringify(data, null, 2)).then(() => {
      copyBtn.textContent = '✔ Copied!';
      setTimeout(() => { copyBtn.textContent = '📋 Copy JSON'; }, 1500);
    }).catch(() => {
      copyBtn.textContent = '✖ Failed';
      setTimeout(() => { copyBtn.textContent = '📋 Copy JSON'; }, 1500);
    });
  });
  bar.appendChild(copyBtn);

  container.append(bar, treeDiv, rawDiv);
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

// ── error alert helper ────────────────────────────────────────────────────────

/**
 * Renders an error alert inside `container`.
 * If `err.status === 401`, renders a 401 block with a "Sign in again" button
 * wired to `onReloginClick` (when provided).
 * Otherwise renders a plain `.alert.alert-error` block.
 *
 * @param {HTMLElement} container
 * @param {Error & { status?: number }} err
 * @param {(() => void) | null} [onReloginClick]
 */
export function showErrorAlert(container, err, onReloginClick = null) {
  if (err.status === 401) {
    container.innerHTML = `
      <div class="alert alert-error">
        <strong>401 Unauthorized</strong> — ${esc(err.message)}
        <br><br>
        <button class="relogin-btn">Sign in again</button>
      </div>`;
    if (onReloginClick) {
      container.querySelector('.relogin-btn').addEventListener('click', onReloginClick);
    }
  } else {
    container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
  }
}

// ── form-field builder helpers ────────────────────────────────────────────────

/**
 * Returns a `<div class="form-group">` string with a text input.
 * @param {string} id
 * @param {string} label
 * @param {string} [value='']
 * @param {{ required?: boolean, placeholder?: string, attrs?: string }} [opts]
 */
export function formTextField(id, label, value = '', { required = false, placeholder = '', attrs = '' } = {}) {
  const labelHtml = required ? `${esc(label)} <span style="color:red">*</span>` : esc(label);
  return `
    <div class="form-group">
      <label for="${esc(id)}">${labelHtml}</label>
      <input type="text" id="${esc(id)}" value="${esc(value)}"${placeholder ? ` placeholder="${esc(placeholder)}"` : ''}${attrs ? ` ${attrs}` : ''} />
    </div>`;
}

/**
 * Returns a `<div class="form-group">` string with a date input.
 * Value is sliced to YYYY-MM-DD defensively.
 * @param {string} id
 * @param {string} label
 * @param {string} [value='']
 * @param {{ required?: boolean, attrs?: string }} [opts]
 */
export function formDateField(id, label, value = '', { required = false, attrs = '' } = {}) {
  const labelHtml = required ? `${esc(label)} <span style="color:red">*</span>` : esc(label);
  const safeValue = esc(String(value ?? '').slice(0, 10));
  return `
    <div class="form-group">
      <label for="${esc(id)}">${labelHtml}</label>
      <input type="date" id="${esc(id)}" value="${safeValue}"${attrs ? ` ${attrs}` : ''} />
    </div>`;
}

/**
 * Returns a `<div class="form-group">` string with a `<select>`.
 * @param {string} id
 * @param {string} label
 * @param {string[]} options
 * @param {string} [selectedValue='']
 * @param {{ required?: boolean, placeholder?: string }} [opts]
 */
export function formSelectField(id, label, options, selectedValue = '', { required = false, placeholder = '' } = {}) {
  const labelHtml = required ? `${esc(label)} <span style="color:red">*</span>` : esc(label);
  const placeholderOption = placeholder
    ? `<option value="">${esc(placeholder)}</option>\n          `
    : '';
  const optionItems = options
    .map(o => `<option value="${esc(o)}"${o === selectedValue ? ' selected' : ''}>${esc(o)}</option>`)
    .join('\n          ');
  return `
    <div class="form-group">
      <label for="${esc(id)}">${labelHtml}</label>
      <select id="${esc(id)}"${required ? ' required' : ''}>
        ${placeholderOption}${optionItems}
      </select>
    </div>`;
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

  const btn = document.createElement('button');
  btn.className   = 'json-expand-btn';
  btn.textContent = '⊞';
  btn.title       = 'Expand all';
  btn.addEventListener('click', e => {
    e.stopPropagation();
    e.preventDefault();
    const parentDetails = btn.closest('details');
    const descendants   = [...parentDetails.querySelectorAll('details')];
    if (descendants.some(d => !d.open)) {
      parentDetails.open = true;
      descendants.forEach(d => { d.open = true; });
      btn.textContent = '⊟';
      btn.title       = 'Collapse all';
    } else {
      parentDetails.open = false;
      descendants.forEach(d => { d.open = false; });
      btn.textContent = '⊞';
      btn.title       = 'Expand all';
    }
  });
  summary.appendChild(btn);
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

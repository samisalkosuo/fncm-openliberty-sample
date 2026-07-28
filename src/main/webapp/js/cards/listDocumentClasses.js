// cards/listDocumentClasses.js — List Document Classes card
import { apiFetch, API } from '../api.js';
import { esc } from '../util.js';

export function init() {
  document.getElementById('list-document-classes-btn').addEventListener('click', async () => {
    const spinner   = document.getElementById('list-document-classes-spinner');
    const container = document.getElementById('list-document-classes-result');
    spinner.classList.remove('hidden');
    container.innerHTML = '';

    try {
      const res = await apiFetch(API.listDocumentClasses);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.text();
      container.innerHTML = `<pre>${data}</pre>`;
    } catch (err) {
      container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
    } finally {
      spinner.classList.add('hidden');
    }
  });
}

// cards/graphql.js — GraphQL Query card
import { GraphQL } from '../api.js';
import { esc, renderJson } from '../util.js';
import { registerCard } from './registry.js';

registerCard({
  id: 'graphql',
  size: 'normal',
  html: () => `
    <div class="card" id="card-graphql">
      <h2>GraphQL Query</h2>
      <div class="form-group">
        <label for="graphql-query">Query</label>
        <textarea id="graphql-query" class="code-input" rows="4">{ domain { objectStores { objectStores { symbolicName } }} }</textarea>
      </div>
      <button id="graphql-run-btn">Run</button>
      <div id="graphql-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Running…
      </div>
      <div id="graphql-result" class="card-result"></div>
    </div>`,
  init() {
    document.getElementById('graphql-run-btn').addEventListener('click', async () => {
      const query     = document.getElementById('graphql-query').value.trim();
      const spinner   = document.getElementById('graphql-spinner');
      const container = document.getElementById('graphql-result');
      spinner.classList.remove('hidden');
      container.innerHTML = '';

      try {
        const data = await GraphQL.execute(query);
        renderJson(container, data);
      } catch (err) {
        container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
      } finally {
        spinner.classList.add('hidden');
      }
    });
  },
});

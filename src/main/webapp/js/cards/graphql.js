// cards/graphql.js — GraphQL Query card
import { apiFetch, API } from '../api.js';
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
      <pre id="graphql-result" class="code-result"></pre>
    </div>`,
  init() {
    document.getElementById('graphql-run-btn').addEventListener('click', async () => {
      const query  = document.getElementById('graphql-query').value.trim();
      const result = document.getElementById('graphql-result');
      result.textContent = 'Running…';

      try {
        const res = await apiFetch(API.graphql, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ query }),
        });
        const json = await res.json();
        result.textContent = JSON.stringify(json, null, 2);
      } catch (err) {
        result.textContent = 'Error: ' + err.message;
      }
    });
  },
});

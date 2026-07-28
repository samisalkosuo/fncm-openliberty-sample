// cards/graphql.js — GraphQL query card
import { apiFetch, API } from '../api.js';

export function init() {
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
}

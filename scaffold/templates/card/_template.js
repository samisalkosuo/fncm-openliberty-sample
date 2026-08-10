// cards/_template.js — copy and rename for each new card
//
// Naming convention: use the feature slug as prefix, e.g. "my-feature"
//   card wrapper id : card-my-feature
//   button id       : my-feature-btn
//   spinner id      : my-feature-spinner
//   result id       : my-feature-result
//
// Steps to add a new card:
//   1. Copy this file → cards/myFeature.js
//   2. Replace every "my-feature" occurrence with your slug
//   3. Add one import line to main.js — that's it. index.html is not touched.
import { apiFetch, API, GraphQL } from '../api.js';
import { esc, renderJson, renderWithToggle } from '../util.js';
import { session } from '../session.js';
import { registerCard } from './registry.js';
// Event bus — uncomment the functions you need:
// import { publish, subscribe, unsubscribe, TOPICS } from '../eventBus.js';
//
// Publishing example (emit an event when something happens):
//   publish(TOPICS.DOCUMENT_SELECTED, { id, name, className });
//
// Subscribing example (react when another card emits an event):
//   subscribe(TOPICS.DOCUMENT_SELECTED, ({ id, name }) => { ... });
//
// Add new topic names to TOPICS in eventBus.js before using them here.

registerCard({
  id: 'my-feature',
  size: 'normal',   // 'normal' | 'wide' | 'tall' | 'large' | 'full'
  html: () => `
    <div class="card" id="card-my-feature">
      <h2>My Feature</h2>
      <button id="my-feature-btn">Load</button>
      <div id="my-feature-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Loading…
      </div>
      <div id="my-feature-result" class="card-result"></div>
    </div>`,
  init() {
    // To receive events from other cards, subscribe here:
    // subscribe(TOPICS.DOCUMENT_SELECTED, (payload) => { ... });

    document.getElementById('my-feature-btn').addEventListener('click', async () => {
      const spinner   = document.getElementById('my-feature-spinner');
      const container = document.getElementById('my-feature-result');
      spinner.classList.remove('hidden');
      container.innerHTML = '';

      try {
        // 1a. call a REST endpoint
        const res = await apiFetch(API.myEndpoint);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        // 2. render result as expandable JSON tree
        const data = await res.json();
        renderJson(container, data);
        // — or, if you have a custom table/visualization —
        // renderWithToggle(container, data, (el, d) => {
        //   el.innerHTML = `<p>${esc(d.someField)}</p>`;
        // });

        // 1b. call GraphQL — returns parsed JSON directly, no .json() needed
        // Use session.config for server-side values in GraphQL variables:
        //   session.config.repositoryIdentifier  — FileNet object store name
        //   session.config.domain                — FileNet domain name
        //   session.config.stanza                — FileNet stanza name
        //
        // Example:
        // const data = await GraphQL.execute(
        //   `query($repo: String!) { folder(repositoryIdentifier: $repo, identifier: "/") { name } }`,
        //   { repo: session.config.repositoryIdentifier }
        // );
        // renderJson(container, data);
        //
        // const data = await GraphQL.execute(`{ domain { name } }`);
        // const data = await GraphQL.execute(`query($id: ID!) { ... }`, { id: '123' });
        // renderJson(container, data);
      } catch (err) {
        container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
      } finally {
        // 3. hide spinner
        spinner.classList.add('hidden');
      }
    });
  },
});

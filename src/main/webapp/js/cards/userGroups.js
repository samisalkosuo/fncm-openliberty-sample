// cards/userGroups.js — User Groups card
import { apiFetch, API } from '../api.js';
import { renderJson, runCardAction } from '../util.js';
import { registerCard } from './registry.js';

registerCard({
  id: 'user-groups',
  size: 'normal',
  html: () => `
    <div class="card" id="card-user-groups">
      <h2>User Groups</h2>
      <button id="user-groups-btn">Load</button>
      <div id="user-groups-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Loading…
      </div>
      <div id="user-groups-result" class="card-result"></div>
    </div>`,
  init() {
    document.getElementById('user-groups-btn').addEventListener('click', () =>
      runCardAction('user-groups-spinner', 'user-groups-result', async container => {
        const res = await apiFetch(API.userGroups);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        renderJson(container, await res.json());
      })
    );
  },
});

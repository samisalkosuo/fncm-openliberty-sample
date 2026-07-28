// cards/documentDetails.js — Document Details card
//
// Subscribes to TOPICS.DOCUMENT_SELECTED and displays the selected document's
// details. No button or API call needed — updates automatically when another
// card publishes the topic.
import { esc } from '../util.js';
import { subscribe, TOPICS } from '../eventBus.js';
import { registerCard } from './registry.js';

registerCard({
  id: 'folder-details',
  size: 'normal',
  html: () => `
    <div class="card" id="card-folder-details">
      <h2>Folder Details</h2>
      <div id="folder-details-result" class="card-result">
        <p class="text-muted">Click a folder name in the folder listing to see its details here.</p>
      </div>
    </div>`,
  init() {
    const container = document.getElementById('folder-details-result');

    subscribe(TOPICS.FOLDER_SELECTED, (folder) => {
      container.innerHTML = `
        <table>
          <tbody>
            <tr><th>Path</th>        <td>${esc(folder.path)}</td></tr>
            <tr><th>ID</th>        <td>${esc(folder.id)}</td></tr>
            <tr><th>Created</th>      <td>${esc(folder.created)}</td></tr>
            <tr><th>Creator</th>     <td>${esc(folder.creator)}</td></tr>
          </tbody>
        </table>`;
    });
  },
});

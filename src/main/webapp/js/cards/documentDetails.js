// cards/documentDetails.js — Document Details card
//
// Subscribes to TOPICS.DOCUMENT_SELECTED and displays the selected document's
// details. No button or API call needed — updates automatically when another
// card publishes the topic.
import { esc } from '../util.js';
import { subscribe, TOPICS } from '../eventBus.js';
import { registerCard } from './registry.js';

registerCard({
  id: 'document-details',
  size: 'normal',
  html: () => `
    <div class="card" id="card-document-details">
      <h2>Document Details</h2>
      <div id="document-details-result" class="card-result">
        <p class="text-muted">Click a folder name in the folder listing to see its details here.</p>
      </div>
    </div>`,
  init() {
    const container = document.getElementById('document-details-result');

    subscribe(TOPICS.DOCUMENT_SELECTED, (doc) => {

      container.innerHTML = `
        <table>
          <tbody>
            <tr><th>Name</th>        <td>${esc(doc.name)}</td></tr>
            <tr><th>Class name</th>        <td>${esc(doc.className)}</td></tr>
            <tr><th>Created</th>      <td>${esc(doc.dateCreated)}</td></tr>
            <tr><th>ID</th>     <td>${esc(doc.id)}</td></tr>
          </tbody>
        </table>`;
    });
  },
});

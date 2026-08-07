// cards/folderTree.js — interactive folder/document tree browser
import { GraphQL } from '../api.js';
import { esc } from '../util.js';
import { session } from '../session.js';
import { registerCard } from './registry.js';
import { publish, TOPICS } from '../eventBus.js';

registerCard({
  id: 'folder-tree',
  size: 'tall',
  html: () => `
    <div class="card" id="card-folder-tree">
      <h2>Folder Tree</h2>
      <button id="folder-tree-btn">Load folders</button>
      <div id="folder-tree-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Loading…
      </div>
      <div id="folder-tree-result" class="card-result"></div>
    </div>`,

  init() {
    document.getElementById('folder-tree-btn').addEventListener('click', async () => {
      const spinner   = document.getElementById('folder-tree-spinner');
      const container = document.getElementById('folder-tree-result');
      spinner.classList.remove('hidden');
      container.innerHTML = '';

      try {
        const data   = await this.getFolders();
        const folders = data?.data?.folders?.folders ?? [];
        const root   = this.buildTree(folders);
        this.renderTree(container, root);
      } catch (err) {
        container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
      } finally {
        spinner.classList.add('hidden');
      }
    });
  },

  /**
   * Convert the flat folder array returned by GraphQL into a tree.
   * Returns the root node (name === "" / parent === null).
   */
  buildTree(folders) {
    // Build a map of id → node
    const nodeMap = new Map();
    for (const f of folders) {
      nodeMap.set(f.id, {
        id:         f.id,
        name:       f.name,
        subFolders: [],
        documents:  [],
      });
    }

    // Populate children using subFolders (preserves server ordering)
    for (const f of folders) {
      const node = nodeMap.get(f.id);
      for (const sf of f.subFolders?.folders ?? []) {
        const child = nodeMap.get(sf.id);
        if (child) node.subFolders.push(child);
      }
      for (const doc of f.containees?.referentialContainmentRelationships ?? []) {
        node.documents.push({ id: doc.head?.id ?? doc.id, name: doc.name });
      }
    }

    // Return the root: the folder with no parent (name is "")
    return folders.find(f => !f.parent) && nodeMap.get(folders.find(f => !f.parent).id);
  },

  /**
   * Render the tree into `container`, starting from root's children
   * (the root folder itself — name "" — is not shown).
   */
  renderTree(container, root) {
    if (!root) {
      container.innerHTML = '<p class="text-muted">No folders found.</p>';
      return;
    }
    const ul = this._buildList(root.subFolders, true);
    ul.classList.add('folder-tree');
    container.appendChild(ul);
  },

  /** Recursively build a <ul> for an array of folder nodes. */
  _buildList(nodes, topLevel) {
    const ul = document.createElement('ul');
    ul.classList.add('ft-list');
    if (!topLevel) ul.classList.add('ft-collapsed');

    for (const node of nodes) {
      ul.appendChild(this._buildFolderItem(node, topLevel));
    }
    return ul;
  },

  /** Build a <li> for a single folder node. */
  _buildFolderItem(node, startOpen) {
    const li = document.createElement('li');
    li.classList.add('ft-item');

    const hasChildren = node.subFolders.length > 0 || node.documents.length > 0;
    const icon = document.createElement('span');
    icon.className   = 'ft-icon';
    icon.textContent = (hasChildren && startOpen) ? '📂' : '📁';

    const label = document.createElement('button');
    label.type      = 'button';
    label.className = 'ft-folder';
    label.appendChild(icon);
    label.appendChild(document.createTextNode(' ' + esc(node.name)));
    li.appendChild(label);

    if (hasChildren) {
      // Build child list (documents + sub-folders)
      const childUl = document.createElement('ul');
      childUl.classList.add('ft-list');
      if (!startOpen) childUl.classList.add('ft-collapsed');

      // Document leaves
      for (const doc of node.documents) {
        const docLi  = document.createElement('li');
        docLi.classList.add('ft-item');
        const docBtn = document.createElement('button');
        docBtn.type      = 'button';
        docBtn.className = 'ft-doc link-btn';
        docBtn.textContent = '📄 ' + esc(doc.name);
        docBtn.addEventListener('click', () => {
          publish(TOPICS.DOCUMENT_ID, doc.id);
          // highlight selected doc
          document.querySelectorAll('.ft-doc.ft-selected')
            .forEach(b => b.classList.remove('ft-selected'));
          docBtn.classList.add('ft-selected');
        });
        docLi.appendChild(docBtn);
        childUl.appendChild(docLi);
      }

      // Sub-folder children (start collapsed)
      for (const child of node.subFolders) {
        childUl.appendChild(this._buildFolderItem(child, false));
      }

      li.appendChild(childUl);

      // Toggle expand/collapse on folder click
      label.addEventListener('click', () => {
        const collapsed = childUl.classList.toggle('ft-collapsed');
        icon.textContent = collapsed ? '📁' : '📂';
        publish(TOPICS.FOLDER_ID, node.id);
      });
    } else {
      // Leaf folder — still publish on click
      label.addEventListener('click', () => {
        publish(TOPICS.FOLDER_ID, node.id);
      });
    }

    return li;
  },

  async getFolders() {
    const graphqlQuery = `
query($repositoryIdentifier: String!) {
  folders(repositoryIdentifier: $repositoryIdentifier)
  {
    folders {
      id
      dateCreated
      name
      owner
      parent {
        id
        name
      }
      subFolders {
        folders {
          id
          name
          dateCreated
        }
      }
      containees {
        referentialContainmentRelationships {
          head {
            id
          }
          id
          name
          dateCreated
        }
      }
    }
  }
}
    `;
    return GraphQL.execute(graphqlQuery, {
      repositoryIdentifier: session.config.repositoryIdentifier,
    });
  },
});

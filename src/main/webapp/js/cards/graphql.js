// cards/graphql.js — GraphQL Query card with sample query library
import { GraphQL } from '../api.js';
import { esc, renderJson } from '../util.js';
import { registerCard } from './registry.js';
import { session } from '../session.js';

// ── Sample query registry ────────────────────────────────────────────────────
// Add new entries here to extend the dropdown.
// Each entry shape:
//   id          – unique string key
//   name        – shown in the dropdown
//   description – shown below the dropdown when this query is selected
//   params      – array of { id, label, sessionKey?, defaultValue? }
//                 sessionKey (optional): dot-path into `session` (e.g. 'config.repositoryIdentifier')
//                 defaultValue (optional): static fallback used when sessionKey is absent or resolves to ''
//                 sessionKey always takes precedence over defaultValue
//   query       – GraphQL query string; use $paramId variables matching the params ids
// ─────────────────────────────────────────────────────────────────────────────
const SAMPLE_QUERIES = [
  {
    id: 'listObjectStores',
    name: 'List Object Stores',
    description: 'Returns the symbolic names of all object stores in the domain. No parameters required.',
    params: [],
    query: `{ domain { objectStores { objectStores { symbolicName } } } }`,
  },
  {
    id: 'listFolders',
    name: 'List Folders',
    description: 'Lists the top-level folders (sub-folders of the root) in the given object store.',
    params: [
      { id: 'repositoryIdentifier', label: 'Repository Identifier', sessionKey: 'config.repositoryIdentifier' },
    ],
    query: `query ListFolders($repositoryIdentifier: String!) {
  folder(
    repositoryIdentifier: $repositoryIdentifier
    identifier: "/"
  ) {
    subFolders {
      folders{
        pathName
      id
      name
      
      }
    }  }
}`,
  },
  {
    id: 'listDocumentsInFolder',
    name: 'List Documents in Folder',
    description: 'Returns documents contained directly in the specified folder path.',
    params: [
      { id: 'repositoryIdentifier', label: 'Repository Identifier', sessionKey: 'config.repositoryIdentifier' },
      { id: 'folderPath', label: 'Folder Path (e.g. /MyFolder)', defaultValue: '/' },
    ],
    query: `query ListDocumentsInFolder($repositoryIdentifier: String!, $folderPath: String!) {
  folder(
    repositoryIdentifier: $repositoryIdentifier
    identifier: $folderPath
  ) {
 id
    name
    pathName
    containedDocuments {
      documents {
        id
        name
      }
    }
	}  
}`,
  },
  {
    id: 'getDocumentDetails',
    name: 'Get Document Details',
    description: 'Retrieves metadata for a single document by its ID.',
    params: [
      { id: 'repositoryIdentifier', label: 'Repository Identifier', sessionKey: 'config.repositoryIdentifier' },
      { id: 'documentId', label: 'Document ID', defaultValue: '' },
    ],
    query: `query GetDocumentDetails($repositoryIdentifier: String!, $documentId: String!) {
  document(
    repositoryIdentifier: $repositoryIdentifier
    identifier: $documentId
  ) {
    
    id
    name
    dateCreated
    dateLastModified
        contentElements {
      
      ... on ContentTransfer {
        retrievalName
        contentType
        contentSize
        downloadUrl
      }
    }
    creator
    className
  }
}
`,
  },
  {
    id: 'getUserGroups',
    name: 'Get User Groups',
    description: 'Returns all groups in the domain that the current user belongs to.',
    params: [],
    query: `query GetGroups {
  secGroups(
    realmIdentifier: null
    searchPattern: ""
    searchType: NONE
    searchAttribute: SHORT_NAME
    sortType: ASCENDING
  ) {
    groups {
      id
      shortName
      displayName
      distinguishedName
    }
  }
}
`,
  },
  {
    id: 'searchDocumentsByClass',
    name: 'Search Documents by Class',
    description: 'Searches for documents of a specific document class in the object store.',
    params: [
      { id: 'repositoryIdentifier', label: 'Repository Identifier', sessionKey: 'config.repositoryIdentifier' },
      { id: 'className', label: 'Document Class Name (e.g. Document)', defaultValue: 'Document' },
    ],
    query: `query SearchDocumentsByClass($repositoryIdentifier: String!, $className: String!) {
  documents(
    repositoryIdentifier: $repositoryIdentifier
    from:  $className 
    where:"[DateCreated] > 20180815T070000Z AND [IsCurrentVersion] = True"
  ) {
    documents {
      id
      name
      dateCreated
      className
    }
  }
}`,
  },
  {
    id: 'getFolderDetails',
    name: 'Get Folder Details',
    description: 'Retrieves metadata for a specific folder, including its sub-folders and document count.',
    params: [
      { id: 'repositoryIdentifier', label: 'Repository Identifier', sessionKey: 'config.repositoryIdentifier' },
      { id: 'folderPath', label: 'Folder Path (e.g. /MyFolder)', defaultValue: '/' },
    ],
    query: `query GetFolderDetails($repositoryIdentifier: String!, $folderPath: String!) {
  folder(
    repositoryIdentifier: $repositoryIdentifier
    identifier: $folderPath
  ) {
    className
    id
    name
    pathName
    subFolders {
      folders {
        name
      }
    }
    containedDocuments {
      documents {
        id
        name
        className
        dateCreated
      }
    }  }
}`,
  },
  {
    id: 'createGenericDocument',
    name: 'Create Generic Document',
    description: 'Creates document with name and class \'Document\'.',
    params: [
      { id: 'repositoryIdentifier', label: 'Repository Identifier', sessionKey: 'config.repositoryIdentifier' },
      { id: 'className', label: 'Document Class Name (e.g. Document)', defaultValue: 'Document' },
    ],    
    query: `mutation CreateGenericDocument($repositoryIdentifier: String!, $className: String!){
    createDocument(
        repositoryIdentifier: $repositoryIdentifier
        classIdentifier: $className
        fileInFolderIdentifier: "/" 
        documentProperties: {
            name: "Generic Document Name" 
        }
        checkinAction: {}
    ) 
    { 
        id 
        name 
    } 
}`
  },
  {
    id: 'describeClass',
    name: 'Describe Class and Properties',
    description: 'Retrieve class description and property descriptions.',
    params: [
      { id: 'repositoryIdentifier', label: 'Repository Identifier', sessionKey: 'config.repositoryIdentifier' },
      { id: 'className', label: 'Document Class Name (e.g. Document)', defaultValue: 'Document' },
    ],
    query: `query DescribeClass($repositoryIdentifier: String!, $className: String!) {
  classDescription(
    	repositoryIdentifier: $repositoryIdentifier
    	identifier: $className) 
  {
    name
    displayName
    symbolicName
    propertyDescriptions (
      filter: {
        isHidden: false 
      }) 
    {
      ...propDescFieldsFragment
    }
    hasProperSubclassProperties
    properSubclassPropertyDescriptions(
      filter: {
        isHidden: false
      }) 
    {
      ...propDescFieldsFragment
    }
  }
}

fragment propDescFieldsFragment on PropertyDescription {
  id
  symbolicName
  displayName
  descriptiveText
  dataType
  cardinality
  isReadOnly
  isValueRequired
  settability
  ... on PropertyDescriptionBoolean {
    propertyDefaultBoolean
  }
  ... on PropertyDescriptionString {
    propertyDefaultString
    maximumLengthString
  }
  ... on PropertyDescriptionInteger32 {
    propertyDefaultInteger32
    propertyMinimumInteger32
    propertyMaximumInteger32
  }
  ... on PropertyDescriptionId {
    propertyDefaultId
  }
  ... on PropertyDescriptionDateTime {
    propertyDefaultDateTime
    propertyMinimumDateTime
    propertyMaximumDateTime
  }
  ... on PropertyDescriptionFloat64 {
    propertyDefaultFloat64
    propertyMinimumFloat64
    propertyMaximumFloat64
  }
}`
  }
];

// ── Resolve a sessionKey like 'config.repositoryIdentifier' against session ──
function resolveSessionValue(sessionKey) {
  return sessionKey.split('.').reduce((obj, key) => obj?.[key], session) ?? '';
}

registerCard({
  id: 'graphql',
  size: 'large',
  html: () => `
    <div class="card" id="card-graphql">
      <h2>GraphQL Query</h2>
      <div class="form-group">
        <label for="graphql-sample-select">Sample Queries</label>
        <select id="graphql-sample-select">
          <option value="">— select a sample query —</option>
          ${SAMPLE_QUERIES.map(q => `<option value="${esc(q.id)}">${esc(q.name)}</option>`).join('\n          ')}
        </select>
      </div>
      <div id="graphql-query-desc" class="hidden" style="margin-bottom:var(--space-lg);padding:.6rem .75rem;border-left:3px solid var(--color-border);color:var(--color-text-muted);font-size:0.88rem;line-height:1.5;"></div>
      <div id="graphql-params"></div>
      <div class="form-group">
        <label for="graphql-query">Query (editable)</label>
        <textarea id="graphql-query" class="code-input" rows="8">{ domain { objectStores { objectStores { symbolicName } } } }</textarea>
      </div>
      <button id="graphql-run-btn">Run</button>
      <div id="graphql-spinner" class="hidden spinner-row">
        <span class="spinner"></span> Running…
      </div>
      <div id="graphql-result" class="card-result"></div>
    </div>`,

  init() {
    const select      = document.getElementById('graphql-sample-select');
    const descBox     = document.getElementById('graphql-query-desc');
    const paramsBox   = document.getElementById('graphql-params');
    const textarea    = document.getElementById('graphql-query');

    // Renders dynamic param inputs for the selected query entry.
    function renderParams(params) {
      paramsBox.innerHTML = '';
      if (!params.length) return;

      params.forEach(param => {
        const sessionValue = param.sessionKey ? resolveSessionValue(param.sessionKey) : '';
        const value = sessionValue || (param.defaultValue ?? '');
        const group = document.createElement('div');
        group.className = 'form-group';
        group.innerHTML = `
          <label for="graphql-param-${esc(param.id)}">${esc(param.label)}</label>
          <input type="text" id="graphql-param-${esc(param.id)}" value="${esc(value)}" placeholder="${esc(param.label)}">`;
        paramsBox.appendChild(group);
      });
    }

    // Populate UI when a sample query is selected.
    select.addEventListener('change', () => {
      const entry = SAMPLE_QUERIES.find(q => q.id === select.value);
      if (!entry) {
        descBox.classList.add('hidden');
        descBox.textContent = '';
        paramsBox.innerHTML = '';
        return;
      }
      descBox.textContent = entry.description;
      descBox.classList.remove('hidden');
      renderParams(entry.params);
      textarea.value = entry.query;
    });

    // Run button — collect variables from rendered param inputs and execute.
    document.getElementById('graphql-run-btn').addEventListener('click', async () => {
      const query     = textarea.value.trim();
      const spinner   = document.getElementById('graphql-spinner');
      const container = document.getElementById('graphql-result');

      // Build variables object from any visible param inputs.
      const variables = {};
      paramsBox.querySelectorAll('input[type=text]').forEach(input => {
        const paramId = input.id.replace('graphql-param-', '');
        if (paramId) variables[paramId] = input.value;
      });

      spinner.classList.remove('hidden');
      container.innerHTML = '';

      try {
        const data = await GraphQL.execute(query, variables);
        renderJson(container, data);
      } catch (err) {
        container.innerHTML = `<div class="alert alert-error">${esc(err.message)}</div>`;
      } finally {
        spinner.classList.add('hidden');
      }
    });
  },
});

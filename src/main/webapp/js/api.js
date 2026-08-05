// api.js — all fetch calls go through here; auth header attached once, everywhere.
import { session } from './session.js';

export const API = {
  login:                '/api/auth/login',
  graphql:              '/api/graphql',
  connectionTest:       '/api/connectiontest',
  listFolders:          '/api/listfolders',
  listDocumentClasses:  '/api/listdocumentclasses',
  userGroups:           '/api/getusergroups',
  documents:            '/api/documents',
  listDocumentsInFolder: '/api/listdocumentsinfolder',
  addBuildingInspectionDocs: '/api/buildinginspectiondocs',
  fileBuildingInspectionDocs: '/api/filebuildinginspectiondocs',
  deleteAllFolders: '/api/deleteallfolders',
  createBuildingInspectionReportDocument:   '/api/createbuildinginspectionreportdocument',
  checkinDocument:                          '/api/checkindocument',
  downloadDocument:                         '/api/downloaddocument',
};

export async function apiFetch(url, options = {}) {
  const headers = {
    'Authorization': 'Bearer ' + session.appToken,
    ...options.headers,
  };
  return fetch(url, { ...options, headers });
}

// GraphQL — single entry point for all GraphQL calls.
// Usage: const data = await GraphQL.execute(query);
//        const data = await GraphQL.execute(query, { varName: value });
export const GraphQL = {
  async execute(query, variables = {}) {
    const res = await apiFetch(API.graphql, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query, variables }),
    });
    if (!res.ok) {
      const err = new Error(res.status === 401 ? 'Session expired. Please sign in again.' : `HTTP ${res.status}`);
      err.status = res.status;
      throw err;
    }
    return res.json();
  },
};

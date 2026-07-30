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
};

export async function apiFetch(url, options = {}) {
  const headers = {
    'Authorization': 'Bearer ' + session.appToken,
    ...options.headers,
  };
  return fetch(url, { ...options, headers });
}

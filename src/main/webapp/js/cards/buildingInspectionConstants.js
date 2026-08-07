// buildingInspectionConstants.js — shared domain constants for building inspection documents.
//
// Imported by documentDetails.js and createBuildingInspectionReportDocument.js
// so both files always use the same option values without manual synchronisation.

export const BUILDING_TYPE_OPTIONS     = ['Unknown', 'Residential', 'Commercial', 'Industrial', 'Public'];
export const COMPLIANCE_STATUS_OPTIONS = ['Unknown', 'Fully Compliant', 'Mostly Compliant', 'Partially Compliant', 'Non-Compliant', 'Requires Follow-up'];

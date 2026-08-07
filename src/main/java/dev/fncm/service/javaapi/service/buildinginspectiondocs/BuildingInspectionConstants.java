package dev.fncm.service.javaapi.service.buildinginspectiondocs;

/**
 * Shared string constants for the building inspection domain.
 *
 * <p>All property names, document class name, root folder name, and choice list
 * display names used across the building inspection module are defined here.
 * Using these constants instead of bare string literals prevents typos and makes
 * renaming trivial.
 */
public final class BuildingInspectionConstants {

    private BuildingInspectionConstants() {
        // utility class — not instantiable
    }

    // -------------------------------------------------------------------------
    // Document class
    // -------------------------------------------------------------------------

    /** Symbolic name of the building inspection document class. */
    public static final String DOC_CLASS = "BuildingInspectionReport";

    // -------------------------------------------------------------------------
    // Root folder
    // -------------------------------------------------------------------------

    /** Name of the root folder that holds all building inspection reports. */
    public static final String ROOT_FOLDER_NAME = "BuildingInspectionReports";

    // -------------------------------------------------------------------------
    // Property names
    // -------------------------------------------------------------------------

    public static final String PROP_MUNICIPALITY       = "Municipality";
    public static final String PROP_PROPERTY_ADDRESS   = "PropertyAddress";
    public static final String PROP_INSPECTOR_NAME     = "InspectorName";
    public static final String PROP_BUILDING_TYPE      = "BuildingType";
    public static final String PROP_COMPLIANCE_STATUS  = "ComplianceStatus";
    public static final String PROP_INSPECTION_DATE    = "InspectionDate";

    // -------------------------------------------------------------------------
    // Choice list display names
    // -------------------------------------------------------------------------

    public static final String CHOICE_LIST_BUILDING_TYPE    = "Building Type Choices";
    public static final String CHOICE_LIST_COMPLIANCE_STATUS = "Compliance Status Choices";
}

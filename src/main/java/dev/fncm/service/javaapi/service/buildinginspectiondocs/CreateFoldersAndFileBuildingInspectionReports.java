package dev.fncm.service.javaapi.service.buildinginspectiondocs;

import java.util.Calendar;

import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.Folder;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ReferentialContainmentRelationship;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.collection.FolderSet;
import com.filenet.api.collection.DocumentSet;
import com.filenet.api.property.Properties;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CreateFoldersAndFileBuildingInspectionReports {

    private static final Logger LOGGER = Logger
            .getLogger(CreateFoldersAndFileBuildingInspectionReports.class.getName());

    private static final String ROOT_FOLDER_NAME = BuildingInspectionConstants.ROOT_FOLDER_NAME;
    private static final String DOCUMENT_CLASS = BuildingInspectionConstants.DOC_CLASS;

    private static final String[] BUILDING_TYPES = {
            "Industrial",
            "Residential",
            "Public",
            "Commercial",
            "Unknown"
    };

    public void execute(ObjectStore objectStore) throws Exception {
        LOGGER.info("=================================================");
        LOGGER.info("Starting Folder Organization");
        LOGGER.info("=================================================");
        LOGGER.info("Object Store: " + objectStore.get_DisplayName());

        Map<String, Folder> typeFolders = createFolderStructure(objectStore);

        DocumentSet documents = queryDocuments(objectStore);
        int totalDocs = countDocuments(documents);
        LOGGER.info("Found " + totalDocs + " documents to organize");

        LOGGER.info("Filing documents into folders...");
        int[] counts = fileAllDocuments(objectStore, documents, typeFolders, totalDocs);
        int processedCount = counts[0];
        int errorCount = counts[1];
        int dateFolderCount = counts[2];

        logOrganizationSummary(totalDocs, processedCount, errorCount, dateFolderCount, typeFolders.size());
    }

    /**
     * Create the full folder structure (root, ByDate, BuildingTypes, per-type sub-folders).
     * Returns the map of building-type name → type folder.
     */
    private Map<String, Folder> createFolderStructure(ObjectStore objectStore) {
        Folder rootFolder = createOrGetFolder(objectStore, null, ROOT_FOLDER_NAME);
        LOGGER.info("✓ Root folder ready: /" + ROOT_FOLDER_NAME);

        createOrGetFolder(objectStore, rootFolder, "ByDate");
        LOGGER.info("✓ ByDate folder ready: /" + ROOT_FOLDER_NAME + "/ByDate");

        Folder buildingTypesFolder = createOrGetFolder(objectStore, rootFolder, "BuildingTypes");
        LOGGER.info("✓ BuildingTypes folder ready: /" + ROOT_FOLDER_NAME + "/BuildingTypes");

        LOGGER.info("Creating building type folders...");
        Map<String, Folder> typeFolders = new HashMap<>();
        for (String buildingType : BUILDING_TYPES) {
            Folder typeFolder = createOrGetFolder(objectStore, buildingTypesFolder, buildingType);
            typeFolders.put(buildingType, typeFolder);
            LOGGER.info("  ✓ " + buildingType);
        }
        return typeFolders;
    }

    /**
     * Iterate over every document, filing each one by date and building type.
     * Returns int[]{processedCount, errorCount, dateFolderCount}.
     */
    private int[] fileAllDocuments(ObjectStore objectStore, DocumentSet documents,
            Map<String, Folder> typeFolders, int totalDocs) {

        Folder byDateFolder = createOrGetFolder(objectStore,
                createOrGetFolder(objectStore, null, ROOT_FOLDER_NAME), "ByDate");

        Map<String, Folder> dateFolders = new HashMap<>();
        int processedCount = 0;
        int errorCount = 0;

        Iterator<?> iterator = documents.iterator();
        while (iterator.hasNext()) {
            Document doc = (Document) iterator.next();
            processedCount++;
            LOGGER.info("-------------------------------------------");
            LOGGER.info("Processing document " + processedCount + " of " + totalDocs);
            LOGGER.info("Document ID: " + doc.get_Id());

            try {
                fileDocument(objectStore, doc, byDateFolder, dateFolders, typeFolders);
            } catch (Exception e) {
                errorCount++;
                LOGGER.log(Level.SEVERE, "  ✗ Error processing document: " + e.getMessage(), e);
            }
        }

        return new int[]{ processedCount, errorCount, dateFolders.size() };
    }

    /**
     * File a single document into its date folder and building-type folder.
     */
    private void fileDocument(ObjectStore objectStore, Document doc,
            Folder byDateFolder, Map<String, Folder> dateFolders,
            Map<String, Folder> typeFolders) {

        Properties props = doc.getProperties();

        Date inspectionDate = (Date) props.getObjectValue(BuildingInspectionConstants.PROP_INSPECTION_DATE);
        String buildingType = props.getStringValue(BuildingInspectionConstants.PROP_BUILDING_TYPE);

        LOGGER.info("  InspectionDate: " + (inspectionDate != null ? inspectionDate : "N/A"));
        LOGGER.info("  BuildingType: " + (buildingType != null ? buildingType : "N/A"));

        if (inspectionDate != null) {
            String datePath = getDateFolderPath(inspectionDate);
            Folder dateFolder = dateFolders.computeIfAbsent(datePath,
                    k -> createDateFolderHierarchy(objectStore, byDateFolder, inspectionDate));
            fileDocumentIntoFolder(doc, dateFolder);
            LOGGER.info("  ✓ Filed in: /" + ROOT_FOLDER_NAME + "/ByDate/" + datePath);
        }

        if (buildingType != null && typeFolders.containsKey(buildingType)) {
            fileDocumentIntoFolder(doc, typeFolders.get(buildingType));
            LOGGER.info("  ✓ Filed in: /" + ROOT_FOLDER_NAME + "/BuildingTypes/" + buildingType);
        } else if (buildingType != null) {
            LOGGER.info("  ⚠ Unknown building type: " + buildingType);
        }
    }

    /**
     * Log the final organization summary.
     */
    private void logOrganizationSummary(int totalDocs, int processedCount,
            int errorCount, int dateFolderCount, int typeFolderCount) {
        LOGGER.info("=================================================");
        LOGGER.info("Organization Summary");
        LOGGER.info("=================================================");
        LOGGER.info("Total documents: " + totalDocs);
        LOGGER.info("Successfully processed: " + (processedCount - errorCount));
        LOGGER.info("Errors: " + errorCount);
        LOGGER.info("Date folders created: " + dateFolderCount);
        LOGGER.info("Type folders created: " + typeFolderCount);
        LOGGER.info("=================================================");
    }

    /**
     * Query all documents of type BuildingInspectionReport.
     * Fetches Id, InspectionDate, BuildingType, and FoldersFiledIn properties.
     */
    private DocumentSet queryDocuments(ObjectStore objectStore) {
        String sql = "SELECT Id, InspectionDate, BuildingType, FoldersFiledIn FROM " + DOCUMENT_CLASS;
        SearchSQL searchSQL = new SearchSQL(sql);
        SearchScope scope = new SearchScope(objectStore);
        return (DocumentSet) scope.fetchObjects(searchSQL, null, null, Boolean.TRUE);
    }

    /**
     * Count documents in a DocumentSet by iterating once.
     */
    private int countDocuments(DocumentSet documents) {
        LOGGER.info("Querying BuildingInspectionReport documents...");
        int count = 0;
        Iterator<?> it = documents.iterator();
        while (it.hasNext()) {
            it.next();
            count++;
        }
        return count;
    }

    /**
     * Create or get an existing folder.
     */
    private Folder createOrGetFolder(ObjectStore objectStore, Folder parentFolder, String folderName) {
        try {
            String folderPath = parentFolder == null ? "/" + folderName
                    : parentFolder.get_PathName() + "/" + folderName;
            return Factory.Folder.fetchInstance(objectStore, folderPath, null);
        } catch (EngineRuntimeException e) {
            Folder newFolder = Factory.Folder.createInstance(objectStore, null);
            newFolder.set_FolderName(folderName);

            if (parentFolder != null) {
                newFolder.set_Parent(parentFolder);
            } else {
                Folder rootFolder = objectStore.get_RootFolder();
                newFolder.set_Parent(rootFolder);
            }

            newFolder.save(RefreshMode.REFRESH);
            return newFolder;
        }
    }

    /**
     * Create date folder hierarchy (YYYY/MM).
     */
    private Folder createDateFolderHierarchy(ObjectStore objectStore, Folder rootFolder, Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1; // Calendar.MONTH is 0-based

        String yearStr = String.valueOf(year);
        String monthStr = String.format("%02d", month);

        Folder yearFolder = createOrGetFolder(objectStore, rootFolder, yearStr);
        return createOrGetFolder(objectStore, yearFolder, monthStr);
    }

    /**
     * Get the date folder path string (YYYY/MM).
     */
    private String getDateFolderPath(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;

        return String.format("%d/%02d", year, month);
    }

    /**
     * File a document into a folder (create ReferentialContainmentRelationship),
     * skipping if the document is already filed there.
     */
    private void fileDocumentIntoFolder(Document doc, Folder folder) {
        FolderSet folders = doc.get_FoldersFiledIn();
        Iterator<?> it = folders.iterator();
        while (it.hasNext()) {
            Folder existingFolder = (Folder) it.next();
            if (existingFolder.get_Id().equals(folder.get_Id())) {
                return;
            }
        }

        ReferentialContainmentRelationship rcr = Factory.ReferentialContainmentRelationship.createInstance(
                doc.getObjectStore(), null);
        rcr.set_Head(doc);
        rcr.set_Tail(folder);
        rcr.save(RefreshMode.REFRESH);
    }

}

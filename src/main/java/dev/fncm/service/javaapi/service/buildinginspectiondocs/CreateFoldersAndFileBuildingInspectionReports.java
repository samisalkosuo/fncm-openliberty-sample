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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;;

public class CreateFoldersAndFileBuildingInspectionReports {

    private static final Logger LOGGER = Logger
            .getLogger(CreateFoldersAndFileBuildingInspectionReports.class.getName());

    private static final String ROOT_FOLDER_NAME = "BuildingInspectionReports";
    private static final String DOCUMENT_CLASS = "BuildingInspectionReport";

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

        // Step 1: Create root folder
        Folder rootFolder = createOrGetFolder(objectStore, null, ROOT_FOLDER_NAME);
        LOGGER.info("✓ Root folder ready: /" + ROOT_FOLDER_NAME);

        // Step 2: Create ByDate parent folder
        Folder byDateFolder = createOrGetFolder(objectStore, rootFolder, "ByDate");
        LOGGER.info("✓ ByDate folder ready: /" + ROOT_FOLDER_NAME + "/ByDate");

        // Step 3: Create BuildingTypes parent folder
        Folder buildingTypesFolder = createOrGetFolder(objectStore, rootFolder, "BuildingTypes");
        LOGGER.info("✓ BuildingTypes folder ready: /" + ROOT_FOLDER_NAME + "/BuildingTypes");

        // Step 4: Create building type folders under BuildingTypes
        LOGGER.info("Creating building type folders...");
        Map<String, Folder> typeFolders = new HashMap<>();
        for (String buildingType : BUILDING_TYPES) {
            Folder typeFolder = createOrGetFolder(objectStore, buildingTypesFolder, buildingType);
            typeFolders.put(buildingType, typeFolder);
            LOGGER.info("  ✓ " + buildingType);
        }

        // Step 5: Query all BuildingInspectionReport documents
        LOGGER.info("Querying BuildingInspectionReport documents...");
        DocumentSet documents = queryDocuments(objectStore);

        int totalDocs = 0;
        Iterator<?> iterator = documents.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            totalDocs++;
        }
        LOGGER.info("Found " + totalDocs + " documents to organize");

        // Step 6: Process each document
        LOGGER.info("Filing documents into folders...");
        Map<String, Folder> dateFolders = new HashMap<>();
        int processedCount = 0;
        int errorCount = 0;

        iterator = documents.iterator();
        while (iterator.hasNext()) {
            Document doc = (Document) iterator.next();
            processedCount++;

            try {
                LOGGER.info("-------------------------------------------");
                LOGGER.info("Processing document " + processedCount + " of " + totalDocs);
                LOGGER.info("Document ID: " + doc.get_Id());

                Properties props = doc.getProperties();

                // Get InspectionDate
                Date inspectionDate = (Date) props.getObjectValue("InspectionDate");
                String buildingType = (String) props.getStringValue("BuildingType");

                LOGGER.info("  InspectionDate: " + (inspectionDate != null ? inspectionDate : "N/A"));
                LOGGER.info("  BuildingType: " + (buildingType != null ? buildingType : "N/A"));

                // File by date
                if (inspectionDate != null) {
                    String datePath = getDateFolderPath(inspectionDate);
                    Folder dateFolder = dateFolders.get(datePath);

                    if (dateFolder == null) {
                        dateFolder = createDateFolderHierarchy(objectStore, byDateFolder, inspectionDate);
                        dateFolders.put(datePath, dateFolder);
                    }

                    fileDocument(doc, dateFolder);
                    LOGGER.info("  ✓ Filed in: /" + ROOT_FOLDER_NAME + "/ByDate/" + datePath);
                }

                // File by building type
                if (buildingType != null && typeFolders.containsKey(buildingType)) {
                    Folder typeFolder = typeFolders.get(buildingType);
                    fileDocument(doc, typeFolder);
                    LOGGER.info("  ✓ Filed in: /" + ROOT_FOLDER_NAME + "/BuildingTypes/" + buildingType);
                } else if (buildingType != null) {
                    LOGGER.info("  ⚠ Unknown building type: " + buildingType);
                }

            } catch (Exception e) {
                errorCount++;
                LOGGER.log(Level.SEVERE, "  ✗ Error processing document: " + e.getMessage(), e);
            }
        }

        // Summary
        LOGGER.info("=================================================");
        LOGGER.info("Organization Summary");
        LOGGER.info("=================================================");
        LOGGER.info("Total documents: " + totalDocs);
        LOGGER.info("Successfully processed: " + (processedCount - errorCount));
        LOGGER.info("Errors: " + errorCount);
        LOGGER.info("Date folders created: " + dateFolders.size());
        LOGGER.info("Type folders created: " + typeFolders.size());
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
     * Create or get an existing folder.
     */
    private Folder createOrGetFolder(ObjectStore objectStore, Folder parentFolder, String folderName) {
        try {
            // Try to find existing folder
            String folderPath = parentFolder == null ? "/" + folderName
                    : parentFolder.get_PathName() + "/" + folderName;
            Folder folder = Factory.Folder.fetchInstance(objectStore, folderPath, null);
            return folder;
        } catch (EngineRuntimeException e) {
            // Folder doesn't exist, create it
            Folder newFolder = Factory.Folder.createInstance(objectStore, null);
            newFolder.set_FolderName(folderName);

            if (parentFolder != null) {
                // Set parent for subfolder
                newFolder.set_Parent(parentFolder);
            } else {
                // For root-level folder, get the root folder and set it as parent
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

        // Create or get year folder
        Folder yearFolder = createOrGetFolder(objectStore, rootFolder, yearStr);

        // Create or get month folder
        Folder monthFolder = createOrGetFolder(objectStore, yearFolder, monthStr);

        return monthFolder;
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
     * File a document into a folder (create ReferentialContainmentRelationship).
     */
    private void fileDocument(Document doc, Folder folder) {
        // Check if document is already filed in this folder
        FolderSet folders = doc.get_FoldersFiledIn();
        Iterator<?> it = folders.iterator();
        while (it.hasNext()) {
            Folder existingFolder = (Folder) it.next();
            if (existingFolder.get_Id().equals(folder.get_Id())) {
                // Already filed in this folder
                return;
            }
        }

        // File the document
        ReferentialContainmentRelationship rcr = Factory.ReferentialContainmentRelationship.createInstance(
                doc.getObjectStore(), null);
        rcr.set_Head(doc);
        rcr.set_Tail(folder);
        rcr.save(RefreshMode.REFRESH);
    }

}

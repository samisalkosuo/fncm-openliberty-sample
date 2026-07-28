package dev.fncm.service.javaapi.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import com.filenet.api.collection.RepositoryRowSet;
import com.filenet.api.property.Properties;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;

import dev.fncm.service.javaapi.FileNetOperation;

public class ListDocumentClassesOperation implements FileNetOperation<String> {

    private static final Logger LOGGER = Logger.getLogger(ListFoldersOperation.class.getName());

    @Override
    public String execute(ObjectStore os, String username) throws Exception {

        String searchFilter = null;
        String sqlQuery = "SELECT SymbolicName, DisplayName, DescriptiveText " +
                "FROM Document " +
                "WHERE IsHidden = FALSE " +
                "AND IsCurrentVersion = TRUE " +
                "ORDER BY SymbolicName";

        LOGGER.info("Querying for document instances to determine document classes...");

        // First, let's get all class definitions and filter manually
        sqlQuery = "SELECT SymbolicName, DisplayName, DescriptiveText " +
                "FROM ClassDefinition " +
                "WHERE IsHidden = FALSE " +
                "ORDER BY SymbolicName";

        LOGGER.info("Executing query: " + sqlQuery);

        SearchSQL searchSQL = new SearchSQL(sqlQuery);
        SearchScope searchScope = new SearchScope(os);
        RepositoryRowSet rowSet = searchScope.fetchRows(searchSQL, null, null, true);

        // Collect all classes and determine which are document classes
        List<DocumentClassInfo> allClasses = new ArrayList<>();
        Iterator<?> iterator = rowSet.iterator();

        while (iterator.hasNext()) {
            RepositoryRow row = (RepositoryRow) iterator.next();
            Properties props = row.getProperties();

            String symbolicName = props.getStringValue("SymbolicName");

            DocumentClassInfo info = new DocumentClassInfo();
            info.symbolicName = symbolicName;
            info.displayName = props.getStringValue("DisplayName");
            info.description = props.getStringValue("DescriptiveText");

            if (isCustomClass(symbolicName)) {
                info.type = ClassType.CUSTOM;

            } else {
                info.type = ClassType.SYSTEM;
            }

            allClasses.add(info);
        }

        // Now check each class to see if it's a document class by trying to query it as
        // a Document
        List<DocumentClassInfo> documentClasses = new ArrayList<>();
        for (DocumentClassInfo classInfo : allClasses) {
            if (isDocumentClass(classInfo.symbolicName, os)) {
                documentClasses.add(classInfo);
            }
        }
/*
        // Apply search filter if provided
        if (searchFilter != null && !searchFilter.trim().isEmpty()) {
            documentClasses = filterClasses(documentClasses, searchFilter);
        }
*/
        // Sort by symbolic name
        Collections.sort(documentClasses, Comparator.comparing(c -> c.symbolicName));

        // Display results
        LOGGER.info("Document Classes Found: " + documentClasses.size());
  /*
        if (searchFilter != null && !searchFilter.trim().isEmpty()) {
            LOGGER.info("(Filtered by: \"" + searchFilter + "\")");
        }
*/
        String result="No document classes found in the object store.";
        if (documentClasses.isEmpty() == false) {
            // Display all classes
            result = displayClasses(documentClasses);
        }
        return result;

    }

    /**
     * Check if a class is a Document class or subclass of Document
     * by trying to query it from the Document table
     */
    private boolean isDocumentClass(String symbolicName, ObjectStore objectStore) {
        try {
            // Try to query this class as if it were a Document subclass
            // If it works, it's a document class
            String testQuery = "SELECT TOP 1 Id FROM " + symbolicName;
            SearchSQL searchSQL = new SearchSQL(testQuery);
            SearchScope searchScope = new SearchScope(objectStore);

            // Try to execute the query - if it succeeds, it's a valid document class
            searchScope.fetchRows(searchSQL, null, null, false);
            return true;

        } catch (Exception e) {
            // If query fails, it's not a document class (or doesn't exist as a queryable
            // table)
            return false;
        }
    }

    /**
     * Filter classes based on case-insensitive search string.
     * Searches in symbolic name, display name, and description.
     */
    private List<DocumentClassInfo> filterClasses(List<DocumentClassInfo> classes, String searchFilter) {
        List<DocumentClassInfo> filtered = new ArrayList<>();
        String lowerSearchFilter = searchFilter.toLowerCase();

        for (DocumentClassInfo classInfo : classes) {
            boolean matches = false;

            // Check symbolic name
            if (classInfo.symbolicName != null &&
                    classInfo.symbolicName.toLowerCase().contains(lowerSearchFilter)) {
                matches = true;
            }

            // Check display name
            if (!matches && classInfo.displayName != null &&
                    classInfo.displayName.toLowerCase().contains(lowerSearchFilter)) {
                matches = true;
            }

            // Check description
            if (!matches && classInfo.description != null &&
                    classInfo.description.toLowerCase().contains(lowerSearchFilter)) {
                matches = true;
            }

            if (matches) {
                filtered.add(classInfo);
            }
        }

        return filtered;
    }

    /**
     * Display all classes
     */
    private String displayClasses(List<DocumentClassInfo> classes) {
        int count = 0;
        StringBuilder sb = new StringBuilder();
        for (DocumentClassInfo classInfo : classes) {
            count++;

            sb.append("[" + count + "] " + classInfo.symbolicName);
            sb.append("\n");

            sb.append("    Display Name: " + classInfo.displayName);
            sb.append("\n");

            if (classInfo.description != null && !classInfo.description.trim().isEmpty()) {
                sb.append("    Description: " + classInfo.description);
                sb.append("\n");

            }

            sb.append("    Type: " + classInfo.type);
            sb.append("\n");

  /*          // Indicate if it's a custom class
            if (isCustomClass(classInfo.symbolicName)) {
                sb.append("    Type: *** CUSTOM CLASS ***");
                sb.append("\n");

            } else {
                sb.append("    Type: System Class");
                sb.append("\n");

            }
*/
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Check if a class is a custom class (not a system class)
     */
    private boolean isCustomClass(String symbolicName) {
        // System classes typically start with standard prefixes
        // Custom classes are user-defined
        String[] systemPrefixes = {
                "Document", "Folder", "CustomObject", "Annotation",
                "Email", "WorkflowDefinition", "Queue", "Roster"
        };

        // If it exactly matches a system class, it's not custom
        for (String prefix : systemPrefixes) {
            if (symbolicName.equals(prefix)) {
                return false;
            }
        }

        // If it starts with a system prefix but has more, it might be custom
        // Check for common custom naming patterns (e.g., contains underscore, mixed
        // case after prefix)
        return symbolicName.contains("_") ||
                symbolicName.matches(".*[A-Z][a-z]+[A-Z].*");
    }

    /**
     * Count system classes
     */
    private int countSystemClasses(List<DocumentClassInfo> classes) {
        int count = 0;
        for (DocumentClassInfo classInfo : classes) {
            if (!isCustomClass(classInfo.symbolicName)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Count custom classes
     */
    private int countCustomClasses(List<DocumentClassInfo> classes) {
        int count = 0;
        for (DocumentClassInfo classInfo : classes) {
            if (isCustomClass(classInfo.symbolicName)) {
                count++;
            }
        }
        return count;
    }

    private enum ClassType {
        CUSTOM,
        SYSTEM
    }
    /**
     * Helper class to store class information
     */
    private static class DocumentClassInfo {
        String symbolicName;
        String displayName;
        String description;
        ClassType type;
    }
}

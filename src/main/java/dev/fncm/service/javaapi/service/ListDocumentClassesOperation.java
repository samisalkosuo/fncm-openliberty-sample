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

import dev.fncm.model.DocumentClassItem;
import dev.fncm.model.DocumentClassListResult;
import dev.fncm.service.javaapi.FileNetOperation;

/**
 * Lists document classes in the object store.
 * Returns a typed {@link DocumentClassListResult} serialised to JSON by Liberty JSON-B.
 */
public class ListDocumentClassesOperation implements FileNetOperation<DocumentClassListResult> {

    private static final Logger LOGGER = Logger.getLogger(ListDocumentClassesOperation.class.getName());

    @Override
    public DocumentClassListResult execute(ObjectStore os, String username) throws Exception {

        String sqlQuery = "SELECT SymbolicName, DisplayName, DescriptiveText " +
                "FROM ClassDefinition " +
                "WHERE IsHidden = FALSE " +
                "ORDER BY SymbolicName";

        LOGGER.info("Executing query: " + sqlQuery);

        SearchSQL searchSQL = new SearchSQL(sqlQuery);
        SearchScope searchScope = new SearchScope(os);
        RepositoryRowSet rowSet = searchScope.fetchRows(searchSQL, null, null, true);

        List<ClassInfo> allClasses = new ArrayList<>();
        Iterator<?> iterator = rowSet.iterator();

        while (iterator.hasNext()) {
            RepositoryRow row = (RepositoryRow) iterator.next();
            Properties props = row.getProperties();

            String symbolicName = props.getStringValue("SymbolicName");
            String type = isCustomClass(symbolicName) ? "CUSTOM" : "SYSTEM";

            allClasses.add(new ClassInfo(
                    symbolicName,
                    props.getStringValue("DisplayName"),
                    props.getStringValue("DescriptiveText"),
                    type));
        }

        // Keep only classes that are queryable as a document table
        List<ClassInfo> documentClasses = new ArrayList<>();
        for (ClassInfo ci : allClasses) {
            if (isDocumentClass(ci.symbolicName, os)) {
                documentClasses.add(ci);
            }
        }

        Collections.sort(documentClasses, Comparator.comparing(c -> c.symbolicName));

        LOGGER.info("Document classes found: " + documentClasses.size());

        List<DocumentClassItem> items = new ArrayList<>(documentClasses.size());
        for (ClassInfo ci : documentClasses) {
            items.add(new DocumentClassItem(ci.symbolicName, ci.displayName, ci.description, ci.type));
        }

        return new DocumentClassListResult(items.size(), items);
    }

    /**
     * Returns true if the class is queryable as a Document table.
     */
    private boolean isDocumentClass(String symbolicName, ObjectStore os) {
        try {
            String testQuery = "SELECT TOP 1 Id FROM " + symbolicName;
            new SearchScope(os).fetchRows(new SearchSQL(testQuery), null, null, false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if the class appears to be user-defined rather than a built-in system class.
     */
    private boolean isCustomClass(String symbolicName) {
        String[] systemClasses = {
                "Document", "Folder", "CustomObject", "Annotation",
                "Email", "WorkflowDefinition", "Queue", "Roster"
        };
        for (String s : systemClasses) {
            if (symbolicName.equals(s)) {
                return false;
            }
        }
        return symbolicName.contains("_") || symbolicName.matches(".*[A-Z][a-z]+[A-Z].*");
    }

    /** Temporary holder used during collection before converting to records. */
    private record ClassInfo(String symbolicName, String displayName, String description, String type) {}
}

// Made with Bob

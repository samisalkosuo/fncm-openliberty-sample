package dev.fncm.service.javaapi.service;

import com.filenet.api.collection.FolderSet;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;

import dev.fncm.model.FolderItem;
import dev.fncm.model.FolderListResult;
import dev.fncm.service.javaapi.FileNetOperation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Lists folders in the object store ordered by creation date descending.
 * Returns a typed {@link FolderListResult} serialised to JSON by Liberty JSON-B.
 */
public class ListFoldersOperation implements FileNetOperation<FolderListResult> {

    private static final Logger LOGGER = Logger.getLogger(ListFoldersOperation.class.getName());

    private static final int MAX_FOLDERS = 100;

    @Override
    public FolderListResult execute(ObjectStore os, String username) throws Exception {
        LOGGER.info("Listing folders in Object Store: " + os.get_DisplayName());
        LOGGER.info("(Showing first " + MAX_FOLDERS + " folders)");

        String sqlQuery = "SELECT TOP " + MAX_FOLDERS
                + " FolderName, PathName, Id, DateCreated, Creator"
                + " FROM Folder ORDER BY DateCreated DESC";

        LOGGER.info("Executing query: " + sqlQuery);

        SearchSQL searchSQL = new SearchSQL(sqlQuery);
        SearchScope searchScope = new SearchScope(os);
        FolderSet folders = (FolderSet) searchScope.fetchObjects(searchSQL, null, null, true);

        List<FolderItem> items = new ArrayList<>();
        Iterator<?> it = folders.iterator();

        while (it.hasNext()) {
            Folder folder = (Folder) it.next();
            items.add(new FolderItem(
                    folder.get_PathName(),
                    folder.get_Id().toString(),
                    folder.get_DateCreated() != null ? folder.get_DateCreated().toString() : null,
                    folder.get_Creator()));
            LOGGER.info("[" + items.size() + "] Path: " + folder.get_PathName());
        }

        LOGGER.info("Total folders listed: " + items.size());
        if (items.isEmpty()) {
            LOGGER.info("No folders found in the object store.");
        }

        return new FolderListResult(items.size(), items);
    }
}

// Made with Bob

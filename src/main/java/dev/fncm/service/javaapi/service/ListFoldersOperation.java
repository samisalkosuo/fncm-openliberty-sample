package dev.fncm.service.javaapi.service;

import com.filenet.api.collection.FolderSet;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;

import dev.fncm.service.javaapi.FileNetOperation;

import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Lists folders in the object store ordered by creation date descending.
 * Returns a plain-text summary (will be a typed record after R7).
 */
public class ListFoldersOperation implements FileNetOperation<String> {

    private static final Logger LOGGER = Logger.getLogger(ListFoldersOperation.class.getName());

    private static final int MAX_FOLDERS = 100;

    @Override
    public String execute(ObjectStore os, String username) throws Exception {
        LOGGER.info("Listing folders in Object Store: " + os.get_DisplayName());
        LOGGER.info("(Showing first " + MAX_FOLDERS + " folders)");

        String sqlQuery = "SELECT TOP " + MAX_FOLDERS
                + " FolderName, PathName, Id, DateCreated, Creator"
                + " FROM Folder ORDER BY DateCreated DESC";

        LOGGER.info("Executing query: " + sqlQuery);

        SearchSQL searchSQL = new SearchSQL(sqlQuery);
        SearchScope searchScope = new SearchScope(os);
        FolderSet folders = (FolderSet) searchScope.fetchObjects(searchSQL, null, null, true);

        StringBuilder sb = new StringBuilder("Folders Found\n");
        int count = 0;
        Iterator<?> it = folders.iterator();

        while (it.hasNext()) {
            Folder folder = (Folder) it.next();
            count++;
            sb.append("[").append(count).append("] Folder Information:\n")
              .append("  Path: ").append(folder.get_PathName()).append('\n')
              .append("  ID: ").append(folder.get_Id()).append('\n')
              .append("  Created: ").append(folder.get_DateCreated()).append('\n')
              .append("  Creator: ").append(folder.get_Creator()).append('\n');

            LOGGER.info("[" + count + "] Path: " + folder.get_PathName());
        }

        sb.append("Total folders listed: ").append(count).append('\n');

        LOGGER.info("Total folders listed: " + count);
        if (count == 0) {
            LOGGER.info("No folders found in the object store.");
        }

        return sb.toString();
    }
}

// Made with Bob

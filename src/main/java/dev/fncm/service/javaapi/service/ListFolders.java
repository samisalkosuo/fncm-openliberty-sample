package dev.fncm.service.javaapi.service;

import java.util.Iterator;
import java.util.logging.Logger;

import com.filenet.api.collection.FolderSet;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;

import dev.fncm.service.javaapi.BaseFileNetApp;
import dev.fncm.service.javaapi.FileNetConfig;

public class ListFolders extends BaseFileNetApp<String> {

    private static final Logger logger = Logger.getLogger(ListFolders.class.getName());

    public ListFolders(String userName, String oauthToken, FileNetConfig config) {
        this.userName = userName;
        this.oauthToken = oauthToken;
        this.config = config;
    }

    @Override
    protected String doWork(ObjectStore objectStore, String[] args) throws Exception {

        StringBuilder sb = new StringBuilder();

        int maxFolders = 100;
        if (args.length > 0) {
            try {
                maxFolders = Integer.parseInt(args[0]);
                logger.info("Using custom folder limit: " + maxFolders);
            } catch (NumberFormatException e) {
                logger.info("Warning: Invalid number format '" + args[0] + "', using default: " + maxFolders);
            }
        }

        logger.info("Listing folders in Object Store: " + objectStore.get_DisplayName());
        logger.info("(Showing first " + maxFolders + " folders)");

        String sqlQuery = "SELECT TOP " + maxFolders + " FolderName, PathName, Id, DateCreated, Creator " +
                         "FROM Folder " +
                         "ORDER BY DateCreated DESC";

        logger.info("Executing query: " + sqlQuery);

        SearchSQL searchSQL = new SearchSQL(sqlQuery);
        SearchScope searchScope = new SearchScope(objectStore);
        FolderSet folders = (FolderSet) searchScope.fetchObjects(searchSQL, null, null, true);

        int count = 0;
        Iterator<?> iterator = folders.iterator();

        logger.info("=================================================");
        logger.info("Folders Found");
        logger.info("=================================================");
        sb.append("Folders Found");
        sb.append('\n');

        while (iterator.hasNext()) {
            Folder folder = (Folder) iterator.next();
            count++;

            sb.append("[" + count + "] Folder Information:");
            sb.append('\n');
            sb.append("  Path: " + folder.get_PathName());
            sb.append('\n');
            sb.append("  ID: " + folder.get_Id());
            sb.append('\n');
            sb.append("  Created: " + folder.get_DateCreated());
            sb.append('\n');
            sb.append("  Creator: " + folder.get_Creator());
            sb.append('\n');

            logger.info("[" + count + "] Folder Information:");
            logger.info("  Name: " + folder.get_FolderName());
            logger.info("  Path: " + folder.get_PathName());
            logger.info("  ID: " + folder.get_Id());
            logger.info("  Created: " + folder.get_DateCreated());
            logger.info("  Creator: " + folder.get_Creator());
        }

        logger.info("=================================================");
        logger.info("Total folders listed: " + count);
        logger.info("=================================================");

        sb.append("Total folders listed: " + count);
        sb.append('\n');

        if (count == 0) {
            logger.info("No folders found in the object store.");
        }

        return sb.toString();
    }
}

// Made with Bob

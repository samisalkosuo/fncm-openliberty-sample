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
    
    
    public ListFolders(String userName, String oauthToken) {
        this.userName = userName;
        this.oauthToken = oauthToken;
    }

    /**
     * Override loadConfiguration to use OAuth validation — password is not required
     * because this class authenticates via an IAM/OAuth token, not a password.
     */
    @Override
    protected FileNetConfig loadConfiguration(String[] args) {
        logger.info("Loading configuration...");
        FileNetConfig cfg = FileNetConfig.load();
        if (args.length >= 4) {
            cfg.overrideFromArgs(args);
            logger.info("✓ Configuration overridden with command-line arguments");
        }
        cfg.validateForOAuth();
        logger.info("✓ Configuration loaded (OAuth mode — password not required)");
        return cfg;
    }
    
    @Override
    protected String doWork(ObjectStore objectStore, String[] args) throws Exception {
        
        StringBuilder sb = new StringBuilder();
        
        
        // Allow custom limit from arguments
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
        logger.info("");
        
        // Query for folders
        String sqlQuery = "SELECT TOP " + maxFolders + " FolderName, PathName, Id, DateCreated, Creator " +
                         "FROM Folder " +
                         "ORDER BY DateCreated DESC";
        
        logger.info("Executing query: " + sqlQuery);
        logger.info("");
        
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
            
            sb.append("[" + count + "] Folder Information:\"");
            sb.append('\n');
            sb.append("  Path: " + folder.get_PathName());
            sb.append('\n');
            sb.append("  ID: " + folder.get_Id());
            sb.append('\n');
            sb.append("  Created: " + folder.get_DateCreated());
            sb.append('\n');
            sb.append("  Creator: " + folder.get_Creator());
            sb.append('\n');

            logger.info("\n[" + count + "] Folder Information:");
            logger.info("  Name: " + folder.get_FolderName());
            logger.info("  Path: " + folder.get_PathName());
            logger.info("  ID: " + folder.get_Id());
            logger.info("  Created: " + folder.get_DateCreated());
            logger.info("  Creator: " + folder.get_Creator());
        }
        
        logger.info("");
        logger.info("=================================================");
        logger.info("Total folders listed: " + count);
        logger.info("=================================================");

        sb.append("Total folders listed: " + count);
        sb.append('\n');

        if (count == 0) {
            logger.info("\nNo folders found in the object store.");
        }

        return sb.toString();
    }
}

// Made with Bob

package dev.fncm.utils;

import java.util.Iterator;
import java.util.logging.Logger;

import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.property.Property;

import dev.fncm.service.javaapi.service.ConnectionTestOperation;

public class DeleteAllFolders {

    private static final Logger LOGGER = Logger.getLogger(ConnectionTestOperation.class.getName());

    public String execute(ObjectStore objectStore) throws Exception {
        LOGGER.info("=================================================");
        LOGGER.info("Delete All Folders Starting");
        LOGGER.info("=================================================");

        Folder root = objectStore.get_RootFolder();

        int deleted = deleteSubFolders(root, "/");

        String msg = "Deleted folders: " + deleted;
        LOGGER.info(msg);
        return msg;
    }



    private int deleteSubFolders(Folder parent, String parentPath) {
        int deleted = 0;

        Property subFoldersProperty = parent.fetchProperty("SubFolders", null, 100);
        IndependentObjectSet subFolders = subFoldersProperty.getIndependentObjectSetValue();

        Iterator<?> it = subFolders.iterator();

        while (it.hasNext()) {
            Folder child = (Folder) it.next();

            String childName = safeFolderName(child);
            String childPath = "/".equals(parentPath)
                    ? "/" + childName
                    : parentPath + "/" + childName;

            // Delete children first
            deleted += deleteSubFolders(child, childPath);

            try {
                LOGGER.info("Deleting folder: " + childPath);

                child.delete();
                child.save(RefreshMode.NO_REFRESH);

                deleted++;
            } catch (Exception e) {
                LOGGER.severe("Failed to delete folder: "
                        + childPath + " -> " + e.getMessage());
            }
        }

        return deleted;
    }

    private String safeFolderName(Folder folder) {
        try {
            String name = folder.get_FolderName();
            if (name != null && !name.isBlank()) {
                return name;
            }
        } catch (Exception ignored) {
        }

        return "<unnamed-folder>";
    }
}

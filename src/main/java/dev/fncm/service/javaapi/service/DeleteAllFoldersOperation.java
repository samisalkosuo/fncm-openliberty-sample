package dev.fncm.service.javaapi.service;

import com.filenet.api.core.ObjectStore;

import dev.fncm.model.ResponseMessage;
import dev.fncm.service.javaapi.FileNetOperation;
import dev.fncm.utils.DeleteAllFolders;

/**
 * Deletes all folders in the object store by delegating to {@link dev.fncm.utils.DeleteAllFolders}.
 */
public class DeleteAllFoldersOperation implements FileNetOperation<ResponseMessage> {

    @Override
    public ResponseMessage execute(ObjectStore os, String username) throws Exception {
        DeleteAllFolders deleteFolders = new DeleteAllFolders();
        deleteFolders.execute(os);

        ResponseMessage msg = new ResponseMessage("Folders deleted");
        return msg;
    }
}

// Made with Bob

package dev.fncm.service.javaapi.service;

import com.filenet.api.core.ObjectStore;

import dev.fncm.model.AddBuildingInspectionDocsResult;
import dev.fncm.service.javaapi.FileNetOperation;

/**
 * _OperationTemplate — copy and rename this file to implement a new FileNet operation.
 *
 * Steps:
 *  1. Rename this class to {@code MyOperation} and this file to {@code MyOperation.java}.
 *  2. Change {@code String} to your desired result type (or add a model record in
 *     {@code dev.fncm.model}).
 *  3. Implement {@code execute()} using the pre-connected {@code os} and {@code username}.
 *  4. In your resource, inject {@link dev.fncm.service.javaapi.FileNetService} and call:
 *     <pre>service.run(new MyOperation(), tokenContext)</pre>
 */
public class AddBuildingInspectionDocsOperation implements FileNetOperation<AddBuildingInspectionDocsResult> {

    @Override
    public AddBuildingInspectionDocsResult execute(ObjectStore os, String username) throws Exception {
        
        AddBuildingInspectionDocsResult result = new AddBuildingInspectionDocsResult(false);
        
        return result;
    }
}

// Made with Bob

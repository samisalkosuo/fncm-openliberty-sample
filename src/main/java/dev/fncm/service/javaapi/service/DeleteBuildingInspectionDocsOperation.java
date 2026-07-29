package dev.fncm.service.javaapi.service;

import com.filenet.api.core.ObjectStore;

import dev.fncm.model.BuildingInspectionDocsResult;
import dev.fncm.service.javaapi.FileNetOperation;

public class DeleteBuildingInspectionDocsOperation implements FileNetOperation<BuildingInspectionDocsResult> {

    @Override
    public BuildingInspectionDocsResult execute(ObjectStore os, String username) throws Exception {
        
        BuildingInspectionDocsResult result = new BuildingInspectionDocsResult("DELETE",false);
        
        return result;
    }
}

// Made with Bob

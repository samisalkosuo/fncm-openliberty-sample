package dev.fncm.service.javaapi.service.buildinginspectiondocs;

import java.util.ArrayList;
import java.util.List;

import com.filenet.api.core.ObjectStore;

import dev.fncm.model.BuildingInspectionDocsResult;
import dev.fncm.service.javaapi.FileNetOperation;

public class DeleteBuildingInspectionDocsOperation implements FileNetOperation<List<BuildingInspectionDocsResult>> {

    @Override
    public List<BuildingInspectionDocsResult> execute(ObjectStore os, String username) throws Exception {

        DeleteBuildingInspectionTypes deleteTypes = new DeleteBuildingInspectionTypes();
        deleteTypes.execute(os);

        List<BuildingInspectionDocsResult> results = new ArrayList<>();
        results.add(new BuildingInspectionDocsResult("Documents and types deleted", true));

        return results;
    }
}

// Made with Bob

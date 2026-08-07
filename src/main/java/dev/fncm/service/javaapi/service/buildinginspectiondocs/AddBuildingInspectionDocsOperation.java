package dev.fncm.service.javaapi.service.buildinginspectiondocs;

import java.util.ArrayList;
import java.util.List;

import com.filenet.api.core.ObjectStore;

import dev.fncm.model.BuildingInspectionDocsResult;
import dev.fncm.service.javaapi.FileNetOperation;

public class AddBuildingInspectionDocsOperation implements FileNetOperation<List<BuildingInspectionDocsResult>> {

    @Override
    public List<BuildingInspectionDocsResult> execute(ObjectStore os, String username) throws Exception {

        CreateBuildingInspectionTypes typeCreator = new CreateBuildingInspectionTypes();
        typeCreator.execute(os);

        List<BuildingInspectionDocsResult> results = new ArrayList<>();
        results.add(new BuildingInspectionDocsResult("Types created", true));

        UploadBuildingInspectionDocs uploadDocs = new UploadBuildingInspectionDocs();
        uploadDocs.execute(os);
        results.add(new BuildingInspectionDocsResult("Docs uploaded", true));

        return results;
    }
}

// Made with Bob

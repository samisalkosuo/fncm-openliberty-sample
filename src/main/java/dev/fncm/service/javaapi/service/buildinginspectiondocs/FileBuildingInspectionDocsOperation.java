package dev.fncm.service.javaapi.service.buildinginspectiondocs;

import java.util.ArrayList;
import java.util.List;

import com.filenet.api.core.ObjectStore;

import dev.fncm.model.BuildingInspectionDocsResult;
import dev.fncm.service.javaapi.FileNetOperation;

public class FileBuildingInspectionDocsOperation implements FileNetOperation<List<BuildingInspectionDocsResult>> {

    @Override
    public List<BuildingInspectionDocsResult> execute(ObjectStore os, String username) throws Exception {

        CreateFoldersAndFileBuildingInspectionReports fileDocs = new CreateFoldersAndFileBuildingInspectionReports();
        fileDocs.execute(os);

        List<BuildingInspectionDocsResult> results = new ArrayList<>();
        results.add(new BuildingInspectionDocsResult("Documents filed", true));

        return results;
    }
}

// Made with Bob

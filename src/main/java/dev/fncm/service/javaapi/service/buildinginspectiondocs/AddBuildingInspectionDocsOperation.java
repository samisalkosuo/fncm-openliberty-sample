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
        typeCreator.execute(os);;
        BuildingInspectionDocsResult typesCreatedResult = new BuildingInspectionDocsResult("Types created",true);
        
        List<BuildingInspectionDocsResult> results = new ArrayList<BuildingInspectionDocsResult>();
        results.add(typesCreatedResult);
        
        return results;
    }
}

// Made with Bob

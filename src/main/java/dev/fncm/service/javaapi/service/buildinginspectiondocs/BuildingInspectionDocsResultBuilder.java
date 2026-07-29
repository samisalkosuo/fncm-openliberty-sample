package dev.fncm.service.javaapi.service.buildinginspectiondocs;

import java.util.ArrayList;
import java.util.List;

import dev.fncm.model.BuildingInspectionDocsResult;

public class BuildingInspectionDocsResultBuilder {
    private List<BuildingInspectionDocsResult> results = new ArrayList<BuildingInspectionDocsResult>();

    
    public void add(String operation, boolean success){
        BuildingInspectionDocsResult result = new BuildingInspectionDocsResult(operation,success);
        results.add(result);
    }

    public List<BuildingInspectionDocsResult> get()
    {
        return results;
    }

}

package dev.fncm.model;

public record AddBuildingInspectionDocsResult(
                boolean success) {

        public AddBuildingInspectionDocsResult withSuccess(boolean newSuccess) {
                return new AddBuildingInspectionDocsResult(newSuccess);
        }
}

// Made with Bob

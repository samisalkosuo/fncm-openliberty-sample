package dev.fncm.model;

public record BuildingInspectionDocsResult(
                String operation,
                boolean success) {

        public BuildingInspectionDocsResult withSuccess(boolean newSuccess) {
                return new BuildingInspectionDocsResult(this.operation(),newSuccess);
        }
}

// Made with Bob

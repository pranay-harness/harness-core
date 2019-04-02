package software.wings.graphql.datafetcher.workflow.adapater;

import lombok.Getter;

public enum WorkflowFieldsEnum {
  ID("id"),
  NAME("name"),
  WORKFLOW_TYPE("workflowType"),
  DESCRIPTION("description"),
  TEMPLATIZED("templatized"),
  SERVICES("services");

  @Getter private String fieldName;

  WorkflowFieldsEnum(String fieldName) {
    this.fieldName = fieldName;
  }
}

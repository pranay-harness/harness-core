package software.wings.graphql.datafetcher.workflow.adapater;

import lombok.Getter;

public enum WorkflowExecutionFieldsEnum {
  ID("id"),
  NAME("name"),
  STATUS("status"),
  START_TIME("startTime"),
  END_TIME("endTime");

  @Getter private String fieldName;

  WorkflowExecutionFieldsEnum(String fieldName) {
    this.fieldName = fieldName;
  }
}

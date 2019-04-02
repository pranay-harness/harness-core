package software.wings.graphql.datafetcher.artifact;

import lombok.Getter;

public enum ArtifactFieldsEnum {
  ID("id"),
  SOURCE_NAME("sourceName"),
  DISPLAY_NAME("displayName"),
  BUILD_NO("buildNo"),
  LAST_DEPLOYED_BY("lastDeployedBy"),
  LAST_DEPLOYED_AT("lastDeployedAt"),
  WORKFLOW_EXECUTION_NAME("workflowExecutionName");

  @Getter private String fieldName;

  ArtifactFieldsEnum(String fieldName) {
    this.fieldName = fieldName;
  }
}
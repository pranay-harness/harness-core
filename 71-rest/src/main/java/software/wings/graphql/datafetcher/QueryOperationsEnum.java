package software.wings.graphql.datafetcher;

import lombok.Getter;

/**
 * The idea of this enum to keep track of fields
 * that are exposed in a schema and to also
 * write test to make sure we are duplicating
 */
public enum QueryOperationsEnum {
  WORKFLOW("workflow"),
  WORKFLOW_LIST("workflows"),
  WORKFLOW_EXECUTION("workflowExecution"),
  WORKFLOW_EXECUTION_LIST("workflowExecutionList"),
  DEPLOYED_ARTIFACTS("deployedArtifacts"),
  APPLICATIONS("applications"),
  APPLICATION("application"),
  ENVIRONMENTS("environments"),
  ENVIRONMENT("environment");

  @Getter private String operationName;

  QueryOperationsEnum(String operationName) {
    this.operationName = operationName;
  }
}

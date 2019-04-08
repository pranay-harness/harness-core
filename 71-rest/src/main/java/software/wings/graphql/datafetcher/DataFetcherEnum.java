package software.wings.graphql.datafetcher;

import lombok.Getter;

public enum DataFetcherEnum {
  WORKFLOW("workflow"),
  WORKFLOWS("workflows"),
  WORKFLOW_EXECUTION("workflowExecution"),
  WORKFLOW_EXECUTIONS("workflowExecutions"),
  DEPLOYED_ARTIFACTS("deployedArtifacts"),
  APPLICATION("application"),
  APPLICATIONS("applications"),
  ENVIRONMENT("environment"),
  ENVIRONMENTS("environments");

  @Getter private String dataFetcherName;

  DataFetcherEnum(String dataFetcherAnnotationName) {
    this.dataFetcherName = dataFetcherAnnotationName;
  }
}

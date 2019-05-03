package software.wings.graphql.schema.query;

import lombok.Value;

@Value
public class QLWorkflowQueryParameters {
  private String workflowId;
  private String executionId;
}

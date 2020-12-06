package software.wings.graphql.schema.query;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLWorkflowQueryParameters {
  private String workflowId;
  private String executionId;
  private String workflowName;
  private String applicationId;
}

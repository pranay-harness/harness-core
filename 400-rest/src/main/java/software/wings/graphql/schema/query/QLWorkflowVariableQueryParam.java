package software.wings.graphql.schema.query;

import lombok.Value;

@Value
public class QLWorkflowVariableQueryParam {
  String workflowId;
  String applicationId;
}

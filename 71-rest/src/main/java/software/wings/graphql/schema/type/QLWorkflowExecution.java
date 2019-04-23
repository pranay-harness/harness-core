package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLWorkflowExecutionKeys")
public class QLWorkflowExecution implements QLExecution {
  String id;
  long queuedTime;
  long startTime;
  long endTime;
  QLExecutionStatus status;
}

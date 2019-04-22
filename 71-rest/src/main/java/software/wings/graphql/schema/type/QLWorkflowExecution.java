package software.wings.graphql.schema.type;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLWorkflowExecution implements QLExecution {
  String id;
  ExecutionStatus status;
  long queuedTime;
  long startTime;
  long endTime;
}
